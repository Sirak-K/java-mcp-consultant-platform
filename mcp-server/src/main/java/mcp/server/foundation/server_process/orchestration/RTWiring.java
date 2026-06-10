package mcp.server.foundation.server_process.orchestration;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxHolder;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.rpc.RPCErr;
import mcp.server.foundation.rpc.RPCJsonEntry;
import mcp.server.foundation.rpc.RPCMappedExcep;
import mcp.server.foundation.rpc.RPCMetName;
import mcp.server.foundation.rpc.RPCReqsPayl;
import mcp.server.foundation.rpc.RPCRespPayl;
import mcp.server.foundation.rpc.RPCRouter;
import mcp.server.foundation.rpc.error.ErrClassifier;
import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.TranspContractSupport;
import mcp.server.foundation.transport.TranspSess;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * RTWiring
 *
 * Ansvar:
 * - Bind inbound handler for transport -> JSON-RPC parse -> router -> response
 * - Bounded inbound concurrency som fail-fast backpressure i transportlagret
 * - Throttle pre-init spam, mapped errors, and not-initialized errors
 */
public final class RTWiring {

  private static final String NOT_APPLICABLE = "N/A";

  // =========================================================
  // PRE_INIT THROTTLE CONFIG
  // =========================================================

  private static final long PRE_INIT_THROTTLE_WINDOW_MS = 750L;
  private static final int PRE_INIT_THROTTLE_MAX_TRACKED = 10_000;

  private final ConcurrentHashMap<String, PreInitThrottleState> preInitThrottle = new ConcurrentHashMap<>();

  private record PreInitThrottleState(long lastAcceptedEpochMillis) {
  }

  private static final long MAPPED_ERROR_THROTTLE_WINDOW_MS = 1_500L;
  private static final int MAPPED_ERROR_THROTTLE_MAX_TRACKED = 10_000;

  private final ConcurrentHashMap<String, ThrottleState> mappedErrorThrottle = new ConcurrentHashMap<>();

  private record ThrottleState(long lastLogEpochMillis, long suppressedCount) {
  }

  private static final long NOT_INITIALIZED_THROTTLE_WINDOW_MS = 1_500L;
  private static final int NOT_INITIALIZED_THROTTLE_MAX_TRACKED = 20_000;

  private final ConcurrentHashMap<String, ThrottleState> notInitializedThrottle = new ConcurrentHashMap<>();

  // =========================================================
  // Dependencies
  // =========================================================

  private final TranspAdap transportAdapter;
  private final RPCJsonEntry rpcJsonEntrypoint;
  private final RPCRouter rpcRouter;
  private final ServerLogger logger;
  private final ObservCtxFactory obsCtxFactory;
  private final ErrClassifier errorClassifier;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;
  private final Semaphore inboundConcurrency;
  private final int maxInboundInFlight;

  public RTWiring(
      TranspAdap transportAdapter,
      RPCJsonEntry rpcJsonEntrypoint,
      RPCRouter rpcRouter,
      ServerLogger logger,
      ObservCtxFactory obsCtxFactory,
      ErrClassifier errorClassifier,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      int maxInboundInFlight) {

    this.transportAdapter = Objects.requireNonNull(transportAdapter, "transportAdapter");
    this.rpcJsonEntrypoint = Objects.requireNonNull(rpcJsonEntrypoint, "rpcJsonEntrypoint");
    this.rpcRouter = Objects.requireNonNull(rpcRouter, "rpcRouter");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.errorClassifier = Objects.requireNonNull(errorClassifier, "errorClassifier");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");

    if (maxInboundInFlight <= 0) {
      throw new IllegalArgumentException("maxInboundInFlight must be > 0");
    }

    this.inboundConcurrency = new Semaphore(maxInboundInFlight, true);
    this.maxInboundInFlight = maxInboundInFlight;
  }

  // =========================================================
  // Wiring
  // =========================================================

  public void RTWiringBind() {
    RTWiringResetTransientState();
    transportAdapter.TranspAdapSetMessageHandler(this::RTWiringHandleInboundRawMessage);
    RTWiringUpdInFlightGauge();
  }

  public void RTWiringResetTransientState() {
    preInitThrottle.clear();
    mappedErrorThrottle.clear();
    notInitializedThrottle.clear();
  }

