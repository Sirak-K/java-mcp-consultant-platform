package mcp.server.foundation.observability.triage;

import mcp.server.foundation.observability.health.HealthCheckView;
import mcp.server.foundation.observability.health.RTHealthView;
import mcp.server.foundation.observability.runtime.OperCheckView;
import mcp.server.foundation.observability.runtime.OperChecksView;
import mcp.server.foundation.observability.runtime.RTCalibrView;
import mcp.server.foundation.observability.runtime.RTMetricsView;
import mcp.server.foundation.observability.runtime.RTStatusView;
import mcp.server.foundation.observability.transport.TranspDiagnSignalView;
import mcp.server.foundation.observability.transport.TranspSignalModel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

final class RTTriageSymptomAssembler {

  private final RuntimeTriageSymptomCatalogService catalogService;

  RTTriageSymptomAssembler(RuntimeTriageSymptomCatalogService catalogService) {
    this.catalogService = Objects.requireNonNull(catalogService, "catalogService");
  }

  RTTriageView assemble(
      RTHealthView readiness,
      RTStatusView runtimeStatus,
      RTMetricsView runtimeMetrics,
      OperChecksView operationalChecks) {

    return new RTTriageView(List.of(
        triageReadinessDown(readiness),
        triageDatabaseUnreachable(readiness, operationalChecks),
        triageTranspErrors(runtimeStatus, runtimeMetrics),
        triageAuthDenials(runtimeStatus, runtimeMetrics),
        triageTailLatencyCalibr(runtimeStatus),
        triageToolTimeouts(runtimeStatus),
        triageToolConcurrRejections(runtimeStatus),
        triageQueuePressure(runtimeStatus),
        triagePersistenceFailures(runtimeStatus),
        triageLoggingGap(runtimeStatus, operationalChecks)));
  }

  private TriageSymptomView triageReadinessDown(RTHealthView readiness) {

    HealthCheckView runtimeCheck = readiness.checks().get("runtime");
    HealthCheckView databaseCheck = readiness.checks().get("database");

    String symptomKey = "readiness_down";
    return symptom(
        symptomKey,
        !readiness.ready(),
        List.of(
            signal(symptomKey, "ready_status_down", !readiness.ready(), detail("status", readiness.status())),
            signal(symptomKey, "runtime_check_down", "DOWN".equals(runtimeCheck.status()), detail("detail", runtimeCheck.detail())),
            signal(symptomKey, "database_check_down", "DOWN".equals(databaseCheck.status()), detail("detail", databaseCheck.detail()))));
  }

  private TriageSymptomView triageDatabaseUnreachable(
      RTHealthView readiness,
      OperChecksView operationalChecks) {

    HealthCheckView databaseCheck = readiness.checks().get("database");
    OperCheckView preStartDatabaseCheck = findCheck(operationalChecks.preStart(), "database_reachable");

    String symptomKey = "database_unreachable";
    return symptom(
        symptomKey,
        "DOWN".equals(databaseCheck.status()),
        List.of(
            signal(symptomKey, "database_down", "DOWN".equals(databaseCheck.status()), detail("detail", databaseCheck.detail())),
            signal(
                symptomKey,
                "database_reachable_failed",
                preStartDatabaseCheck != null && !preStartDatabaseCheck.passed(),
                detail("detail", preStartDatabaseCheck == null ? "database_reachable saknas" : preStartDatabaseCheck.detail()))));
  }

