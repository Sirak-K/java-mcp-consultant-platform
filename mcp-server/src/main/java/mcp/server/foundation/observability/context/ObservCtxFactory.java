package mcp.server.foundation.observability.context;

import mcp.server.foundation.transport.TranspSess;
import mcp.server.foundation.transport.http.shared.HTTPReqsMetadata;
import mcp.server.foundation.transport.http.shared.HTTPTranspSess;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMeta;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Factory for building observation context from the current runtime data.
 */
public final class ObservCtxFactory {

  private static final String TRANSPORT_WEBSOCKET = "websocket";

  public ObservCtx ObservCtxFactoryEmpty() {
    return ObservCtx.builder().build();
  }

  public ObservCtx ObservCtxFactoryCurrentOrEmpty() {
    ObservCtx current = ObservCtxHolder.ObservCtxHolderGet();
    return current == null ? ObservCtxFactoryEmpty() : current;
  }

  public ObservCtx ObservCtxFactoryFromTranspCoordinates(
      String transportName,
      String transportConnectionId,
      String mcpSessId) {

    return ObservCtxFactoryFromTranspCoordinatesWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        null);
  }

  public ObservCtx ObservCtxFactoryFromTranspCoordinates(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      McpSessRTMeta runtimeMeta) {

    return ObservCtxFactoryFromTranspCoordinatesWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        null,
        runtimeMeta);
  }

  public ObservCtx ObservCtxFactoryFromTranspCoordinates(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      ReqsAuthBinding requestAuthBinding) {

    return ObservCtxFactoryFromTranspCoordinatesWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        requestAuthBinding,
        null);
  }

  public ObservCtx ObservCtxFactoryFromTranspCoordinatesWithReqsAuthBinding(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      ReqsAuthBinding requestAuthBinding) {

    return ObservCtxFactoryFromTranspCoordinatesWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        requestAuthBinding,
        null);
  }

  public ObservCtx ObservCtxFactoryFromTranspCoordinatesWithReqsAuthBinding(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      ReqsAuthBinding requestAuthBinding,
      McpSessRTMeta runtimeMeta) {

    return ObservCtxFactoryBuild(
        mcpSessId,
        transportConnectionId,
        null,
        null,
        null,
        null,
        transportName,
        null,
        null,
        null,
        requestAuthBinding,
        runtimeMeta);
  }

  public ObservCtx ObservCtxFactoryForTranspLifecycle(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      String sessionPhase) {

    return ObservCtxFactoryForTranspLifecycleWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        sessionPhase,
        null);
  }

  public ObservCtx ObservCtxFactoryForTranspLifecycle(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      String sessionPhase,
      McpSessRTMeta runtimeMeta) {

    return ObservCtxFactoryForTranspLifecycleWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        sessionPhase,
        null,
        runtimeMeta);
  }

  public ObservCtx ObservCtxFactoryForTranspLifecycle(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      String sessionPhase,
      ReqsAuthBinding requestAuthBinding) {

    return ObservCtxFactoryForTranspLifecycleWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        sessionPhase,
        requestAuthBinding,
        null);
  }

  public ObservCtx ObservCtxFactoryForTranspLifecycleWithReqsAuthBinding(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      String sessionPhase,
      ReqsAuthBinding requestAuthBinding) {

    return ObservCtxFactoryForTranspLifecycleWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        sessionPhase,
        requestAuthBinding,
        null);
  }

  public ObservCtx ObservCtxFactoryForTranspLifecycleWithReqsAuthBinding(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      String sessionPhase,
      ReqsAuthBinding requestAuthBinding,
      McpSessRTMeta runtimeMeta) {

    return ObservCtxFactoryBuild(
        mcpSessId,
        transportConnectionId,
        null,
        null,
        null,
        null,
        transportName,
        sessionPhase,
        null,
        null,
        requestAuthBinding,
        runtimeMeta);
  }

  public ObservCtx ObservCtxFactoryFromTranspSess(TranspSess session) {

    Objects.requireNonNull(session, "session");

    return ObservCtxFactoryFromTranspCoordinatesWithReqsAuthBinding(
        session.TranspSessGetTranspName(),
        session.TranspSessGetTranspConnId(),
        session.TranspSessGetMcpSessId(),
        session.TranspSessGetReqsAuthBinding(),
        session.TranspSessGetRuntimeMeta());
  }

  public ObservCtx ObservCtxFactoryFromHTTPTranspSess(HTTPTranspSess session) {

    Objects.requireNonNull(session, "session");

    return ObservCtxFactoryWithClientMetadata(
        ObservCtxFactoryFromTranspSess(session.HtsGetTranspSess()),
        session.HtsGetLastReqsMetadata());
  }

  public ObservCtx ObservCtxFactoryForRpc(
      TranspSess session,
      String rpcCorrelaId,
      String rpcMet) {

    Objects.requireNonNull(session, "session");

    return ObservCtxFactoryForRpcWithReqsAuthBinding(
        session.TranspSessGetTranspName(),
        session.TranspSessGetTranspConnId(),
        session.TranspSessGetMcpSessId(),
        rpcCorrelaId,
        rpcMet,
        session.TranspSessGetReqsAuthBinding(),
        session.TranspSessGetRuntimeMeta());
  }

  public ObservCtx ObservCtxFactoryForRpc(
      HTTPTranspSess session,
      String rpcCorrelaId,
      String rpcMet) {

    Objects.requireNonNull(session, "session");

    return ObservCtxFactoryWithClientMetadata(
        ObservCtxFactoryForRpc(
            session.HtsGetTranspSess(),
            rpcCorrelaId,
            rpcMet),
        session.HtsGetLastReqsMetadata());
  }

  public ObservCtx ObservCtxFactoryForRpc(
      String mcpSessId,
      String wsConnId,
      String rpcCorrelaId,
      String rpcMet) {

    return ObservCtxFactoryBuild(
        mcpSessId,
        wsConnId,
        rpcCorrelaId,
        rpcMet,
        null,
        System.nanoTime(),
        TRANSPORT_WEBSOCKET,
        null,
        null,
        null,
        null,
        null);
  }

  public ObservCtx ObservCtxFactoryForRpc(
      String mcpSessId,
      String wsConnId,
      String rpcCorrelaId,
      String rpcMet,
      ReqsAuthBinding requestAuthBinding) {

    return ObservCtxFactoryForRpcWithReqsAuthBinding(
        mcpSessId,
        wsConnId,
        rpcCorrelaId,
        rpcMet,
        requestAuthBinding,
        (McpSessRTMeta) null);
  }

  public ObservCtx ObservCtxFactoryForRpcWithReqsAuthBinding(
      String mcpSessId,
      String wsConnId,
      String rpcCorrelaId,
      String rpcMet,
      ReqsAuthBinding requestAuthBinding) {

    return ObservCtxFactoryForRpcWithReqsAuthBinding(
        mcpSessId,
        wsConnId,
        rpcCorrelaId,
        rpcMet,
        requestAuthBinding,
        null);
  }

  public ObservCtx ObservCtxFactoryForRpcWithReqsAuthBinding(
      String mcpSessId,
      String wsConnId,
      String rpcCorrelaId,
      String rpcMet,
      ReqsAuthBinding requestAuthBinding,
      McpSessRTMeta runtimeMeta) {

    return ObservCtxFactoryBuild(
        mcpSessId,
        wsConnId,
        rpcCorrelaId,
        rpcMet,
        null,
        System.nanoTime(),
        TRANSPORT_WEBSOCKET,
        null,
        null,
        null,
        requestAuthBinding,
        runtimeMeta);
  }

  public ObservCtx ObservCtxFactoryForRpc(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      String rpcCorrelaId,
      String rpcMet) {

    return ObservCtxFactoryForRpcWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        rpcCorrelaId,
        rpcMet,
        null,
        null);
  }

  public ObservCtx ObservCtxFactoryForRpc(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      String rpcCorrelaId,
      String rpcMet,
      ReqsAuthBinding requestAuthBinding) {

    return ObservCtxFactoryForRpcWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        rpcCorrelaId,
        rpcMet,
        requestAuthBinding,
        (McpSessRTMeta) null);
  }

  public ObservCtx ObservCtxFactoryForRpcWithReqsAuthBinding(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      String rpcCorrelaId,
      String rpcMet,
      ReqsAuthBinding requestAuthBinding) {

    return ObservCtxFactoryForRpcWithReqsAuthBinding(
        transportName,
        transportConnectionId,
        mcpSessId,
        rpcCorrelaId,
        rpcMet,
        requestAuthBinding,
        null);
  }

  public ObservCtx ObservCtxFactoryForRpcWithReqsAuthBinding(
      String transportName,
      String transportConnectionId,
      String mcpSessId,
      String rpcCorrelaId,
      String rpcMet,
      ReqsAuthBinding requestAuthBinding,
      McpSessRTMeta runtimeMeta) {

    return ObservCtxFactoryBuild(
        mcpSessId,
        transportConnectionId,
        rpcCorrelaId,
        rpcMet,
        null,
        System.nanoTime(),
        transportName,
        null,
        null,
        null,
        requestAuthBinding,
        runtimeMeta);
  }

  public ObservCtx ObservCtxFactoryCopy(ObservCtx base) {

    Objects.requireNonNull(base, "base");

    return base.ObservCtxToBuilder().build();
  }

  public ObservCtx ObservCtxFactoryMerge(
      ObservCtx primary,
      ObservCtx secondary) {

    if (primary == null && secondary == null) {
      return ObservCtxFactoryEmpty();
    }

    if (primary == null) {
      return ObservCtxFactoryCopy(secondary);
    }

    if (secondary == null) {
      return ObservCtxFactoryCopy(primary);
    }

    return ObservCtxFactoryBuild(
        ObservCtxFactoryPrefer(primary.ObservCtxGetMcpSessId(), secondary.ObservCtxGetMcpSessId()),
        ObservCtxFactoryPrefer(primary.ObservCtxGetWsConnId(), secondary.ObservCtxGetWsConnId()),
        ObservCtxFactoryPrefer(primary.ObservCtxGetRPCCorrelaId(), secondary.ObservCtxGetRPCCorrelaId()),
        ObservCtxFactoryPrefer(primary.ObservCtxGetRPCMet(), secondary.ObservCtxGetRPCMet()),
        ObservCtxFactoryPrefer(primary.ObservCtxGetToolName(), secondary.ObservCtxGetToolName()),
        primary.ObservCtxGetReqsStartNano() != null
            ? primary.ObservCtxGetReqsStartNano()
            : secondary.ObservCtxGetReqsStartNano(),
        ObservCtxFactoryPrefer(primary.ObservCtxGetTranspName(), secondary.ObservCtxGetTranspName()),
        ObservCtxFactoryPrefer(primary.ObservCtxGetSessPhase(), secondary.ObservCtxGetSessPhase()),
        ObservCtxFactoryPrefer(primary.ObservCtxGetClientAddress(), secondary.ObservCtxGetClientAddress()),
        ObservCtxFactoryPrefer(primary.ObservCtxGetErrType(), secondary.ObservCtxGetErrType()),
        primary.ObservCtxGetReqsAuthBinding() != null
            ? primary.ObservCtxGetReqsAuthBinding()
            : secondary.ObservCtxGetReqsAuthBinding(),
        primary.ObservCtxGetRuntimeMeta() != null
            ? primary.ObservCtxGetRuntimeMeta()
            : secondary.ObservCtxGetRuntimeMeta());
  }

  public ObservCtx ObservCtxFactoryWithRpcMetadata(
      ObservCtx base,
      String rpcCorrelaId,
      String rpcMet) {

    return ObservCtxFactoryMutate(
        base,
        builder -> builder
            .rpcCorrelaId(rpcCorrelaId)
            .rpcMet(rpcMet));
  }

  public ObservCtx ObservCtxFactoryWithToolName(
      ObservCtx base,
      String toolName) {

    return ObservCtxFactoryMutate(
        base,
        builder -> builder.toolName(toolName));
  }

  public ObservCtx ObservCtxFactoryWithSessionPhase(
      ObservCtx base,
      String sessionPhase) {

    return ObservCtxFactoryMutate(
        base,
        builder -> builder.sessionPhase(sessionPhase));
  }

  public ObservCtx ObservCtxFactoryWithRequestStartNano(
      ObservCtx base,
      Long requestStartNano) {

    return ObservCtxFactoryMutate(
        base,
        builder -> builder.requestStartNano(requestStartNano));
  }

  public ObservCtx ObservCtxFactoryWithErrType(
      ObservCtx base,
      String errorType) {

    return ObservCtxFactoryMutate(
        base,
        builder -> builder.errorType(errorType));
  }

  public ObservCtx ObservCtxFactoryWithClientAddress(
      ObservCtx base,
      String clientAddress) {

    return ObservCtxFactoryMutate(
        base,
        builder -> builder.clientAddress(clientAddress));
  }

  public ObservCtx ObservCtxFactoryWithClientMetadata(
      ObservCtx base,
      HTTPReqsMetadata requestMetadata) {

    if (requestMetadata == null) {
      return base == null ? ObservCtxFactoryEmpty() : ObservCtxFactoryCopy(base);
    }

    return ObservCtxFactoryWithClientAddress(
        base,
        requestMetadata.HTTPReqMetaPreferredClientAddress());
  }

  public ObservCtx ObservCtxFactoryWithRequestBinding(
      ObservCtx base,
      ReqsAuthBinding requestAuthBinding) {

    return ObservCtxFactoryMutate(
        base,
        builder -> builder.requestAuthBinding(requestAuthBinding));
  }

  public ObservCtx ObservCtxFactoryFromReqsAuthBinding(ReqsAuthBinding requestAuthBinding) {
    return ObservCtxFactoryWithRequestBinding(ObservCtxFactoryEmpty(), requestAuthBinding);
  }

  private ObservCtx ObservCtxFactoryBuild(
      String mcpSessId,
      String wsConnId,
      String rpcCorrelaId,
      String rpcMet,
      String toolName,
      Long requestStartNano,
      String transportName,
      String sessionPhase,
      String clientAddress,
      String errorType,
      ReqsAuthBinding requestAuthBinding,
      McpSessRTMeta runtimeMeta) {

    return ObservCtx.builder()
        .mcpSessId(mcpSessId)
        .wsConnId(wsConnId)
        .rpcCorrelaId(rpcCorrelaId)
        .rpcMet(rpcMet)
        .toolName(toolName)
        .requestStartNano(requestStartNano)
        .transportName(transportName)
        .sessionPhase(sessionPhase)
        .clientAddress(clientAddress)
        .errorType(errorType)
        .requestAuthBinding(requestAuthBinding)
        .runtimeMeta(runtimeMeta)
        .build();
  }

  private static String ObservCtxFactoryPrefer(String primary, String secondary) {
    return (primary == null || primary.isBlank()) ? secondary : primary;
  }

  private ObservCtx ObservCtxFactoryMutate(
      ObservCtx base,
      Consumer<ObservCtx.Builder> mutation) {

    Objects.requireNonNull(mutation, "mutation");

    ObservCtx.Builder builder = base == null
        ? ObservCtx.builder()
        : base.ObservCtxToBuilder();

    mutation.accept(builder);
    return builder.build();
  }
}