  public void RTWiringClearTranspState(
      String transportConnectionId,
      String mcpSessId) {

    if (transportConnectionId == null || transportConnectionId.isBlank()) {
      return;
    }

    preInitThrottle.remove(transportConnectionId);
    mappedErrorThrottle.remove(transportConnectionId);

    if (mcpSessId == null || mcpSessId.isBlank()) {
      String keyPrefix = transportConnectionId + "|";
      notInitializedThrottle.keySet().removeIf(key -> key != null && key.startsWith(keyPrefix));
      return;
    }

    notInitializedThrottle.remove(transportConnectionId + "|" + mcpSessId);
  }

  // =========================================================
  // PRE_INIT THROTTLE
  // =========================================================

  private boolean RTWiringPreInitShouldAccept(String transportConnectionId, String method) {

    if (transportConnectionId == null || transportConnectionId.isBlank()) {
      return true;
    }

    if (method == null || method.isBlank()) {
      return true;
    }

    // Pre-init throttle only protects repeated initialize calls. Legitimate
    // follow-up discovery calls must remain available after initialization.
    if (!RPCMetName.INITIALIZE.equals(method)) {
      return true;
    }

    if (preInitThrottle.size() > PRE_INIT_THROTTLE_MAX_TRACKED) {
      preInitThrottle.clear();
    }

    long now = System.currentTimeMillis();

    PreInitThrottleState newState = preInitThrottle.compute(transportConnectionId, (k, prev) -> {

      if (prev == null) {
        return new PreInitThrottleState(now);
      }

      long since = now - prev.lastAcceptedEpochMillis;

      if (since >= PRE_INIT_THROTTLE_WINDOW_MS) {
        return new PreInitThrottleState(now);
      }

      // reject (keep old timestamp)
      return prev;
    });

    return newState.lastAcceptedEpochMillis == now;
  }

  private boolean RTWiringMappedErrShouldLogNow(String transportConnectionId, RPCErr err) {

    if (transportConnectionId == null || transportConnectionId.isBlank()) {
      return true;
    }

    return RTWiringShouldLogWithThrottle(
        mappedErrorThrottle,
        transportConnectionId,
        MAPPED_ERROR_THROTTLE_MAX_TRACKED,
        MAPPED_ERROR_THROTTLE_WINDOW_MS);
  }

  private String RTWiringMappedErrBuildLogSuffix(String transportConnectionId) {

    if (transportConnectionId == null || transportConnectionId.isBlank()) {
      return "";
    }

    return RTWiringSuppressedSuffix(mappedErrorThrottle, transportConnectionId);
  }

  private boolean RTWiringNotInitializedShouldLogNow(String transportConnectionId, String mcpSessId) {

    if (transportConnectionId == null || transportConnectionId.isBlank()) {
      return true;
    }

    if (mcpSessId == null || mcpSessId.isBlank()) {
      return true;
    }

    String key = RTWiringNotInitializedThrottleKey(transportConnectionId, mcpSessId);

    return RTWiringShouldLogWithThrottle(
        notInitializedThrottle,
        key,
        NOT_INITIALIZED_THROTTLE_MAX_TRACKED,
        NOT_INITIALIZED_THROTTLE_WINDOW_MS);
  }

  private String RTWiringNotInitializedBuildLogSuffix(String transportConnectionId, String mcpSessId) {

    if (transportConnectionId == null || transportConnectionId.isBlank()) {
      return "";
    }

    if (mcpSessId == null || mcpSessId.isBlank()) {
      return "";
    }

    String key = RTWiringNotInitializedThrottleKey(transportConnectionId, mcpSessId);

    return RTWiringSuppressedSuffix(notInitializedThrottle, key);
  }

  private static boolean RTWiringShouldLogWithThrottle(
      ConcurrentHashMap<String, ThrottleState> throttle,
      String key,
      int maxTracked,
      long windowMillis) {

    if (key == null || key.isBlank()) {
      return true;
    }

    if (throttle.size() > maxTracked) {
      throttle.clear();
    }

    long now = System.currentTimeMillis();

    ThrottleState newState = throttle.compute(key, (ignored, previous) -> {

      if (previous == null) {
        return new ThrottleState(now, 0L);
      }

      long since = now - previous.lastLogEpochMillis;

      if (since >= windowMillis) {
        return new ThrottleState(now, 0L);
      }

      return new ThrottleState(previous.lastLogEpochMillis, previous.suppressedCount + 1L);
    });

    return newState.lastLogEpochMillis == now;
  }

