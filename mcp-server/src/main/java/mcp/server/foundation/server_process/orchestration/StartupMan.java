package mcp.server.foundation.server_process.orchestration;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpMetricCatal;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.server_process.client_context.session.McpSessRTOrch;
import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.TranspSess;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * StartupMan
 *
 * Ansvar:
 * - Starta transport
 * - Binda RTWiring inbound handler
 * - Binda transport session open/close hooks -> SessionRTOrch
 *
 * OBS:
 * - RTStatus transitions ägs av McpRTOrch (strict).
 * - StartupMan får inte markera STARTING/RUNNING.
 */
public final class StartupMan {

  private static final String MCP_NOT_APPLICABLE = "N/A";
  private static final String TRANSPORT_UNBOUND = "UNBOUND";
  private static final String METRIC_RUNTIME_STARTUP_DURATION = "runtime.startup.duration";
  private static final String METRIC_TRANSPORT_START_DURATION = "runtime.transport.start.duration";
  private static final String METRIC_RUNTIME_STARTUPS_TOTAL = "runtime.startups.total";

  private final TranspAdap transportAdapter;
  private final RTWiring wiring;
  private final McpSessRTOrch sessionRTOrch;
  private final ServerLogger serverLogger;
  private final ObservCtxFactory obsCtxFactory;
  private final RTMetrics runtimeMetrics;
  private final ServerLifecyStateStore lifecycleStateStore;

  // Idempotency guards: protect against transport/adapter double-callbacks.
  private final Set<String> openTranspConnectionIds = ConcurrentHashMap.newKeySet();

  public StartupMan(
      TranspAdap transportAdapter,
      RTWiring wiring,
      McpSessRTOrch sessionRTOrch,
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics) {

    this(
        transportAdapter,
        wiring,
        sessionRTOrch,
        serverLogger,
        obsCtxFactory,
        runtimeMetrics,
        ServerLifecyStateStore.ServerLifeStateNoOp());
  }

  public StartupMan(
      TranspAdap transportAdapter,
      RTWiring wiring,
      McpSessRTOrch sessionRTOrch,
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      ServerLifecyStateStore lifecycleStateStore) {

    this.transportAdapter = Objects.requireNonNull(transportAdapter, "transportAdapter");
    this.wiring = Objects.requireNonNull(wiring, "wiring");
    this.sessionRTOrch = Objects.requireNonNull(sessionRTOrch, "sessionRTOrch");
    this.serverLogger = Objects.requireNonNull(serverLogger, "serverLogger");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
  }

  public boolean StartupManStart() {
    long startupStartedAt = System.nanoTime();

    try {
      openTranspConnectionIds.clear();
      wiring.RTWiringBind();

      transportAdapter.TranspAdapSetSessionOpenHandler(this::StartupManOnTranspOpen);
      transportAdapter.TranspAdapSetSessionCloseHandler(this::StartupManOnTranspClose);

      long transportStartStartedAt = System.nanoTime();
      transportAdapter.TranspAdapStart();
      runtimeMetrics.RTMetricsRecordTimerMillis(
          METRIC_TRANSPORT_START_DURATION,
          TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - transportStartStartedAt)));

      long processCreatedTimestampSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
      runtimeMetrics.RTMetricsSetGauge(
          McpMetricCatal.MCP_SERVER_PROCESS_CREATED_TIMESTAMP_SECONDS,
          processCreatedTimestampSeconds);

      Long lastStoppedTimestampSeconds = lifecycleStateStore.ServerLifeStateReadLastStoppedTimestampSeconds();
      if (lastStoppedTimestampSeconds != null) {
        runtimeMetrics.RTMetricsSetGauge(
            McpMetricCatal.MCP_SERVER_PROCESS_LAST_STOPPED_TIMESTAMP_SECONDS,
            lastStoppedTimestampSeconds);
      }

      runtimeMetrics.RTMetricsSetGauge(StartupManActiveConnectionsMetric(), 0L);
      runtimeMetrics.RTMetricsIncrementCounter(METRIC_RUNTIME_STARTUPS_TOTAL);

