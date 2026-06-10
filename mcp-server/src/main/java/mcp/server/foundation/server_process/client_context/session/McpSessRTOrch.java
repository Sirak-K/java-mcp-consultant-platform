package mcp.server.foundation.server_process.client_context.session;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpMetricCatal;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspSignalModel;
import mcp.server.foundation.rpc.RPCMetName;
import mcp.server.foundation.rpc.RPCRespPayl;
import mcp.server.foundation.rpc.RPCSessPhase;
import mcp.server.foundation.server_process.client_context.session.event.McpSessEventPayl;
import mcp.server.foundation.server_process.client_context.session.event.McpSessEventPubl;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMeta;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.server_process.client_context.session.persistence.service.McpSessRTStore;
import mcp.server.foundation.server_process.orchestration.RTMcpSessPhase;
import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.TranspSess;
import mcp.server.foundation.transport.websocket.WsConnId;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * McpSessRTOrch
 *
 * Foundation-level Runtime Orch för MCP session lifecycle.
 *
 * Ansvar:
 * - Logical MCP session open/close
 * - Bind/unbind logical ↔ physical identity
 * - Emit session notifications (CONNECTED / INITIALIZED / CLOSED)
 *
 * Ingen Spring-koppling.
 */
public final class McpSessRTOrch {

  private static final String WS_UNBOUND = "UNBOUND";

  private static final long PRE_INIT_TTL_SECONDS = 10L;