  private TriageSymptomView triageTranspErrors(
      RTStatusView runtimeStatus,
      RTMetricsView runtimeMetrics) {

    long transportErrors = runtimeStatus.transportErrorCount();
    long transportErrorCounter = counter(
        runtimeMetrics,
        TranspSignalModel.TransSigTranspErrorsMetricName(runtimeStatus.transport()));

    String symptomKey = "transport_errors_observed";
    return symptom(
        symptomKey,
        transportErrors > 0L || transportErrorCounter > 0L,
        List.of(
            signal(symptomKey, "transport_error_count", transportErrors > 0L, detail("count", transportErrors)),
            signal(symptomKey, "transport_errors_total", transportErrorCounter > 0L, detail("count", transportErrorCounter)),
            canonicalTranspSignal(runtimeStatus, TranspSignalModel.SIGNAL_DISCONNECTS_TOTAL),
            canonicalTranspSignal(runtimeStatus, TranspSignalModel.SIGNAL_RECONNECTS_TOTAL),
            canonicalTranspSignal(runtimeStatus, TranspSignalModel.SIGNAL_OVERLOAD_REJECTIONS_TOTAL),
            canonicalTranspSignal(runtimeStatus, TranspSignalModel.SIGNAL_TIMEOUTS_TOTAL),
            canonicalTranspSignal(runtimeStatus, TranspSignalModel.SIGNAL_OUTBOUND_ERRORS_TOTAL),
            transportLoggingSignal(symptomKey, runtimeStatus)));
  }

  private TriageSymptomView triageAuthDenials(
      RTStatusView runtimeStatus,
      RTMetricsView runtimeMetrics) {

    long authDeniedCounter = counter(
        runtimeMetrics,
        TranspSignalModel.TransSigAuthDeniedMetricName(runtimeStatus.transport()));

    String symptomKey = "auth_denials_observed";
    return symptom(
        symptomKey,
        authDeniedCounter > 0L,
        List.of(
            signal(symptomKey, "auth_denied_total", authDeniedCounter > 0L, detail("count", authDeniedCounter)),
            canonicalTranspSignal(runtimeStatus, TranspSignalModel.SIGNAL_AUTH_DENIALS_TOTAL),
            authLoggingSignal(symptomKey, runtimeStatus)));
  }

  private TriageSymptomView triageLoggingGap(
      RTStatusView runtimeStatus,
      OperChecksView operationalChecks) {

    OperCheckView logCheck = findCheck(operationalChecks.postStart(), "canonical_logs_present");
    boolean loggingGap = !runtimeStatus.logs().LogStatusViewHasRequiredLogsReady();

    String symptomKey = "canonical_logging_missing";
    return symptom(
        symptomKey,
        loggingGap,
        List.of(
            signal(
                symptomKey,
                "server_log_missing",
                runtimeStatus.logs().serverLogRequired() && !runtimeStatus.logs().serverLogExists(),
                detail("required", runtimeStatus.logs().serverLogRequired(), "exists", runtimeStatus.logs().serverLogExists())),
            signal(
                symptomKey,
                "error_log_missing",
                runtimeStatus.logs().errorLogRequired() && !runtimeStatus.logs().errorLogExists(),
                detail("required", runtimeStatus.logs().errorLogRequired(), "exists", runtimeStatus.logs().errorLogExists())),
            signal(
                symptomKey,
                "audit_log_missing",
                runtimeStatus.logs().auditLogRequired() && !runtimeStatus.logs().auditLogExists(),
                detail("required", runtimeStatus.logs().auditLogRequired(), "exists", runtimeStatus.logs().auditLogExists())),
            signal(
                symptomKey,
                "test_log_missing",
                runtimeStatus.logs().testLogRequired() && !runtimeStatus.logs().testLogExists(),
                detail("required", runtimeStatus.logs().testLogRequired(), "exists", runtimeStatus.logs().testLogExists())),
            signal(
                symptomKey,
                "canonical_logs_present_failed",
                logCheck != null && !logCheck.passed(),
                detail("detail", logCheck == null ? "canonical_logs_present saknas" : logCheck.detail()))));
  }

  private TriageSymptomView triageToolTimeouts(RTStatusView runtimeStatus) {

    long timeoutCount = runtimeStatus.coreSignals().toolTimeoutsTotal();
    boolean active = timeoutCount > 0L;

    String symptomKey = "tool_timeouts_observed";
    return symptom(
        symptomKey,
        active,
        List.of(
            signal(symptomKey, "tool_timeouts_total", active, detail("count", timeoutCount)),
            signal(
                symptomKey,
                "most_failing_tools_present",
                !runtimeStatus.coreSignals().mostFailingTools().isEmpty(),
                detail("count", runtimeStatus.coreSignals().mostFailingTools().size()))));
  }

