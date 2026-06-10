package mcp.server.domain.candidate_presentation.application.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import mcp.server.domain.candidate_profiles.api.CandidateProfileEvidence;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.candidate_profiles.api.CandidateProfileSkillView;
import mcp.server.domain.matching.api.CandidateToSlotMatchEvidence;
import mcp.server.domain.matching.api.CandidateToSlotMatchQuery;
import mcp.server.domain.matching.api.MatchScoreBreakdownView;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.missions.application.MissionSpecification;
import mcp.server.domain.missions.application.RegisteredMissionQuery;

class CandidatePresentationEvidenceServiceTest {

    @Test
    void collectEvidenceUsesCandidateProfileApiEvidenceInsteadOfCandidateProfileInternals() {
        CandidateToSlotMatchQuery matchQuery = mock(CandidateToSlotMatchQuery.class);
        MissionQueryService missionQueryService = mock(MissionQueryService.class);
        CandidateProfileQuery candidateProfileQuery = mock(CandidateProfileQuery.class);
        CandidatePresentationEvidenceService service = new CandidatePresentationEvidenceService(
                matchQuery,
                missionQueryService,
                candidateProfileQuery);
        Instant matchedAt = Instant.parse("2026-01-01T10:00:00Z");
        CandidateToSlotMatchEvidence match = new CandidateToSlotMatchEvidence(
                99L,
                10L,
                555L,
                75,
                "Qualified",
                true,
                true,
                1,
                List.of(42L),
                List.of("Java"),
                matchedAt);
        MissionSpecification.SpecificationView missionSpecification = specification();
        RegisteredMissionQuery.MissionSlotReadView slot = new RegisteredMissionQuery.MissionSlotReadView(
                555L,
                88L,
                1,
                missionSpecification.missionSlots().get(0));
        RegisteredMissionQuery.MissionReadView mission = new RegisteredMissionQuery.MissionReadView(
                88L,
                "OPEN",
                missionSpecification,
                List.of(slot));
        CandidateProfileEvidence candidateEvidence = candidateEvidence();
        when(matchQuery.findMatchEvidence(99L)).thenReturn(Optional.of(match));
        when(missionQueryService.requireMissionSlot(555L)).thenReturn(slot);
        when(missionQueryService.requireMissionForSlot(555L)).thenReturn(mission);
        when(candidateProfileQuery.findEvidenceProfile(10L)).thenReturn(Optional.of(candidateEvidence));
        when(matchQuery.inspectScoreBreakdown(99L)).thenReturn(scoreBreakdown());

        CandidatePresentationEvidenceView evidence = service.collectEvidence(99L);

        assertThat(evidence.candidateContext().candidateProfileId()).isEqualTo(10L);
        assertThat(evidence.candidateContext().candidateName()).isEqualTo("Ada Lovelace");
        assertThat(evidence.candidateContext().generatedSummaryCoreCompetenceOverview())
                .isEqualTo("Strong backend engineering");
        assertThat(evidence.missionContext().missionSlotId()).isEqualTo(555L);
        assertThat(evidence.skillEvidence().candidatePrimarySkills()).singleElement()
                .satisfies(skill -> {
                    assertThat(skill.skillId()).isEqualTo(42L);
                    assertThat(skill.skillTitle()).isEqualTo("Java");
                    assertThat(skill.skillCategory()).isEqualTo("PRIMARY");
                });
        assertThat(evidence.skillEvidence().matchedPrimarySkills()).containsExactly("Java (Senior)");
        assertThat(evidence.experienceEvidence().workExperiences()).singleElement()
                .satisfies(workExperience -> {
                    assertThat(workExperience.jobTitle()).isEqualTo("Backend Engineer");
                    assertThat(workExperience.workExpCompany()).isEqualTo("Platform Team");
                });
        assertThat(evidence.internalEvidenceTrace().score()).isEqualTo(75);
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

    private static CandidateProfileEvidence candidateEvidence() {
        return new CandidateProfileEvidence(
                10L,
                "Ada Lovelace",
                "Ada",
                "Lovelace",
                "ada@example.test",
                "Backend Developer",
                "SENIOR",
                5,
                "AVAILABLE",
                "Sweden",
                "Stockholm",
                "Remote",
                "REMOTE",
                "Backend-focused consultant",
                "5",
                new CandidateProfileEvidence.GeneratedSummary(
                        "READY",
                        "Strong backend engineering",
                        "Stockholm",
                        "Available for remote assignments",
                        Instant.parse("2026-01-01T09:00:00Z")),
                List.of(new CandidateProfileSkillView("PRIMARY", 42L, "Java", (short) 3, "Senior")),
                List.of(new CandidateProfileSkillView("SECONDARY", 84L, "PostgreSQL", (short) 2, "Intermediate")),
                List.of(new CandidateProfileEvidence.WorkExperience(
                        1,
                        "Backend Engineer",
                        "Platform Team",
                        "556000-0000",
                        "Stockholm",
                        "Sweden",
                        LocalDate.parse("2023-01-01"),
                        null,
                        true)),
                List.of(new CandidateProfileEvidence.Education(
                        1,
                        "KTH",
                        "Computer Science",
                        LocalDate.parse("2018-08-01"),
                        LocalDate.parse("2021-06-01"),
                        false)),
                List.of(new CandidateProfileEvidence.Certification(
                        1,
                        "Java Certification")));
    }

    private static MatchScoreBreakdownView scoreBreakdown() {
        return new MatchScoreBreakdownView(
                99L,
                75,
                "Qualified",
                70,
                true,
                "READY_FOR_REVIEW",
                List.of(),
                List.of("Java"),
                List.of(),
                "2026-01-01 | 10:00:00");
    }
}
