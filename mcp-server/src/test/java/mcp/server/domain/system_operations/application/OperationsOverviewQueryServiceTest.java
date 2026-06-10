package mcp.server.domain.system_operations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.customers.api.CustomerQuery;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.system_operations.application.OperationsOverviewQueryService.OperationsOverview;
import mcp.server.foundation.observability.diagnostics.RuntimeSessionDiagnosticsService;
import mcp.server.foundation.observability.runtime.LogStatusView;
import mcp.server.foundation.observability.runtime.RTCalibrView;
import mcp.server.foundation.observability.runtime.RTConcurrView;
import mcp.server.foundation.observability.runtime.RTCoreSignalsView;
import mcp.server.foundation.observability.runtime.RTStatusView;
import mcp.server.foundation.observability.runtime.RTVisibilityService;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class OperationsOverviewQueryServiceTest {

  private final CandidateProfileQuery candidateProfileQuery = mock(CandidateProfileQuery.class);
  private final CustomerQuery customerQuery = mock(CustomerQuery.class);
  private final MissionQueryService missionQueryService = mock(MissionQueryService.class);
  private final RTVisibilityService runtimeVisibilityService = mock(RTVisibilityService.class);
  private final RuntimeSessionDiagnosticsService runtimeSessionDiagnosticsService =
      mock(RuntimeSessionDiagnosticsService.class);
  private final OperationsOverviewQueryService service = new OperationsOverviewQueryService(
      candidateProfileQuery,
      customerQuery,
      missionQueryService,
      runtimeVisibilityService,
      runtimeSessionDiagnosticsService);

  @Test
  void overviewAggregatesCountsAndLastSessionActivityThroughReadContracts() {
    when(runtimeVisibilityService.RTVisibilitySvcGetRTStatus()).thenReturn(runtimeStatus(4));
    when(runtimeSessionDiagnosticsService.lastSessionActivityAt())
        .thenReturn(Optional.of(Instant.parse("2026-06-08T18:30:00Z")));
    when(customerQuery.countRegisteredCustomers()).thenReturn(7L);
    when(candidateProfileQuery.countRegisteredCandidateProfiles()).thenReturn(11L);
    when(missionQueryService.countRegisteredMissions()).thenReturn(13L);

    OperationsOverview overview = service.overview();

    assertThat(overview.activeSessions()).isEqualTo(4);
    assertThat(overview.lastSessionAt()).isEqualTo("2026-06-08T18:30:00Z");
    assertThat(overview.customerCount()).isEqualTo(7L);
    assertThat(overview.candidateCount()).isEqualTo(11L);
    assertThat(overview.totalMissions()).isEqualTo(13L);
  }

  @Test
  void overviewUsesNullLastSessionWhenDiagnosticsHaveNoPersistedActivity() {
    when(runtimeVisibilityService.RTVisibilitySvcGetRTStatus()).thenReturn(runtimeStatus(0));
    when(runtimeSessionDiagnosticsService.lastSessionActivityAt()).thenReturn(Optional.empty());

    OperationsOverview overview = service.overview();

    assertThat(overview.activeSessions()).isZero();
    assertThat(overview.lastSessionAt()).isNull();
  }

  private RTStatusView runtimeStatus(int logicalSessionCount) {
    return new RTStatusView(
        "RUNNING",
        "streamable-http",
        logicalSessionCount,
        0,
        0,
        0,
        0L,
        0L,
        List.of(),
        Map.of(),
        new LogStatusView(false, false, false, false, false, false, false, false, false, false, false, false),
        new RTCoreSignalsView(
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            List.of(),
            new RTConcurrView(0, 0, List.of()),
            new RTCalibrView("OK", false, "none", List.of())));
  }
}