  private static String RTWiringSuppressedSuffix(
      ConcurrentHashMap<String, ThrottleState> throttle,
      String key) {

    if (key == null || key.isBlank()) {
      return "";
    }

    ThrottleState state = throttle.get(key);
    if (state == null) {
      return "";
    }
    if (state.suppressedCount <= 0) {
      return "";
    }
    return " (suppressed=" + state.suppressedCount + ")";
  }

  private static String RTWiringNotInitializedThrottleKey(
      String transportConnectionId,
      String mcpSessId) {

    return transportConnectionId + "|" + mcpSessId;
  }

  // =========================================================
  // Inbound
  // =========================================================

  private void RTWiringHandleInboundRawMessage(
      TranspSess session,
      String rawText) {

    Objects.requireNonNull(session, "session");

    if (rawText == null || rawText.isBlank()) {
      return;
    }

    String transportConnectionId = session.TranspSessGetTranspConnId();
    String mcpSessId = session.TranspSessGetMcpSessId();
    String corr = NOT_APPLICABLE;
    ObservCtx rawContext = obsCtxFactory.ObservCtxFactoryFromTranspSess(session);
    long inboundStartedAt = System.nanoTime();
    String metricPrefix = RTWiringMetricPrefix(session);

    logger.ServerLogInfoObserved(
        ServerLogger.Component.RPC,
        rawContext,
        "RECEIVE",
        "RPC_RAW_MESSAGE_RECEIVED",
        "RTWiring: raw inbound RPC payload bytes=" + rawText.length());

    RPCReqsPayl request;
    ObservCtx rpcContext = null;
    boolean acquiredInboundSlot = false;

    try {
      long parseStartedAt = System.nanoTime();
      request = rpcJsonEntrypoint.RPCJsonEntryParseReqs(rawText);
      runtimeMetrics.RTMetricsRecordTimerMillis(
          metricPrefix + ".rpc.parse.duration",
          TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - parseStartedAt)));

      if (request == null) {
        return;
      }

      corr = RPCRouter.RPCRouterResolveCorrelaId(request);
      rpcContext = obsCtxFactory.ObservCtxFactoryForRpc(
          session,
          corr,
          request.RPCReqPlGetMet());

      logger.ServerLogInfoObserved(
          ServerLogger.Component.RPC,
          rpcContext,
          "PARSE",
          "RPC_REQUEST_PARSED",
          "RTWiring: request parsed");

      if (!request.RPCReqPlIsNotification()) {
        telemetryMetrics.McpTelemIncrementSessReqs(rpcContext);
      }

      if (!inboundConcurrency.tryAcquire()) {

        String errorType = "BACKPRESSURE_REJECTED";
        session.TranspSessRecordRejected(corr);
        RPCRespPayl err = transportAdapter.TranspAdapMapOverload(request);

        if (err != null) {
          RTWiringSendResponse(session, err, rpcContext);
        }

        runtimeMetrics.RTMetricsIncrementCounter(
            RTWiringMetricPrefix(session) + ".capacity.rejected.total");

        logger.ServerLogWarnObserved(
            ServerLogger.Component.RPC,
            obsCtxFactory.ObservCtxFactoryWithErrType(rpcContext, errorType),
            "REJECT",
            "RPC_TRANSPORT_BACKPRESSURE_REJECTED",
            "RTWiring: inbound transport backpressure reject",
            errorType);
        telemetryMetrics.McpTelemRecordRpc(
            obsCtxFactory.ObservCtxFactoryWithErrType(rpcContext, errorType),
            "rejected",
            errorType,
            durationFrom(rpcContext));

        return;
      }

      acquiredInboundSlot = true;
      RTWiringUpdInFlightGauge();
      session.TranspSessRecordReceived(corr);

