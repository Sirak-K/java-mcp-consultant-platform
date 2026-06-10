package mcp.server.foundation.transport;

import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspSignalModel;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class TranspOutbTelemSupport {

  private static final String OUTBOUND = "outbound";
  private static final String TRANSPORT_ERROR = "TRANSPORT_ERROR";

  private TranspOutbTelemSupport() {
  }

  public static void TranspOutbTelemRecordSent(
      RTMetrics runtimeMetrics,
      String transportName) {

    Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    runtimeMetrics.RTMetricsIncrementCounter(
        TranspSignalModel.TransSigOutbMessagesMetricName(transportName));
  }

  public static void TranspOutbTelemRecordSent(
      RTMetrics runtimeMetrics,
      String transportName,
      String durationMetricName,
      long sendStartedAt) {

    TranspOutbTelemRecordSent(runtimeMetrics, transportName);
    TranspOutbTelemRecordDuration(runtimeMetrics, durationMetricName, sendStartedAt);
  }

  public static void TranspOutbTelemRecordSent(
      RTMetrics runtimeMetrics,
      String transportName,
      String durationMetricName,
      long sendStartedAt,
      TranspSess session,
      String message) {

    TranspOutbTelemRecordSent(runtimeMetrics, transportName, durationMetricName, sendStartedAt);
    Objects.requireNonNull(session, "session")
        .TranspSessRecordResponded(TranspContractSupport.TransContTryExtractCorrelaId(message));
  }

  public static void TranspOutbTelemRecordDuration(
      RTMetrics runtimeMetrics,
      String durationMetricName,
      long sendStartedAt) {

    Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    runtimeMetrics.RTMetricsRecordTimerMillis(
        durationMetricName,
        TranspOutbTelemDurationMillis(sendStartedAt));
  }

  public static void TranspOutbTelemRecordErr(
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      ObservCtx context,
      String transportName) {

    Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
    runtimeMetrics.RTMetricsIncrementCounter(
        TranspSignalModel.TransSigTranspErrorsMetricName(transportName));
    telemetryMetrics.McpTelemIncrementTranspError(
        context,
        OUTBOUND,
        TRANSPORT_ERROR);
  }

  private static long TranspOutbTelemDurationMillis(long sendStartedAt) {
    return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - sendStartedAt));
  }
}