  private TriageSymptomView triageTailLatencyCalibr(RTStatusView runtimeStatus) {

    RTCalibrView calibration = runtimeStatus.coreSignals().calibration();
    boolean active = calibration.actionRequired();
    String topRecommendation = calibration.recommendations().isEmpty()
        ? "no_recommendations"
        : calibration.recommendations().get(0).id() + " (" + calibration.recommendations().get(0).priority() + ")";

    String symptomKey = "tail_latency_calibration_required";
    return symptom(
        symptomKey,
        active,
        List.of(
            signal(symptomKey, "status", active, detail("status", calibration.status())),
            signal(
                symptomKey,
                "recommendations_present",
                !calibration.recommendations().isEmpty(),
                detail("count", calibration.recommendations().size())),
            signal(
                symptomKey,
                "tool_queue_wait_p95_millis",
                runtimeStatus.coreSignals().queueWaitP95Millis() > 0L,
                detail("p95Millis", runtimeStatus.coreSignals().queueWaitP95Millis())),
            signal(
                symptomKey,
                "tool_queue_wait_p99_millis",
                runtimeStatus.coreSignals().queueWaitP99Millis() > 0L,
                detail("p99Millis", runtimeStatus.coreSignals().queueWaitP99Millis())),
            signal(symptomKey, "top_recommendation", !calibration.recommendations().isEmpty(),
                detail("topRecommendation", topRecommendation))));
  }

  private TriageSymptomView triageToolConcurrRejections(RTStatusView runtimeStatus) {

    long rejectionCount = runtimeStatus.coreSignals().toolRejectionsTotal();
    boolean active = rejectionCount > 0L;

    String symptomKey = "tool_concurrency_rejections_observed";
    return symptom(
        symptomKey,
        active,
        List.of(
            signal(symptomKey, "tool_rejections_total", active, detail("count", rejectionCount)),
            signal(
                symptomKey,
                "global_concurrency_available_permits_low",
                runtimeStatus.coreSignals().concurrency().globalAvailablePermits() == 0,
                detail(
                    "available",
                    runtimeStatus.coreSignals().concurrency().globalAvailablePermits(),
                    "max",
                    runtimeStatus.coreSignals().concurrency().globalMaxConcurrency()))));
  }

  private TriageSymptomView triageQueuePressure(RTStatusView runtimeStatus) {

    long queuePressureEventCount = runtimeStatus.coreSignals().queuePressureEventCount();
    long queueWaitMaxMillis = runtimeStatus.coreSignals().queueWaitMaxMillis();
    long queueWaitP95Millis = runtimeStatus.coreSignals().queueWaitP95Millis();
    long queueWaitP99Millis = runtimeStatus.coreSignals().queueWaitP99Millis();
    boolean active = queuePressureEventCount > 0L
        || queueWaitMaxMillis > 0L
        || queueWaitP95Millis > 0L
        || queueWaitP99Millis > 0L;

    String symptomKey = "tool_queue_pressure_observed";
    return symptom(
        symptomKey,
        active,
        List.of(
            signal(symptomKey, "tool_queue_pressure_events", queuePressureEventCount > 0L,
                detail("count", queuePressureEventCount)),
            signal(symptomKey, "tool_queue_wait_max_millis", queueWaitMaxMillis > 0L,
                detail("maxMillis", queueWaitMaxMillis)),
            signal(symptomKey, "tool_queue_wait_p95_millis", queueWaitP95Millis > 0L,
                detail("p95Millis", queueWaitP95Millis)),
            signal(symptomKey, "tool_queue_wait_p99_millis", queueWaitP99Millis > 0L,
                detail("p99Millis", queueWaitP99Millis))));
  }

