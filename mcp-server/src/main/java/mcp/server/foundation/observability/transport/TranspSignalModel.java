package mcp.server.foundation.observability.transport;

import mcp.server.foundation.observability.metrics.RTMetrics;

import java.util.List;
import java.util.Objects;

/**
 * Shared transport signal naming used by ops surfaces and telemetry views.
 */
public final class TranspSignalModel {

  public static final String SIGNAL_DISCONNECTS_TOTAL = "transport_disconnects_total";
  public static final String SIGNAL_OVERLOAD_REJECTIONS_TOTAL = "transport_overload_rejections_total";
  public static final String SIGNAL_TIMEOUTS_TOTAL = "transport_timeouts_total";
  public static final String SIGNAL_AUTH_DENIALS_TOTAL = "transport_auth_denials_total";
  public static final String SIGNAL_RECONNECTS_TOTAL = "transport_reconnects_total";
  public static final String SIGNAL_OUTBOUND_ERRORS_TOTAL = "transport_outbound_errors_total";

  private TranspSignalModel() {
  }

  public static String TransSigFamily(String transportName) {

    String normalized = TransSigNormalize(transportName);

    if ("streamable-http".equals(normalized)) {
      return "http";
    }

    if ("websocket".equals(normalized)) {
      return "ws";
    }

    return normalized;
  }

  public static String TransSigNormalize(String transportName) {

    Objects.requireNonNull(transportName, "transportName");

    String normalized = transportName.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("transportName must not be blank");
    }

    return normalized;
  }

  public static List<TranspDiagnSignalView> TransSigBuildCanonicalSignals(
      String transportName,
      RTMetrics runtimeMetrics) {

    Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");

    String family = TransSigFamily(transportName);

    return List.of(
        TransSigCounterSignal(
            SIGNAL_DISCONNECTS_TOTAL,
            family,
            runtimeMetrics,
            TransSigDisconnectMetricName(transportName)),
        TransSigCounterSignal(
            SIGNAL_OVERLOAD_REJECTIONS_TOTAL,
            family,
            runtimeMetrics,
            TransSigOverloadRejectionsMetricName(transportName)),
        TransSigCounterSignal(
            SIGNAL_TIMEOUTS_TOTAL,
            family,
            runtimeMetrics,
            TransSigTimeoutsMetricName(transportName)),
        TransSigCounterSignal(
            SIGNAL_AUTH_DENIALS_TOTAL,
            family,
            runtimeMetrics,
            TransSigAuthDeniedMetricName(transportName)),
        TransSigCounterSignal(
            SIGNAL_RECONNECTS_TOTAL,
            family,
            runtimeMetrics,
            TransSigReconnectsMetricName(transportName)),
        TransSigCounterSignal(
            SIGNAL_OUTBOUND_ERRORS_TOTAL,
            family,
            runtimeMetrics,
            TransSigTranspErrorsMetricName(transportName)));
  }

  private static TranspDiagnSignalView TransSigCounterSignal(
      String signalName,
      String transportFamily,
      RTMetrics runtimeMetrics,
      String metricName) {

    long value = runtimeMetrics.RTMetricsGetCounter(metricName);
    return new TranspDiagnSignalView(
        signalName,
        transportFamily,
        value,
        value > 0L,
        "metric=" + metricName);
  }

  public static String TransSigDisconnectMetricName(String transportName) {
    String family = TransSigFamily(transportName);
    if ("http".equals(family)) {
      return "http.sessions.closed.total";
    }
    return family + ".connections.closed.total";
  }

  public static String TransSigOverloadRejectionsMetricName(String transportName) {
    return TransSigFamily(transportName) + ".capacity.rejected.total";
  }

  public static String TransSigTimeoutsMetricName(String transportName) {
    return TransSigFamily(transportName) + ".transport.timeouts.total";
  }

  public static String TransSigAuthDeniedMetricName(String transportName) {
    return TransSigFamily(transportName) + ".auth.denied.total";
  }

  public static String TransSigReconnectsMetricName(String transportName) {
    return TransSigFamily(transportName) + ".reconnects.total";
  }

  public static String TransSigTranspErrorsMetricName(String transportName) {
    return TransSigFamily(transportName) + ".transport.errors.total";
  }

  public static String TransSigOutbMessagesMetricName(String transportName) {
    return TransSigFamily(transportName) + ".messages.out.total";
  }

  public static String TransSigSessClosedMetricName(String transportName) {
    return TransSigDisconnectMetricName(transportName);
  }
}