      if (!RTWiringPreInitShouldAccept(transportConnectionId, request.RPCReqPlGetMet())) {
        session.TranspSessRecordRejected(corr);

        logger.ServerLogWarnObserved(
            ServerLogger.Component.RPC,
            obsCtxFactory.ObservCtxFactoryWithErrType(rpcContext, "THROTTLE_REJECTED"),
            "REJECT",
            "RPC_PRE_INIT_THROTTLE_REJECTED",
            "RTWiring: pre-init throttle reject method=" + request.RPCReqPlGetMet(),
            "THROTTLE_REJECTED");
        telemetryMetrics.McpTelemRecordRpc(
            obsCtxFactory.ObservCtxFactoryWithErrType(rpcContext, "THROTTLE_REJECTED"),
            "rejected",
            "THROTTLE_REJECTED",
            durationFrom(rpcContext));

        RPCRespPayl err = RPCRouter.RPCRouterMapMappedExcepToResponse(
            RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
                "PRE_INIT throttle: too frequent"),
            request);

        if (err != null) {
          RTWiringSendResponse(session, err, rpcContext);
        }

        logger.ServerLogInfoObserved(
            ServerLogger.Component.RPC,
            rpcContext,
            "SEND",
            "RPC_RESPONSE_SENT",
            "RTWiring: throttle response sent",
            durationFrom(rpcContext));

