package mcp.server.foundation.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxHolder;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.tracing.McpObservationSupport;
import mcp.server.foundation.prompt_interface.PromptLoader;
import mcp.server.foundation.prompt_interface.PromptReg;
import mcp.server.foundation.prompt_interface.PromptService;
import mcp.server.foundation.resource_interface.ResrcReg;
import mcp.server.foundation.rpc.error.ErrClassifier;
import mcp.server.foundation.server_process.client_context.session.McpSessRTOrch;
import mcp.server.foundation.server_process.client_context.session.McpSessReg;
import mcp.server.foundation.server_process.client_context.session.McpSessStateVerif;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.tool_interface.ToolInvocEngine;
import mcp.server.foundation.transport.TranspSess;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * RPCRouter
 *
 * Ansvar:
 * - Äger serverns RPC-routing på foundation-nivå.
 * - Utför session-state verification (policy) innan dispatch.
 * - Delegerar sedan till RPCMetResol för metodspecifik logik.
 *
 * Notera:
 * - RpcCapabilityDispatcher har eliminerats (garanterat redundant passthrough).
 * - Router anropar resolver direkt för determinism och minskad API-yta.
 *
 * Kontrakt:
 * - Canonical rpcCorrelaId resolution sker här.
 * - Canonical exception->response mapping sker här.
 */
public final class RPCRouter {

  private final McpSessRTOrch sessionRTOrch;
  private final McpSessStateVerif stateVerification;
  private final RPCMetResol methodResolver;
  private final ObservCtxFactory obsCtxFactory;
  private final ErrClassifier errorClassifier;
  private final ServerLogger logger;
  private final McpObservationSupport observationSupport;

  public RPCRouter(
      McpSessRTOrch sessionRTOrch,
      McpSessReg sessionRegistry,
      ToolInvocEngine toolEngine,
      ResrcReg resourceReg,
      RPCCapaDscr rpcCapaDscr,
      RPCJsonSeria rpcJsonSeria,
      ObservCtxFactory obsCtxFactory,
      ErrClassifier errorClassifier,
      ServerLogger logger) {
    this(
        sessionRTOrch,
        sessionRegistry,
        toolEngine,
        PromptSurfaceDefaults.create(),
        resourceReg,
        rpcCapaDscr,
        rpcJsonSeria,
        obsCtxFactory,
        errorClassifier,
        logger);
  }

  private RPCRouter(
      McpSessRTOrch sessionRTOrch,
      McpSessReg sessionRegistry,
      ToolInvocEngine toolEngine,
      PromptSurfaceDefaults promptDefaults,
      ResrcReg resourceReg,
      RPCCapaDscr rpcCapaDscr,
      RPCJsonSeria rpcJsonSeria,
      ObservCtxFactory obsCtxFactory,
      ErrClassifier errorClassifier,
      ServerLogger logger) {
    this(
        sessionRTOrch,
        sessionRegistry,
        toolEngine,
        promptDefaults.promptReg(),
        promptDefaults.promptService(),
        resourceReg,
        rpcCapaDscr,
        rpcJsonSeria,
        obsCtxFactory,
        errorClassifier,
        logger,
        new McpObservationSupport(ObservationRegistry.NOOP));
  }

  public RPCRouter(
      McpSessRTOrch sessionRTOrch,
      McpSessReg sessionRegistry,
      ToolInvocEngine toolEngine,
      PromptReg promptReg,
      PromptService promptService,
      ResrcReg resourceReg,
      RPCCapaDscr rpcCapaDscr,
      RPCJsonSeria rpcJsonSeria,
      ObservCtxFactory obsCtxFactory,
      ErrClassifier errorClassifier,
      ServerLogger logger) {
    this(
        sessionRTOrch,
        sessionRegistry,
        toolEngine,
        promptReg,
        promptService,
        resourceReg,
        rpcCapaDscr,
        rpcJsonSeria,
        obsCtxFactory,
        errorClassifier,
        logger,
        new McpObservationSupport(ObservationRegistry.NOOP));
  }

