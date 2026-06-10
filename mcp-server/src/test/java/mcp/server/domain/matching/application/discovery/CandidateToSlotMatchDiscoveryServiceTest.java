package mcp.server.domain.matching.application.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import mcp.server.domain.candidate_profiles.api.CandidateProfileMatchingView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.candidate_profiles.api.CandidateProfileRoleView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileSkillView;
import mcp.server.domain.matching.api.CandidateMatchDiscoveryResult;
import mcp.server.domain.matching.model.CandidateToMissionSlotMatchScorer;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.missions.application.MissionSpecification;

class CandidateToSlotMatchDiscoveryServiceTest {

    @Test
    void findCandidatesForMissionScoresCandidateProfilesThroughPublicCandidateProfileQuery() {
        CandidateProfileQuery candidateProfileQuery = mock(CandidateProfileQuery.class);
        MissionQueryService missionQueryService = mock(MissionQueryService.class);
        CandidateToSlotMatchReconciliationService reconciliationService =
                mock(CandidateToSlotMatchReconciliationService.class);
        CandidateToSlotMatchDiscoveryService service = new CandidateToSlotMatchDiscoveryService(
                candidateProfileQuery,
                missionQueryService,
                reconciliationService);
        CandidateProfileMatchingView candidate = new CandidateProfileMatchingView(
                10L,
                "Ada Lovelace",
                "Ada",
                "Lovelace",
                "ada@example.test",
                "AVAILABLE",
                "REMOTE",
                List.of(new CandidateProfileRoleView(1, 7L, "Backend Developer", 5, (short) 3)),
                List.of(new CandidateProfileSkillView("PRIMARY", 42L, "Java", (short) 3, "Senior")));
        MissionSpecification.SpecificationView specification = specification();
        when(candidateProfileQuery.matchableCandidateProfiles()).thenReturn(List.of(candidate));

        List<CandidateMatchDiscoveryResult> results = service.findCandidatesForMission(specification, Map.of(1, 555L));

        assertThat(results).singleElement()
                .satisfies(result -> {
                    assertThat(result.candidateProfileId()).isEqualTo(10L);
                    assertThat(result.candidateName()).isEqualTo("Ada Lovelace");
                    assertThat(result.roleMatched()).isTrue();
                    assertThat(result.workModeMatched()).isTrue();
                    assertThat(result.matchedSkillCount()).isEqualTo(1);
                    assertThat(result.matchedSkills()).containsExactly("Java");
                    assertThat(result.score()).isGreaterThanOrEqualTo(
                            CandidateToMissionSlotMatchScorer.QUALIFIED_MATCH_SCORE);
                });
        verify(reconciliationService).reconcilePair(
                eq(10L),
                eq(555L),
                argThat(score -> score.score() >= CandidateToMissionSlotMatchScorer.QUALIFIED_MATCH_SCORE),
                any(Instant.class));
    }

    private static MissionSpecification.SpecificationView specification() {
        return new MissionSpecification.SpecificationView(
                "Customer",
                "customer@example.test",
                "Backend modernization",
                List.of(new MissionSpecification.SlotSpecificationView(
                        1,
                        7L,
                        "Backend Developer",
                        3,
                        List.of(new MissionSpecification.SkillRequirementView(
                                42L,
                                "Java",
                                (short) 3,
                                "Senior",
                                "PRIMARY")))),
                "2026-01-01",
                "2026-06-30",
                "REMOTE",
                new MissionSpecification.PresentationView("", "", "", "", "", ""));
    }
}
