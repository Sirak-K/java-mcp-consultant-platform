package mcp.server.foundation.observability.persistence;

import io.micrometer.observation.Observation;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.context.ObservCtxHolder;
import mcp.server.foundation.observability.metrics.McpMetricCatal;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.tracing.McpObservationSupport;
import mcp.server.foundation.rpc.error.ErrClassifier;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Central persistence instrumentation for repository adapters.
 */
public final class McpPersistenceTelem {

  public enum PersistenceIntent {
    READ,
    WRITE
  }

  private final McpTelemMetrics telemetryMetrics;
  private final McpObservationSupport observationSupport;
  private final ServerLogger logger;
  private final ErrClassifier errorClassifier;
  private final RTMetrics runtimeMetrics;
  private final ObservCtxFactory obsCtxFactory;

  public McpPersistenceTelem(
      McpTelemMetrics telemetryMetrics,
      McpObservationSupport observationSupport,
      ServerLogger logger,
      ErrClassifier errorClassifier,
      RTMetrics runtimeMetrics,
      ObservCtxFactory obsCtxFactory) {

    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
    this.observationSupport = Objects.requireNonNull(observationSupport, "observationSupport");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.errorClassifier = Objects.requireNonNull(errorClassifier, "errorClassifier");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
  }

  public <T> T McpPersistObserve(
      String repository,
      String operation,
      Supplier<T> work) {

    return McpPersistObserve(PersistenceIntent.WRITE, repository, operation, work);
  }

  public <T> T McpPersistObserveRead(
      String repository,
      String operation,
      Supplier<T> work) {

    return McpPersistObserve(PersistenceIntent.READ, repository, operation, work);
  }

  public <T> T McpPersistObserveWrite(
      String repository,
      String operation,
      Supplier<T> work) {

    return McpPersistObserve(PersistenceIntent.WRITE, repository, operation, work);
  }

  private <T> T McpPersistObserve(
      PersistenceIntent intent,
      String repository,
      String operation,
      Supplier<T> work) {

    Objects.requireNonNull(intent, "intent");
    Objects.requireNonNull(repository, "repository");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(work, "work");

    ObservCtx context = obsCtxFactory.ObservCtxFactoryCurrentOrEmpty();
    Observation observation = observationSupport.McpObsStartPersistenceObservation(
        context,
        repository,
        operation);
    Observation.Scope scope = observation.openScope();
    ObservCtxHolder.Scope holderScope = ObservCtxHolder.ObservCtxHolderOpenScope(context);
    long startedAt = System.nanoTime();

    try {
      T result = work.get();
      long durationMillis = McpPersistDurationMillis(startedAt);
      McpPersistRecordRuntimeMetrics(repository, operation, durationMillis, false);
      telemetryMetrics.McpTelemRecordPersistence(context, repository, operation, "success", null, durationMillis);
      logger.ServerLogInfoObserved(
          ServerLogger.Component.RUNTIME,
          context,
          McpPersistAction(intent),
          "PERSISTENCE_CALL_COMPLETED",
          "Persistence call completed intent="
              + intent.name().toLowerCase()
              + " repository="
              + repository
              + " operation="
              + operation,
          durationMillis);
      return result;
    } catch (RuntimeException ex) {
      long durationMillis = McpPersistDurationMillis(startedAt);
      String errorType = errorClassifier.ErrClassifierClassify(ex).name();
      McpPersistRecordRuntimeMetrics(repository, operation, durationMillis, true);
      telemetryMetrics.McpTelemRecordPersistence(context, repository, operation, "error", errorType, durationMillis);
      observationSupport.McpObsMarkErr(observation, ex, errorType);
      logger.ServerLogErrorObserved(
          ServerLogger.Component.RUNTIME,
          obsCtxFactory.ObservCtxFactoryWithErrType(context, errorType),
          McpPersistAction(intent),
          "PERSISTENCE_CALL_FAILED",
          "Persistence call failed intent="
              + intent.name().toLowerCase()
              + " repository="
              + repository
              + " operation="
              + operation
              + ": "
              + ex.getMessage(),
          durationMillis,
          errorType,
          ex);
      throw ex;
    } finally {
      holderScope.close();
      scope.close();
      observation.stop();
    }
  }

  private static long McpPersistDurationMillis(long startedAt) {
    return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startedAt));
  }

  private void McpPersistRecordRuntimeMetrics(
      String repository,
      String operation,
      long durationMillis,
      boolean failed) {

    runtimeMetrics.RTMetricsIncrementCounter(McpMetricCatal.MCP_RUNTIME_PERSISTENCE_CALLS_TOTAL);
    if (failed) {
      runtimeMetrics.RTMetricsIncrementCounter(McpMetricCatal.MCP_RUNTIME_PERSISTENCE_FAILURES_TOTAL);
    }

    runtimeMetrics.RTMetricsRecordTimerMillis(McpMetricCatal.MCP_RUNTIME_PERSISTENCE_DURATION, durationMillis);
    runtimeMetrics.RTMetricsRecordTimerMillis(
        McpMetricCatal.MCP_RUNTIME_PERSISTENCE_OPERATION_DURATION_PREFIX
            + McpPersistMetricSegment(repository)
            + "."
            + McpPersistMetricSegment(operation)
            + McpMetricCatal.MCP_RUNTIME_PERSISTENCE_OPERATION_DURATION_SUFFIX,
        durationMillis);
  }

  private static String McpPersistMetricSegment(String raw) {
    return raw == null || raw.isBlank()
        ? "unknown"
        : raw.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
  }

  private static String McpPersistAction(PersistenceIntent intent) {
    return intent == PersistenceIntent.READ ? "PERSIST_READ" : "PERSIST_WRITE";
  }
}