  public RPCRouter(
      McpSessRTOrch sessionRTOrch,
      McpSessReg sessionRegistry,
      ToolInvocEngine toolEngine,
      PromptReg promptReg,
      PromptService promptService,
      ResrcReg resourceReg,
      RPCCapaDscr rpcCapaDscr,
      RPCJsonSeria rpcJsonSeria,
      ObservCtxFactory obsCtxFactory,
      ErrClassifier errorClassifier,
      ServerLogger logger,
      McpObservationSupport observationSupport) {

    Objects.requireNonNull(sessionRTOrch, "sessionRTOrch");
    Objects.requireNonNull(sessionRegistry, "sessionRegistry");

    this.sessionRTOrch = sessionRTOrch;
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.errorClassifier = Objects.requireNonNull(errorClassifier, "errorClassifier");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.observationSupport = Objects.requireNonNull(observationSupport, "observationSupport");

    this.stateVerification = new McpSessStateVerif(sessionRegistry);
    this.methodResolver = new RPCMetResol(
        Objects.requireNonNull(toolEngine, "toolEngine"),
        Objects.requireNonNull(promptReg, "promptReg"),
        Objects.requireNonNull(promptService, "promptService"),
        Objects.requireNonNull(resourceReg, "resourceReg"),
        Objects.requireNonNull(rpcCapaDscr, "rpcCapaDscr"),
        Objects.requireNonNull(rpcJsonSeria, "rpcJsonSeria"),
        sessionRegistry,
        obsCtxFactory,
        logger);
  }

  /**
   * Canonical correlation id resolver.
   *
   * Kontrakt:
   * - Om request är notification eller saknar root "id" => genererat correlation id.
   * - Om id finns => returnera id i stabil textform.
   */
  public static String RPCRouterResolveCorrelaId(RPCReqsPayl request) {

    if (request == null) {
      return UUID.randomUUID().toString();
    }

    if (request.RPCReqPlIsNotification()) {
      return UUID.randomUUID().toString();
    }

    JsonNode id = request.RPCReqPlGetId();
    if (id == null || id.isNull()) {
      return UUID.randomUUID().toString();
    }

    if (id.isTextual()) {
      return id.asText();
    }

    if (id.isNumber()) {
      return id.asText();
    }

    return id.toString();
  }

  public static RPCRespPayl RPCRouterMapMappedExcepToResponse(
      RPCMappedExcep mapped,
      RPCReqsPayl request) {

    Objects.requireNonNull(mapped, "mapped");
    Objects.requireNonNull(request, "request");

    JsonNode id = request.RPCReqPlGetId();
    if (id == null || id.isNull()) {
      return null;
    }

    return RPCRespPayl.RpcRespPlError(
        id,
        mapped.RPCMappedExcepGetRPCErr());
  }

  public static RPCRespPayl RPCRouterMapRuntimeExcepToResponse(
      RuntimeException ex,
      RPCReqsPayl request) {

    Objects.requireNonNull(ex, "ex");
    Objects.requireNonNull(request, "request");

    JsonNode id = request.RPCReqPlGetId();
    if (id == null || id.isNull()) {
      return null;
    }

    return RPCRespPayl.RpcRespPlError(
        id,
        RPCErr.RPCErrInternalErr(ex.getMessage()));
  }

  public static RPCRespPayl RPCRouterMapParseMappedExcepToResp(RPCMappedExcep mapped) {

    Objects.requireNonNull(mapped, "mapped");
    return RPCRespPayl.RpcRespPlError(
        new TextNode(UUID.randomUUID().toString()),
        mapped.RPCMappedExcepGetRPCErr());
  }

  public static RPCRespPayl RPCRouterMapParseRTExcepToResp(RuntimeException ex) {

    Objects.requireNonNull(ex, "ex");
    return RPCRespPayl.RpcRespPlError(
        new TextNode(UUID.randomUUID().toString()),
        RPCErr.RPCErrInternalErr(ex.getMessage()));
  }

  // =========================================================
  // Routing
  // =========================================================