      serverLogger.ServerLogInfoObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "START",
          "TRANSPORT_STARTED",
          "StartupMan: transport started (RTStatus owned by McpRTOrch)");

      return true;
    } finally {
      runtimeMetrics.RTMetricsRecordTimerMillis(
          METRIC_RUNTIME_STARTUP_DURATION,
          TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startupStartedAt)));
    }
  }

  private void StartupManOnTranspOpen(TranspSess session) {

    if (session == null) {
      return;
    }

    String transportConnectionId = session.TranspSessGetTranspConnId();
    if (transportConnectionId == null || transportConnectionId.isBlank()) {
      transportConnectionId = TRANSPORT_UNBOUND;
    }

    // Ensure exactly 1 OPEN handling per physical transport connection.
    if (!openTranspConnectionIds.add(session.TranspSessGetTranspName() + "|" + transportConnectionId)) {

      serverLogger.ServerLogWarnStructured(
          StartupManComponentForSession(session),
          session.TranspSessGetMcpSessId(),
          transportConnectionId,
          MCP_NOT_APPLICABLE,
          "StartupMan: duplicate transport session OPEN ignored");

      return;
    }

    sessionRTOrch.McpSessRTOrchOpen(session);
    runtimeMetrics.RTMetricsSetGauge(
        StartupManActiveConnectionsMetric(),
        openTranspConnectionIds.size());

    ObservCtx context = obsCtxFactory.ObservCtxFactoryFromTranspSess(session);

    serverLogger.ServerLogInfoObserved(
        StartupManComponentForSession(session),
        context,
        "OPEN",
        StartupManOpenEventName(session),
        "StartupMan: transport session OPEN");
  }

  private void StartupManOnTranspClose(TranspSess session) {

    if (session == null) {
      return;
    }

    String transportConnectionId = session.TranspSessGetTranspConnId();
    if (transportConnectionId == null || transportConnectionId.isBlank()) {
      transportConnectionId = TRANSPORT_UNBOUND;
    }

    // Ensure exactly 1 CLOSE handling per physical transport connection.
    if (!openTranspConnectionIds.remove(session.TranspSessGetTranspName() + "|" + transportConnectionId)) {

      serverLogger.ServerLogWarnStructured(
          StartupManComponentForSession(session),
          session.TranspSessGetMcpSessId(),
          transportConnectionId,
          MCP_NOT_APPLICABLE,
          "StartupMan: duplicate transport session CLOSE ignored");

      return;
    }

    wiring.RTWiringClearTranspState(
        transportConnectionId,
        session.TranspSessGetMcpSessId());
    sessionRTOrch.McpSessRTOrchForgetTranspConn(transportConnectionId);
    sessionRTOrch.McpSessRTOrchClose(session.TranspSessGetMcpSessIdObject());
    runtimeMetrics.RTMetricsSetGauge(
        StartupManActiveConnectionsMetric(),
        openTranspConnectionIds.size());

    ObservCtx context = obsCtxFactory.ObservCtxFactoryFromTranspSess(session);

    serverLogger.ServerLogInfoObserved(
        StartupManComponentForSession(session),
        context,
        "CLOSE",
        StartupManCloseEventName(session),
        "StartupMan: transport session CLOSE");
  }

  private String StartupManActiveConnectionsMetric() {
    return switch (transportAdapter.TranspAdapGetTranspName()) {
      case "stdio" -> "stdio.connections.active";
      case "streamable-http" -> "http.sessions.active";
      default -> "ws.connections.active";
    };
  }

  private static ServerLogger.Component StartupManComponentForSession(TranspSess session) {
    if (session != null && TranspSess.TRANSPORT_WEBSOCKET.equals(session.TranspSessGetTranspName())) {
      return ServerLogger.Component.WS;
    }
    return ServerLogger.Component.RUNTIME;
  }

  private static String StartupManOpenEventName(TranspSess session) {
    if (session != null && "stdio".equals(session.TranspSessGetTranspName())) {
      return "STDIO_TRANSPORT_OPENED";
    }
    if (session != null && "streamable-http".equals(session.TranspSessGetTranspName())) {
      return "HTTP_SESSION_OPENED";
    }
    return "WS_CONNECTION_OPENED";
  }

  private static String StartupManCloseEventName(TranspSess session) {
    if (session != null && "stdio".equals(session.TranspSessGetTranspName())) {
      return "STDIO_TRANSPORT_CLOSED";
    }
    if (session != null && "streamable-http".equals(session.TranspSessGetTranspName())) {
      return "HTTP_SESSION_CLOSED";
    }
    return "WS_CONNECTION_CLOSED";
  }
}
