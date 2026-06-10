package mcp.server.domain.system_operations.application;

import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.customers.api.CustomerQuery;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.foundation.observability.diagnostics.RuntimeSessionDiagnosticsService;
import mcp.server.foundation.observability.runtime.RTStatusView;
import mcp.server.foundation.observability.runtime.RTVisibilityService;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public final class OperationsOverviewQueryService {

  private final CandidateProfileQuery candidateProfileQuery;
  private final CustomerQuery customerQuery;
  private final MissionQueryService missionQueryService;
  private final RTVisibilityService runtimeVisibilityService;
  private final RuntimeSessionDiagnosticsService runtimeSessionDiagnosticsService;

  public OperationsOverviewQueryService(
      CandidateProfileQuery candidateProfileQuery,
      CustomerQuery customerQuery,
      MissionQueryService missionQueryService,
      RTVisibilityService runtimeVisibilityService,
      RuntimeSessionDiagnosticsService runtimeSessionDiagnosticsService) {

    this.candidateProfileQuery = Objects.requireNonNull(candidateProfileQuery, "candidateProfileQuery");
    this.customerQuery = Objects.requireNonNull(customerQuery, "customerQuery");
    this.missionQueryService = Objects.requireNonNull(missionQueryService, "missionQueryService");
    this.runtimeVisibilityService = Objects.requireNonNull(runtimeVisibilityService, "runtimeVisibilityService");
    this.runtimeSessionDiagnosticsService = Objects.requireNonNull(
        runtimeSessionDiagnosticsService,
        "runtimeSessionDiagnosticsService");
  }

  public OperationsOverview overview() {
    RTStatusView runtimeStatus = runtimeVisibilityService.RTVisibilitySvcGetRTStatus();
    String lastSessionAt = runtimeSessionDiagnosticsService.lastSessionActivityAt()
        .map(Instant::toString)
        .orElse(null);

    return new OperationsOverview(
        runtimeStatus.logicalSessionCount(),
        lastSessionAt,
        customerQuery.countRegisteredCustomers(),
        candidateProfileQuery.countRegisteredCandidateProfiles(),
        missionQueryService.countRegisteredMissions());
  }

  public record OperationsOverview(
      int activeSessions,
      String lastSessionAt,
      long customerCount,
      long candidateCount,
      long totalMissions) {
  }
}