  /**
   * Canonical routing entrypoint.
   *
   * @return RPCRespPayl eller null om request är notification (id==null)
   */
  public RPCRespPayl RPCRouterRoute(
      RPCReqsPayl request,
      TranspSess transportSession) {

    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(transportSession, "transportSession");

    // Notifications ska inte generera svar
    if (request.RPCReqPlIsNotification()) {
      return null;
    }

    String correlationId = RPCRouterResolveCorrelaId(request);
    McpSessId sessionId = transportSession.TranspSessGetMcpSessIdObject();
    ObservCtx context = obsCtxFactory.ObservCtxFactoryForRpc(
        transportSession,
        correlationId,
        request.RPCReqPlGetMet());
    Observation observation = observationSupport.McpObsStartRPCObservation(
        context,
        request.RPCReqPlGetMet());
    Observation.Scope scope = observation.openScope();
    ObservCtxHolder.Scope holderScope = ObservCtxHolder.ObservCtxHolderOpenScope(context);

    try {
      logger.ServerLogInfoObserved(
          ServerLogger.Component.RPC,
          context,
          "TRACE",
          "RPC_CORRELATION_RESOLVED",
          "RPCRouter: correlation resolved");

      logger.ServerLogInfoObserved(
          ServerLogger.Component.RPC,
          context,
          "ROUTE",
          "RPC_ROUTE_STARTED",
          "RPCRouter: route started");

      // 1) verify session-state invariants (fail-fast)
      // (init tillåts; övriga metoder kräver normalt init)
      try {
        if (!RPCRouterShouldBypassSessStateVerif(request, transportSession)) {
          stateVerification.SessStateVerifVerifyRequestAllowed(
              sessionId,
              request.RPCReqPlGetMet());
        }
      } catch (RuntimeException ex) {

        String errorType = errorClassifier.ErrClassifierClassify(ex).name();
        observationSupport.McpObsMarkErr(observation, ex, errorType);
        logger.ServerLogWarnObserved(
            ServerLogger.Component.RPC,
            context,
            "VERIFY",
            "RPC_SESSION_VERIFICATION_FAILED",
            "RPCRouter: session verification failed: " + ex.getMessage(),
            errorType);

        throw ex;
      }

      logger.ServerLogInfoObserved(
          ServerLogger.Component.RPC,
          context,
          "VERIFY",
          "RPC_SESSION_VERIFICATION_PASSED",
          "RPCRouter: session verification passed");

      // 2) method resolve + execute (capability/tool)
      RPCRespPayl response = methodResolver.RpcMetResoResolve(
          request,
          sessionId,
          context);

      logger.ServerLogInfoObserved(
          ServerLogger.Component.RPC,
          context,
          "ROUTE",
          "RPC_ROUTE_COMPLETED",
          "RPCRouter: route completed",
          durationFrom(context));

      // 3) State transitions are owned by McpSessRTOrch.
      // Router must not implement method-specific state mutation.
      sessionRTOrch.McpSessRTOrchApplyRpcSideEffects(
          sessionId,
          request.RPCReqPlGetMet(),
          response);

      logger.ServerLogInfoObserved(
          ServerLogger.Component.RPC,
          context,
          "APPLY",
          "RPC_SIDE_EFFECTS_APPLIED",
          "RPCRouter: RPC side effects applied");

      return response;
    } catch (RuntimeException ex) {
      observationSupport.McpObsMarkErr(
          observation,
          ex,
          errorClassifier.ErrClassifierClassify(ex).name());
      throw ex;
    } finally {
      holderScope.close();
      scope.close();
      observation.stop();
    }
  }

  private static Long durationFrom(ObservCtx context) {

    if (context == null || context.ObservCtxGetReqsStartNano() == null) {
      return null;
    }

    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - context.ObservCtxGetReqsStartNano());
  }

  private static boolean RPCRouterShouldBypassSessStateVerif(
      RPCReqsPayl request,
      TranspSess transportSession) {

    if (request == null || transportSession == null || !transportSession.TranspSessIsReqScoped()) {
      return false;
    }

    String method = request.RPCReqPlGetMet();
    if (method == null || method.isBlank()) {
      return false;
    }

    return RPCMetName.RPCMetNameIsSessionlessFoundationMethod(method);
  }

  private record PromptSurfaceDefaults(
      PromptReg promptReg,
      PromptService promptService) {

    private static PromptSurfaceDefaults create() {
      PromptReg registry = new PromptReg();
      return new PromptSurfaceDefaults(
          registry,
          new PromptService(registry, new PromptLoader()));
    }
  }
}