  private static final ScheduledExecutorService PRE_INIT_TTL_EXECUTOR = Executors
      .newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          Thread t = new Thread(r, "mcp-preinit-ttl");
          t.setDaemon(true);
          return t;
        }
      });

  private final ConcurrentHashMap<McpSessId, ScheduledFuture<?>> preInitTtlTasks = new ConcurrentHashMap<>();

  // Warn-throttle for buggy clients creating repeated PRE_INIT timeouts.
  // Keyed by wsConnId (physical identity), falls back to UNBOUND.
  private final ConcurrentHashMap<String, Long> preInitTtlWarnLastAtMs = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Integer> preInitTtlWarnSuppressed = new ConcurrentHashMap<>();

  private final McpSessReg registry;
  private final McpSessBindingReg bindingRegistry;
  private final McpSessEventPubl publisher;
  private final TranspAdap transportAdapter;
  private final ServerLogger logger;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;
  private final ObservCtxFactory obsCtxFactory;
  private final McpSessRTMetaFactory runtimeMetaFactory;
  private final McpSessRTStore runtimeSessionStore;

  public McpSessRTOrch(
      McpSessReg registry,
      McpSessBindingReg bindingRegistry,
      McpSessEventPubl publisher,
      TranspAdap transportAdapter,
      ServerLogger logger,
      RTMetrics runtimeMetrics,
      ObservCtxFactory obsCtxFactory) {

    this(
        registry,
        bindingRegistry,
        publisher,
        transportAdapter,
        logger,
        runtimeMetrics,
        McpTelemMetrics.McpTelemNoOp(),
        obsCtxFactory,
        new McpSessRTMetaFactory(),
        null);
  }

  public McpSessRTOrch(
      McpSessReg registry,
      McpSessBindingReg bindingRegistry,
      McpSessEventPubl publisher,
      TranspAdap transportAdapter,
      ServerLogger logger,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      ObservCtxFactory obsCtxFactory,
      McpSessRTMetaFactory runtimeMetaFactory) {

    this(
        registry,
        bindingRegistry,
        publisher,
        transportAdapter,
        logger,
        runtimeMetrics,
        telemetryMetrics,
        obsCtxFactory,
        runtimeMetaFactory,
        null);
  }

  public McpSessRTOrch(
      McpSessReg registry,
      McpSessBindingReg bindingRegistry,
      McpSessEventPubl publisher,
      TranspAdap transportAdapter,
      ServerLogger logger,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      ObservCtxFactory obsCtxFactory,
      McpSessRTMetaFactory runtimeMetaFactory,
      McpSessRTStore runtimeSessionStore) {

    this.registry = Objects.requireNonNull(registry, "registry");
    this.bindingRegistry = Objects.requireNonNull(bindingRegistry, "bindingRegistry");
    this.publisher = Objects.requireNonNull(publisher, "publisher");
    this.transportAdapter = Objects.requireNonNull(transportAdapter, "transportAdapter");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.runtimeMetaFactory = Objects.requireNonNull(runtimeMetaFactory, "runtimeMetaFactory");
    this.runtimeSessionStore = runtimeSessionStore;
  }

  // =========================================================
  // PRE INIT TTL (Defensive cleanup)
  // =========================================================

  void McpSessRTOrchSchedulePreInitTtl(McpSessId mcpSessId) {

    ScheduledFuture<?> prev = preInitTtlTasks.remove(mcpSessId);
    if (prev != null) {
      prev.cancel(false);
    }

    ScheduledFuture<?> fut = PRE_INIT_TTL_EXECUTOR.schedule(() -> {
      try {
        var phase = registry.SessRegGetPhase(mcpSessId);

        if (phase == null) {
          return;
        }

        // TTL gäller endast om sessionen aldrig nådde POST_INIT.
        if (phase == RPCSessPhase.POST_INIT) {
          return;
        }

        if (phase == RPCSessPhase.CLOSED) {
          return;
        }

        // Pre-init zombie: auto-destroy.
        McpSessRTOrchCloseFinal(mcpSessId);

        String transportConnectionId = McpSessRTOrchNormalizeTranspConnId(
            bindingRegistry.getTranspConnId(mcpSessId));

        if (McpSessRTOrchPreInitTtlShouldLogNow(transportConnectionId)) {

          String suffix = McpSessRTOrchPreInitTtlBuildLogSuffix(transportConnectionId);
          runtimeMetrics.RTMetricsIncrementCounter("mcp.sessions.pre_init_ttl_expired.total");

          ObservCtx context = obsCtxFactory.ObservCtxFactoryWithErrType(
              obsCtxFactory.ObservCtxFactoryForTranspLifecycle(
                  McpSessRTOrchResolveTranspName(mcpSessId),
                  transportConnectionId,
                  mcpSessId.toString(),
                  RPCSessPhase.PRE_INIT.name()),
              "THROTTLE_REJECTED");

          logger.ServerLogWarnObserved(
              ServerLogger.Component.MCP,
              context,
              "EXPIRE",
              "MCP_SESSION_PRE_INIT_TTL_EXPIRED",
              "McpSessRTOrch: PRE_INIT TTL expired -> auto-destroy (10s)" + suffix,
              "THROTTLE_REJECTED");
        }

      } catch (Exception ignored) {
        // TTL cleanup must never throw.
      }
    }, PRE_INIT_TTL_SECONDS, TimeUnit.SECONDS);

    preInitTtlTasks.put(mcpSessId, fut);
  }

  private void McpSessRTOrchCancelPreInitTtl(McpSessId mcpSessId) {

    ScheduledFuture<?> fut = preInitTtlTasks.remove(mcpSessId);
    if (fut != null) {
      fut.cancel(false);
    }
  }

  private boolean McpSessRTOrchPreInitTtlShouldLogNow(String wsId) {

    if (wsId == null || wsId.isBlank()) {
      wsId = WS_UNBOUND;
    }

    long now = System.currentTimeMillis();
    Long prev = preInitTtlWarnLastAtMs.putIfAbsent(wsId, now);

    if (prev == null) {
      return true;
    }

    long delta = now - prev;
    if (delta >= 1000L) {
      preInitTtlWarnLastAtMs.put(wsId, now);
      return true;
    }

    preInitTtlWarnSuppressed.compute(wsId, (key, current) -> current == null ? 1 : current + 1);
    return false;
  }

  private String McpSessRTOrchPreInitTtlBuildLogSuffix(String wsId) {

    if (wsId == null || wsId.isBlank()) {
      wsId = WS_UNBOUND;
    }

    Integer suppressed = preInitTtlWarnSuppressed.remove(wsId);
    if (suppressed == null || suppressed <= 0) {
      return "";
    }

    return " (suppressed=" + suppressed + ")";
  }

  // =========================================================
  // OPEN
  // =========================================================

  public McpSessId McpSessRTOrchOpen(TranspSess session) {

    Objects.requireNonNull(session, "session");

    McpSessId mcpSessId = session.TranspSessGetMcpSessIdObject();
    String transportConnectionId = McpSessRTOrchNormalizeTranspConnId(session.TranspSessGetTranspConnId());
    String transportName = session.TranspSessGetTranspName();

    if (mcpSessId == null) {
      throw new IllegalStateException("McpSessRTOrchOpen requires TranspSess with McpSessId");
    }

    registry.SessRegRegisterConnected(mcpSessId);
    McpSessRTOrchSyncRuntimeMeta(mcpSessId, session.TranspSessGetRuntimeMeta());

    McpSessRTOrchSchedulePreInitTtl(mcpSessId);

    bindingRegistry.bind(mcpSessId, session);

    runtimeMetrics.RTMetricsIncrementCounter("mcp.sessions.created.total");
    runtimeMetrics.RTMetricsSetGauge("mcp.sessions.active", registry.SessRegGetActiveSessCount());
    runtimeMetrics.RTMetricsSetGauge(
        McpMetricCatal.MCP_SERVER_LAST_SESSION_CREATED_TIMESTAMP_SECONDS,
        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    telemetryMetrics.McpTelemSessionOpened(
        mcpSessId.toString(),
        transportName,
        RPCSessPhase.PRE_INIT.name());

    publisher.SessEventPublPublish(
        new McpSessEventPayl(
            McpSessEventPayl.EventType.CONNECTED,
            mcpSessId.toString()));

    ObservCtx openContext = obsCtxFactory.ObservCtxFactoryForTranspLifecycle(
        transportName,
        transportConnectionId,
        mcpSessId.toString(),
        RPCSessPhase.PRE_INIT.name());

    logger.ServerLogInfoObserved(
        ServerLogger.Component.MCP,
        openContext,
        "OPEN",
        "MCP_SESSION_CREATED",
        "McpSessRTOrch: OPEN + BIND ok");

    logger.ServerLogInfoObserved(
        ServerLogger.Component.MCP,
        openContext,
        "BIND",
        McpSessRTOrchBindEventName(transportName),
        "McpSessRTOrch: session bound to transport connection");

    return mcpSessId;
  }

  // =========================================================
  // RPC SIDE EFFECTS
  // =========================================================

  public void McpSessRTOrchApplyRpcSideEffects(
      McpSessId mcpSessId,
      String rpcMet,
      RPCRespPayl response) {

    Objects.requireNonNull(mcpSessId, "mcpSessId");

    if (rpcMet == null || rpcMet.isBlank()) {
      return;
    }

    if (response == null || response.RPCRespPlHasErr()) {
      return;
    }

    if (RPCMetName.SESSIONS_SUBSCRIBE.equals(rpcMet)) {
      McpSessRTOrchHandleSubscribe(mcpSessId);
      return;
    }

    if (RPCMetName.INITIALIZE.equals(rpcMet)) {
      McpSessRTOrchMarkAsInitialized(mcpSessId);
      return;
    }
  }

  private void McpSessRTOrchHandleSubscribe(McpSessId mcpSessId) {

    Objects.requireNonNull(mcpSessId, "mcpSessId");

    // Idempotency guard: replay skickas exakt en gång per session.
    boolean ok = registry.SessRegTryMarkAsSentinel(mcpSessId);

    if (!ok) {
      return;
    }

    McpSessRTOrchTouchRuntimeMeta(mcpSessId);

    // Retroactive snapshot replay (subscribe + initial state pattern):
    // Push all currently INITIALIZED sessions so subscriber gets consistent
    // state even if it subscribed after those sessions were established.
    for (McpSessId id : registry.SessRegGetAllSessIds()) {
      if (registry.SessRegIsInitialized(id)) {
        publisher.SessEventPublPublishTo(
            mcpSessId,
            new McpSessEventPayl(McpSessEventPayl.EventType.INITIALIZED, id.toString()));
      }
    }

    String transportConnectionId = McpSessRTOrchNormalizeTranspConnId(
        bindingRegistry.getTranspConnId(mcpSessId));
    ObservCtx subscribeContext = obsCtxFactory.ObservCtxFactoryForTranspLifecycle(
        McpSessRTOrchResolveTranspName(mcpSessId),
        transportConnectionId,
        mcpSessId.toString(),
        registry.SessRegGetPhase(mcpSessId).name());

    logger.ServerLogInfoObserved(
        ServerLogger.Component.MCP,
        subscribeContext,
        "PUBLISH",
        "MCP_SESSION_NOTIFICATION_PUBLISHED",
        "McpSessRTOrch: sessions/subscribe snapshot replay ok");
  }

  private void McpSessRTOrchMarkAsInitialized(McpSessId mcpSessId) {

    Objects.requireNonNull(mcpSessId, "mcpSessId");

    boolean ok = registry.SessRegTryMarkInitialized(mcpSessId);

    if (!ok) {
      return;
    }

    McpSessRTOrchAdvanceRuntimePhase(mcpSessId, RTMcpSessPhase.ACTIVE);
    McpSessRTOrchCancelPreInitTtl(mcpSessId);
    telemetryMetrics.McpTelemSessionPhaseUpdated(
        mcpSessId.toString(),
        McpSessRTOrchResolveTranspName(mcpSessId),
        RPCSessPhase.POST_INIT.name());

    publisher.SessEventPublPublish(
        new McpSessEventPayl(
            McpSessEventPayl.EventType.INITIALIZED,
            mcpSessId.toString()));

    String transportConnectionId = McpSessRTOrchNormalizeTranspConnId(
        bindingRegistry.getTranspConnId(mcpSessId));
    ObservCtx initializedContext = obsCtxFactory.ObservCtxFactoryForTranspLifecycle(
        McpSessRTOrchResolveTranspName(mcpSessId),
        transportConnectionId,
        mcpSessId.toString(),
        RPCSessPhase.POST_INIT.name());

    logger.ServerLogInfoObserved(
        ServerLogger.Component.MCP,
        initializedContext,
        "INITIALIZE",
        "MCP_SESSION_INITIALIZED",
        "McpSessRTOrch: INITIALIZED ok");
  }

  // =========================================================
  // CLOSE
  // =========================================================

  public void McpSessRTOrchClose(McpSessId mcpSessId) {

    if (mcpSessId == null) {
      return;
    }

    // Soft close: used by transport close path.
    McpSessRTOrchCloseFinal(mcpSessId);
  }

  public void McpSessRTOrchCloseAll() {

    for (McpSessId id : registry.SessRegGetAllSessIds()) {
      McpSessRTOrchCloseFinal(id);
    }
  }

  public void McpSessRTOrchForgetTranspConn(String transportConnectionId) {

    String normalizedTranspConnectionId = McpSessRTOrchNormalizeTranspConnId(transportConnectionId);
    preInitTtlWarnLastAtMs.remove(normalizedTranspConnectionId);
    preInitTtlWarnSuppressed.remove(normalizedTranspConnectionId);
  }

  private void McpSessRTOrchCloseFinal(McpSessId mcpSessId) {

    if (mcpSessId == null) {
      return;
    }

    McpSessRTOrchCancelPreInitTtl(mcpSessId);

    // If already closed -> no-op.
    boolean closed = registry.SessRegTryMarkClosed(mcpSessId);

    if (!closed) {
      return;
    }

    McpSessRTOrchAdvanceRuntimePhase(mcpSessId, RTMcpSessPhase.CLOSED);

    String transportConnectionId = bindingRegistry.getTranspConnId(mcpSessId);
    String transportName = McpSessRTOrchResolveTranspName(mcpSessId);

    transportAdapter.TranspAdapCloseSessById(mcpSessId.asString());

    if (transportConnectionId != null) {
      bindingRegistry.unbind(mcpSessId);
    }

    try {
      publisher.SessEventPublPublish(
          new McpSessEventPayl(
              McpSessEventPayl.EventType.CLOSED,
              mcpSessId.toString()));

      runtimeMetrics.RTMetricsIncrementCounter("mcp.sessions.closed.total");
      runtimeMetrics.RTMetricsSetGauge(
          McpMetricCatal.MCP_SERVER_LAST_SESSION_CLOSED_TIMESTAMP_SECONDS,
          TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
      telemetryMetrics.McpTelemSessClosed(mcpSessId.toString(), transportName, "normal_close");

      ObservCtx closeContext = obsCtxFactory.ObservCtxFactoryForTranspLifecycle(
          transportName,
          McpSessRTOrchNormalizeTranspConnId(transportConnectionId),
          mcpSessId.toString(),
          RPCSessPhase.CLOSED.name());

      logger.ServerLogInfoObserved(
          ServerLogger.Component.MCP,
          closeContext,
          "CLOSE",
          "MCP_SESSION_CLOSED",
          "McpSessRTOrch: CLOSED ok");
    } finally {
      McpSessRTOrchDeletePersistedRuntimeMeta(mcpSessId);
      // Remove the logical session only after broadcasting CLOSED so lifecycle
      // observers can still correlate the event with a valid session id.
      registry.SessRegCloseFinal(mcpSessId);
      runtimeMetrics.RTMetricsSetGauge("mcp.sessions.active", registry.SessRegGetActiveSessCount());
      McpSessRTOrchForgetTranspConn(transportConnectionId);
    }
  }

  // =========================================================
  // Transp lost
  // =========================================================

  public void McpSessRTOrchMarkTranspLost(McpSessId mcpSessId, WsConnId wsConnId) {

    if (mcpSessId == null) {
      return;
    }

    if (wsConnId == null) {
      return;
    }

    registry.SessRegMarkTranspLost(mcpSessId);
    runtimeMetrics.RTMetricsIncrementCounter(
        TranspSignalModel.TransSigTranspErrorsMetricName("websocket"));

    logger.ServerLogWarnObserved(
        ServerLogger.Component.WS,
        obsCtxFactory.ObservCtxFactoryWithErrType(
            obsCtxFactory.ObservCtxFactoryFromTranspCoordinates(
                "websocket",
                wsConnId.toString(),
                mcpSessId.toString()),
            "TRANSPORT_ERROR"),
        "ERROR",
        "WS_TRANSPORT_ERROR",
        "McpSessRTOrch: TRANSPORT LOST",
        "TRANSPORT_ERROR");
  }

  private static String McpSessRTOrchNormalizeTranspConnId(String transportConnectionId) {
    return transportConnectionId == null || transportConnectionId.isBlank()
        ? WS_UNBOUND
        : transportConnectionId;
  }

  private String McpSessRTOrchResolveTranspName(McpSessId mcpSessId) {

    String transportName = bindingRegistry.getTranspName(mcpSessId);

    return transportName == null || transportName.isBlank()
        ? TranspSess.TRANSPORT_WEBSOCKET
        : transportName;
  }

  private static String McpSessRTOrchBindEventName(String transportName) {
    if (TranspSess.TRANSPORT_WEBSOCKET.equals(transportName)) {
      return "MCP_SESSION_BOUND_TO_WS_CONNECTION";
    }
    return "MCP_SESSION_BOUND_TO_TRANSPORT_CONNECTION";
  }

  private void McpSessRTOrchTouchRuntimeMeta(McpSessId mcpSessId) {

    McpSessRTMeta current = registry.SessRegGetRuntimeMeta(mcpSessId);
    if (current == null) {
      return;
    }

    McpSessRTOrchSyncRuntimeMeta(
        mcpSessId,
        runtimeMetaFactory.McpSessRTMetaFactoryTouch(current));
  }

  private void McpSessRTOrchAdvanceRuntimePhase(
      McpSessId mcpSessId,
      RTMcpSessPhase nextPhase) {

    McpSessRTMeta current = registry.SessRegGetRuntimeMeta(mcpSessId);
    if (current == null) {
      return;
    }

    McpSessRTOrchSyncRuntimeMeta(
        mcpSessId,
        runtimeMetaFactory.McpSessRTMetaFactoryAdvancePhase(current, nextPhase));
  }

  private void McpSessRTOrchSyncRuntimeMeta(
      McpSessId mcpSessId,
      McpSessRTMeta runtimeMeta) {

    if (mcpSessId == null || runtimeMeta == null) {
      return;
    }

    registry.SessRegSetRuntimeMeta(mcpSessId, runtimeMeta);

    TranspSess liveSession = transportAdapter.TranspAdapGetSessionById(mcpSessId.asString());
    if (liveSession != null) {
      liveSession.TranspSessSetRuntimeMeta(runtimeMeta);
    }

    McpSessRTOrchPersistRuntimeMeta(runtimeMeta);
  }

  private void McpSessRTOrchPersistRuntimeMeta(McpSessRTMeta runtimeMeta) {

    if (runtimeSessionStore == null || runtimeMeta == null) {
      return;
    }

    if (!runtimeMeta.McpSessRTMetaIsDurableTarget()) {
      return;
    }

    runtimeSessionStore.persist(runtimeMeta);
  }

  private void McpSessRTOrchDeletePersistedRuntimeMeta(McpSessId mcpSessId) {

    if (runtimeSessionStore == null || mcpSessId == null) {
      return;
    }

    runtimeSessionStore.delete(mcpSessId);
  }
}
