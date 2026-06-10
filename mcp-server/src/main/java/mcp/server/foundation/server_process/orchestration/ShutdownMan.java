package mcp.server.foundation.server_process.orchestration;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.metrics.McpMetricCatal;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.server_process.client_context.session.McpSessRTOrch;
import mcp.server.foundation.transport.TranspAdap;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * ShutdownMan
 *
 * Ren POJO (ingen Spring-koppling).
 *
 * Ansvar:
 * - Kontrollerad shutdown av runtime (transport + session cleanup)
 */
public final class ShutdownMan {

  private static final String METRIC_RUNTIME_SHUTDOWN_DURATION = "runtime.shutdown.duration";
  private static final String METRIC_TRANSPORT_STOP_DURATION = "runtime.transport.stop.duration";
  private static final String METRIC_SESSION_CLOSE_ALL_DURATION = "runtime.sessions.close_all.duration";
  private static final String METRIC_RUNTIME_SHUTDOWNS_TOTAL = "runtime.shutdowns.total";

  private final TranspAdap transportAdapter;
  private final McpSessRTOrch sessOrch;
  private final ServerLogger logger;
  private final RTMetrics runtimeMetrics;
  private final ServerLifecyStateStore lifecycleStateStore;

  public ShutdownMan(
      TranspAdap transportAdapter,
      McpSessRTOrch sessOrch,
      ServerLogger logger,
      RTMetrics runtimeMetrics) {

    this(
        transportAdapter,
        sessOrch,
        logger,
        runtimeMetrics,
        ServerLifecyStateStore.ServerLifeStateNoOp());
  }

  public ShutdownMan(
      TranspAdap transportAdapter,
      McpSessRTOrch sessOrch,
      ServerLogger logger,
      RTMetrics runtimeMetrics,
      ServerLifecyStateStore lifecycleStateStore) {

    this.transportAdapter = Objects.requireNonNull(transportAdapter, "transportAdapter");
    this.sessOrch = Objects.requireNonNull(sessOrch, "sessOrch");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
  }

  public void ShutdownManStop() {
    long shutdownStartedAt = System.nanoTime();

    logger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        null,
        "STOP",
        "TRANSPORT_STOPPING",
        "ShutdownMan: stopping transport + closing all sessions");

    try {
      long transportStopStartedAt = System.nanoTime();
      transportAdapter.TranspAdapStop();
      runtimeMetrics.RTMetricsRecordTimerMillis(
          METRIC_TRANSPORT_STOP_DURATION,
          TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - transportStopStartedAt)));
    } finally {
      long closeSessionsStartedAt = System.nanoTime();
      sessOrch.McpSessRTOrchCloseAll();
      runtimeMetrics.RTMetricsRecordTimerMillis(
          METRIC_SESSION_CLOSE_ALL_DURATION,
          TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - closeSessionsStartedAt)));
    }

    runtimeMetrics.RTMetricsIncrementCounter(METRIC_RUNTIME_SHUTDOWNS_TOTAL);
    long lastStoppedTimestampSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    runtimeMetrics.RTMetricsSetGauge(
        McpMetricCatal.MCP_SERVER_PROCESS_LAST_STOPPED_TIMESTAMP_SECONDS,
        lastStoppedTimestampSeconds);
    lifecycleStateStore.ServerLifeStateWriteLastStoppedTimestampSeconds(lastStoppedTimestampSeconds);
    runtimeMetrics.RTMetricsRecordTimerMillis(
        METRIC_RUNTIME_SHUTDOWN_DURATION,
        TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - shutdownStartedAt)));

    logger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        null,
        "STOP",
        "TRANSPORT_STOPPED",
        "ShutdownMan: stop complete");
  }
}
