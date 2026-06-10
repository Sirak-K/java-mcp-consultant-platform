package mcp.server.foundation.observability.triage;

import mcp.server.foundation.observability.health.RTHealthService;
import mcp.server.foundation.observability.health.RTHealthView;
import mcp.server.foundation.observability.runtime.OperChecksView;
import mcp.server.foundation.observability.runtime.RTMetricsView;
import mcp.server.foundation.observability.runtime.RTStatusView;
import mcp.server.foundation.observability.runtime.RTVisibilityService;

import java.util.Objects;

/**
 * Maps common runtime symptoms to health, log and metrics signals.
 */
public final class RTTriageService {

  private final RTHealthService runtimeHealthService;
  private final RTVisibilityService runtimeVisibilityService;
  private final RTTriageSymptomAssembler symptomAssembler;

  public RTTriageService(
      RTHealthService runtimeHealthService,
      RTVisibilityService runtimeVisibilityService,
      RuntimeTriageSymptomCatalogService symptomCatalogService) {

    this(runtimeHealthService, runtimeVisibilityService, new RTTriageSymptomAssembler(symptomCatalogService));
  }

  RTTriageService(
      RTHealthService runtimeHealthService,
      RTVisibilityService runtimeVisibilityService,
      RTTriageSymptomAssembler symptomAssembler) {

    this.runtimeHealthService = Objects.requireNonNull(runtimeHealthService, "runtimeHealthService");
    this.runtimeVisibilityService = Objects.requireNonNull(runtimeVisibilityService, "runtimeVisibilityService");
    this.symptomAssembler = Objects.requireNonNull(symptomAssembler, "symptomAssembler");
  }

  public RTTriageView RTTriageSvcGetView() {

    RTHealthView readiness = runtimeHealthService.RTHealthSvcGetReadiness();
    RTStatusView runtimeStatus = runtimeVisibilityService.RTVisibilitySvcGetRTStatus();
    RTMetricsView runtimeMetrics = runtimeVisibilityService.RTVisibilitySvcGetMetrics();
    OperChecksView operationalChecks = runtimeVisibilityService.RTVisibilitySvcGetOperChecks();

    return symptomAssembler.assemble(readiness, runtimeStatus, runtimeMetrics, operationalChecks);
  }
}