        return;
      }

    } catch (RuntimeException ex) {
      runtimeMetrics.RTMetricsRecordTimerMillis(
          metricPrefix + ".rpc.parse.duration",
          TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - inboundStartedAt)));

      String errorType = errorClassifier.ErrClassifierClassify(ex).name();
      RPCRespPayl parseErr = transportAdapter.TranspAdapMapParseErr(ex);

      transportAdapter.TranspAdapSendTo(
          session,
          rpcJsonEntrypoint.RPCJsonEntryToRespJson(parseErr));

      logger.ServerLogErrorObserved(
          ServerLogger.Component.RPC,
          obsCtxFactory.ObservCtxFactoryWithErrType(rawContext, errorType),
          "PARSE",
          "RPC_PARSE_FAILED",
          "RTWiring: failed parse inbound request: " + ex.getMessage(),
          errorType,
          ex);
      telemetryMetrics.McpTelemRecordRpc(
          obsCtxFactory.ObservCtxFactoryWithErrType(rawContext, errorType),
          "parse_error",
          errorType,
          TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - inboundStartedAt)));

      return;
    }

    // Router execution
    RPCRespPayl response;
    long routeStartedAt = System.nanoTime();

    try {
      session.TranspSessRecordRouted(corr);

      ObservCtxHolder.Scope holderScope = ObservCtxHolder.ObservCtxHolderOpenScope(rpcContext);
      try {
        response = rpcRouter.RPCRouterRoute(request, session);
      } finally {
        holderScope.close();
      }

    } catch (RPCMappedExcep mapped) {

      RPCErr rpcErr = mapped.RPCMappedExcepGetRPCErr();
      String errorType = errorClassifier.ErrClassifierClassify(mapped).name();

      RPCRespPayl err = RPCRouter.RPCRouterMapMappedExcepToResponse(mapped, request);

      if (err != null) {
        RTWiringSendResponse(session, err, rpcContext);
      }

      boolean isNotInitialized = rpcErr != null && "Session not initialized".equals(rpcErr.RPCErrGetMessage());

      if (isNotInitialized) {

        if (RTWiringNotInitializedShouldLogNow(transportConnectionId, mcpSessId)) {

          String suffix = RTWiringNotInitializedBuildLogSuffix(transportConnectionId, mcpSessId);

          logger.ServerLogWarnObserved(
              ServerLogger.Component.RPC,
              obsCtxFactory.ObservCtxFactoryWithErrType(rpcContext, errorType),
              "ERROR",
              "RPC_MAPPED_ERROR_RETURNED",
              "RTWiring: router mapped error: code="
                  + (rpcErr == null ? "null" : rpcErr.RPCErrGetCode())
                  + " msg="
                  + (rpcErr == null ? "null" : rpcErr.RPCErrGetMessage())
                  + suffix,
              errorType);
        }

      } else if (RTWiringMappedErrShouldLogNow(transportConnectionId, rpcErr)) {

        String suffix = RTWiringMappedErrBuildLogSuffix(transportConnectionId);

        logger.ServerLogWarnObserved(
            ServerLogger.Component.RPC,
            obsCtxFactory.ObservCtxFactoryWithErrType(rpcContext, errorType),
            "ERROR",
            "RPC_MAPPED_ERROR_RETURNED",
            "RTWiring: router mapped error: code="
                + (rpcErr == null ? "null" : rpcErr.RPCErrGetCode())
                + " msg="
                + (rpcErr == null ? "null" : rpcErr.RPCErrGetMessage())
                + suffix,
            errorType);
      }

      telemetryMetrics.McpTelemRecordRpc(
          obsCtxFactory.ObservCtxFactoryWithErrType(
              rpcContext,
              errorType),
          "error",
          errorType,
          durationFrom(rpcContext));

      return;
    }

    catch (RuntimeException ex) {

      RPCRespPayl err = RPCRouter.RPCRouterMapRuntimeExcepToResponse(ex, request);

      String corr2 = RPCRouter.RPCRouterResolveCorrelaId(request);

      if (err != null) {
        RTWiringSendResponse(session, err, rpcContext);
      }

      String errorType = errorClassifier.ErrClassifierClassify(ex).name();

      logger.ServerLogErrorObserved(
          ServerLogger.Component.RPC,
          obsCtxFactory.ObservCtxFactoryWithErrType(
              obsCtxFactory.ObservCtxFactoryForRpc(session, corr2, request.RPCReqPlGetMet()),
              errorType),
          "ERROR",
          "RPC_UNEXPECTED_ERROR",
          "RTWiring: router failed: " + ex.getMessage(),
          durationFrom(rpcContext),
          errorType,
          ex);
      telemetryMetrics.McpTelemRecordRpc(
          obsCtxFactory.ObservCtxFactoryWithErrType(rpcContext, errorType),
          "error",
          errorType,
          durationFrom(rpcContext));

      return;
    } finally {
      runtimeMetrics.RTMetricsRecordTimerMillis(
          metricPrefix + ".rpc.route.duration",
          TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - routeStartedAt)));
      if (acquiredInboundSlot) {
        inboundConcurrency.release();
        RTWiringUpdInFlightGauge();
      }
    }

    if (response != null) {
      RTWiringSendResponse(session, response, rpcContext);
      telemetryMetrics.McpTelemRecordRpc(rpcContext, "success", null, durationFrom(rpcContext));

      logger.ServerLogInfoObserved(
          ServerLogger.Component.RPC,
          rpcContext,
          "SEND",
          "RPC_RESPONSE_SENT",
          "RTWiring: response sent",
          durationFrom(rpcContext));
    } else {
      telemetryMetrics.McpTelemRecordRpc(rpcContext, "notification", null, durationFrom(rpcContext));
    }
  }

  private static Long durationFrom(ObservCtx context) {

    if (context == null || context.ObservCtxGetReqsStartNano() == null) {
      return null;
    }

    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - context.ObservCtxGetReqsStartNano());
  }

  private void RTWiringSendResponse(
      TranspSess session,
      RPCRespPayl response,
      ObservCtx context) {
    long sendStartedAt = System.nanoTime();

    transportAdapter.TranspAdapSendTo(
        session,
        rpcJsonEntrypoint.RPCJsonEntryToRespJson(response));
    telemetryMetrics.McpTelemIncrementSessResp(context);

    Long durationMillis = durationFrom(context);
    if (durationMillis != null) {
      runtimeMetrics.RTMetricsRecordTimerMillis(
          RTWiringMetricPrefix(session) + ".rpc.duration",
          durationMillis);
      runtimeMetrics.RTMetricsRecordTimerMillis(
          RTWiringMetricPrefix(session) + ".transport.roundtrip.duration",
          durationMillis);
    }
    runtimeMetrics.RTMetricsRecordTimerMillis(
        RTWiringMetricPrefix(session) + ".rpc.send.duration",
        TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - sendStartedAt)));
  }

  private void RTWiringUpdInFlightGauge() {
    runtimeMetrics.RTMetricsSetGauge(
        transportAdapter.TranspAdapMetricPrefix() + ".messages.inflight",
        Math.max(0, maxInboundInFlight - inboundConcurrency.availablePermits()));
  }

  private static String RTWiringMetricPrefix(TranspSess session) {

    if (session == null) {
      return "transport";
    }

    return TranspContractSupport.TransContMetricPrefix(session.TranspSessGetTranspName());
  }
}
