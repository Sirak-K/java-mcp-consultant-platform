package mcp.server.foundation.observability.readiness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import mcp.server.foundation.observability.diagnostics.RTDiagnService;
import mcp.server.foundation.observability.diagnostics.RTDiagnView;
import mcp.server.foundation.observability.health.RTHealthService;
import mcp.server.foundation.observability.health.RTHealthView;
import mcp.server.foundation.observability.runtime.OperChecksView;
import mcp.server.foundation.observability.runtime.RTStatusView;
import mcp.server.foundation.observability.runtime.RTVisibilityService;
import mcp.server.foundation.observability.triage.RTTriageService;
import mcp.server.foundation.observability.triage.RTTriageView;

/**
 * Operational readiness core. This service snapshots the operational gate without duplicating
 * the existing health/runtime/triage/diagnostics views.
 */
public final class RTReadinessService {

  private static final String GATE_TAG = "OPERATIONAL_READINESS";
  private static final String GATE_ID = "operational-verification-and-readiness";

  private final RTHealthService healthService;
  private final RTVisibilityService visibilityService;
  private final RTTriageService triageService;
  private final RTDiagnService diagnService;

  public RTReadinessService(
      RTHealthService healthService,
      RTVisibilityService visibilityService,
      RTTriageService triageService,
      RTDiagnService diagnService) {
    this.healthService = Objects.requireNonNull(healthService, "healthService");
    this.visibilityService = Objects.requireNonNull(visibilityService, "visibilityService");
    this.triageService = Objects.requireNonNull(triageService, "triageService");
    this.diagnService = Objects.requireNonNull(diagnService, "diagnService");
  }

  public RTReadinessGateView RTReadinessSvcBuildOperGate() {
    final RTHealthView healthReadiness = healthService.RTHealthSvcGetReadiness();
    final RTHealthView healthSummary = healthService.RTHealthSvcGetSummary();
    final boolean healthReady = healthService.RTHealthSvcIsReady();

    final RTStatusView rtStatus = visibilityService.RTVisibilitySvcGetRTStatus();
    final OperChecksView operChecks = visibilityService.RTVisibilitySvcGetOperChecks();
    final RTTriageView triageView = triageService.RTTriageSvcGetView();
    final RTDiagnView diagnView = diagnService.RTDiagSvcGetView();

    final List<RTReadinessCriterion> criteria = new ArrayList<>(5);
    criteria.add(
        criterion(
            "health-ready",
            "Health readiness is green",
            true,
            healthReady,
            RTReadinessEvidence.of("health.readiness", healthReadiness),
            RTReadinessEvidence.of("health.summary", healthSummary)));
    criteria.add(
        criterion(
            "runtime-status",
            "Runtime status view is available",
            true,
            rtStatus != null,
            RTReadinessEvidence.of("runtime.status", rtStatus)));
    criteria.add(
        criterion(
            "required-logs-ready",
            "Required operational and audit logs are ready",
            true,
            rtStatus != null && rtStatus.logs().LogStatusViewHasRequiredLogsReady(),
            RTReadinessEvidence.of("runtime.logs", rtStatus == null ? null : rtStatus.logs())));
    criteria.add(
        criterion(
            "oper-checks",
            "Operational checks view is available",
            true,
            operChecks != null,
            RTReadinessEvidence.of("runtime.operChecks", operChecks)));
    criteria.add(
        criterion(
            "triage-view",
            "Triage view is available",
            true,
            triageView != null,
            RTReadinessEvidence.of("triage.view", triageView)));
    criteria.add(
        criterion(
            "diagnostics-view",
            "Diagnostics view is available",
            true,
            diagnView != null,
            RTReadinessEvidence.of("diagnostics.view", diagnView)));

    final boolean overallPass = criteria.stream().allMatch(RTReadinessCriterion::passed);
    final RTReadinessStatus overallStatus = overallPass ? RTReadinessStatus.PASS : RTReadinessStatus.FAIL;
    return new RTReadinessGateView(
        GATE_ID,
        GATE_TAG,
        overallStatus,
        overallPass,
        criteria,
        0,
        0,
        0,
        0,
        Instant.now());
  }

  private static RTReadinessCriterion criterion(
      String key,
      String label,
      boolean required,
      boolean pass,
      RTReadinessEvidence... evidence) {
    final RTReadinessStatus status = pass ? RTReadinessStatus.PASS : RTReadinessStatus.FAIL;
    return new RTReadinessCriterion(key, label, required, status, List.of(evidence));
  }
}