  private TriageSymptomView triagePersistenceFailures(RTStatusView runtimeStatus) {

    long persistenceFailures = runtimeStatus.coreSignals().persistenceFailuresTotal();
    boolean active = persistenceFailures > 0L;

    String symptomKey = "persistence_failures_observed";
    return symptom(
        symptomKey,
        active,
        List.of(
            signal(symptomKey, "persistence_failures_total", active, detail("count", persistenceFailures)),
            signal(
                symptomKey,
                "persistence_calls_total",
                runtimeStatus.coreSignals().persistenceCallsTotal() > 0L,
                detail("count", runtimeStatus.coreSignals().persistenceCallsTotal()))));
  }

  private TriageSignalView transportLoggingSignal(String symptomKey, RTStatusView runtimeStatus) {
    if (runtimeStatus.logs().testSinkEnabled()) {
      return signal(
          symptomKey,
          "test_log_available",
          runtimeStatus.logs().testLogExists(),
          detail(
              "enabled",
              runtimeStatus.logs().testSinkEnabled(),
              "required",
              runtimeStatus.logs().testLogRequired(),
              "exists",
              runtimeStatus.logs().testLogExists()));
    }

    return signal(
        symptomKey,
        "error_log_available",
        runtimeStatus.logs().errorLogExists(),
        detail(
            "enabled",
            runtimeStatus.logs().fileSinkEnabled(),
            "required",
            runtimeStatus.logs().errorLogRequired(),
            "exists",
            runtimeStatus.logs().errorLogExists()));
  }

  private TriageSignalView authLoggingSignal(String symptomKey, RTStatusView runtimeStatus) {
    if (runtimeStatus.logs().testSinkEnabled()) {
      return signal(
          symptomKey,
          "test_log_available",
          runtimeStatus.logs().testLogExists(),
          detail(
              "enabled",
              runtimeStatus.logs().testSinkEnabled(),
              "required",
              runtimeStatus.logs().testLogRequired(),
              "exists",
              runtimeStatus.logs().testLogExists()));
    }

    return signal(
        symptomKey,
        "audit_log_available",
        runtimeStatus.logs().auditLogExists(),
        detail(
            "enabled",
            runtimeStatus.logs().auditSinkEnabled(),
            "required",
            runtimeStatus.logs().auditLogRequired(),
            "exists",
            runtimeStatus.logs().auditLogExists()));
  }

  private TriageSymptomView symptom(
      String symptomKey,
      boolean active,
      List<TriageSignalView> signals) {

    return catalogService.symptom(symptomKey, active, signals);
  }

  private TriageSignalView signal(
      String symptomKey,
      String signalKey,
      boolean observed,
      Map<String, String> detailValues) {

    return catalogService.signal(symptomKey, signalKey, observed, detailValues);
  }

  private static Map<String, String> detail(Object... keyValues) {
    if (keyValues.length % 2 != 0) {
      throw new IllegalArgumentException("detail key-values must be even.");
    }
    java.util.LinkedHashMap<String, String> details = new java.util.LinkedHashMap<>();
    for (int index = 0; index < keyValues.length; index += 2) {
      details.put(
          Objects.toString(keyValues[index], ""),
          Objects.toString(keyValues[index + 1], ""));
    }
    return Map.copyOf(details);
  }

  private static OperCheckView findCheck(
      List<OperCheckView> checks,
      String name) {

    return checks.stream()
        .filter(check -> name.equals(check.name()))
        .findFirst()
        .orElse(null);
  }

  private static long counter(RTMetricsView runtimeMetrics, String metricName) {
    return runtimeMetrics.counters().getOrDefault(metricName, 0L);
  }

  private static TriageSignalView canonicalTranspSignal(
      RTStatusView runtimeStatus,
      String signalName) {

    TranspDiagnSignalView signal = runtimeStatus.transportSignals().stream()
        .filter(candidate -> signalName.equals(candidate.name()))
        .findFirst()
        .orElse(null);

    if (signal == null) {
      return new TriageSignalView("transport", signalName, false, "missing");
    }

    return new TriageSignalView(
        "transport",
        signal.name(),
        signal.observed(),
        "value=" + signal.value() + ", " + signal.detail());
  }
}
