package mcp.server.domain.match_notifications.application.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_profiles.api.CandidateProfileCardView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.candidate_profiles.api.CandidateProfileRoleView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileSkillView;
import mcp.server.domain.match_notifications.application.delivery.MatchNotificationDeliveryService;
import mcp.server.domain.match_notifications.web.MatchNotificationWebContract;
import mcp.server.domain.matching.api.CandidateMissionMatchEvidenceGroup;
import mcp.server.domain.matching.api.CandidateToSlotMatchEvidence;
import mcp.server.domain.matching.api.CandidateToSlotMatchQuery;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.missions.application.MissionSpecification;
import mcp.server.domain.missions.application.RegisteredMissionQuery;
import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

class MatchNotificationPreviewServiceTest {

  private final CandidateToSlotMatchQuery matchQuery = mock(CandidateToSlotMatchQuery.class);
  private final CandidateProfileQuery candidateProfileQuery = mock(CandidateProfileQuery.class);
  private final MissionQueryService missionQueryService = mock(MissionQueryService.class);
  private final MatchNotificationDeliveryService deliveryService = mock(MatchNotificationDeliveryService.class);
  private final MatchNotificationEmailTemplateCatalogService templateCatalog =
      new MatchNotificationEmailTemplateCatalogService(new ProjectCatalogJsonLoader(new ObjectMapper()));
  private final MatchNotificationPreviewService previewService = new MatchNotificationPreviewService(
      matchQuery,
      candidateProfileQuery,
      missionQueryService,
      deliveryService,
      new MatchNotificationEmailContentBuilder(templateCatalog),
      templateCatalog);

  @Test
  void previewMatchBuildsCatalogBackedGroupedPreviewFromPublicDomainQueries() {
    CandidateToSlotMatchEvidence laterSlotMatch = match(1002L, 902L, 88, List.of(42L), List.of("Java"));
    CandidateToSlotMatchEvidence earlierSlotMatch = match(1001L, 901L, 93, List.of(42L, 84L), List.of("Java", "SQL"));
    CandidateMissionMatchEvidenceGroup group = new CandidateMissionMatchEvidenceGroup(
        laterSlotMatch,
        301L,
        "candidate-501-mission-301",
        Instant.parse("2026-01-01T10:00:00Z"),
        List.of(laterSlotMatch, earlierSlotMatch));
    RegisteredMissionQuery.MissionReadView mission = mission();
    when(matchQuery.findCandidateMissionMatchEvidenceGroup(1002L)).thenReturn(Optional.of(group));
    when(candidateProfileQuery.candidateProfileCardsById(List.of(501L))).thenReturn(Map.of(501L, candidate()));
    when(missionQueryService.requireMissionForSlot(902L)).thenReturn(mission);
    when(missionQueryService.requireMissionSlot(902L)).thenReturn(mission.slots().get(1));
    when(missionQueryService.requireMissionSlot(901L)).thenReturn(mission.slots().get(0));

    MatchNotificationWebContract.MatchNotificationPreviewView preview = previewService.previewMatch(1002L);

    assertThat(preview.matchId()).isEqualTo(1001L);
    assertThat(preview.matchIds()).containsExactly(1001L, 1002L);
    assertThat(preview.candidateProfileId()).isEqualTo(501L);
    assertThat(preview.missionId()).isEqualTo(301L);
    assertThat(preview.groupedMatchCount()).isEqualTo(2);
    assertThat(preview.subject()).isEqualTo(templateCatalog.subject());
    assertThat(preview.evidenceBrief()).contains("Ada Lovelace", "Backend Engineer", "Platform Upgrade");
    assertThat(preview.textBody()).contains(
        "Match Grade:",
        "Mission Slot 1: Backend Engineer",
        "Mission Slot 2: Data Engineer",
        "Java (Senior)",
        "SQL (Intermediate)");
    assertThat(preview.htmlBody()).contains("Customer Name:</span> Acme Ops");
  }

  private static CandidateToSlotMatchEvidence match(
      long matchId,
      long missionSlotId,
      int score,
      List<Long> matchedSkillIds,
      List<String> matchedSkillTitles) {
    return new CandidateToSlotMatchEvidence(
        matchId,
        501L,
        missionSlotId,
        score,
        "Qualified Match",
        true,
        true,
        matchedSkillTitles.size(),
        matchedSkillIds,
        matchedSkillTitles,
        Instant.parse("2026-01-01T10:00:00Z"));
  }

  private static CandidateProfileCardView candidate() {
    return new CandidateProfileCardView(
        501L,
        "Ada Lovelace",
        "Backend Engineer",
        "Senior",
        7,
        "AVAILABLE",
        "Sweden",
        "REMOTE",
        "REMOTE",
        List.of(new CandidateProfileRoleView(1, 77L, "Backend Engineer", 7, (short) 3)),
        List.of(
            new CandidateProfileSkillView("PRIMARY", 42L, "Java", (short) 3, "Senior"),
            new CandidateProfileSkillView("PRIMARY", 84L, "SQL", (short) 2, "Intermediate")),
        List.of());
  }

  private static RegisteredMissionQuery.MissionReadView mission() {
    MissionSpecification.PresentationView presentation = new MissionSpecification.PresentationView(
        "",
        "",
        "",
        "",
        "",
        "");
    MissionSpecification.SlotSpecificationView slotOne = new MissionSpecification.SlotSpecificationView(
        1,
        77L,
        "Backend Engineer",
        5,
        List.of());
    MissionSpecification.SlotSpecificationView slotTwo = new MissionSpecification.SlotSpecificationView(
        2,
        88L,
        "Data Engineer",
        4,
        List.of());
    MissionSpecification.SpecificationView specification = new MissionSpecification.SpecificationView(
        "Acme Ops",
        "ops@example.test",
        "Platform Upgrade",
        List.of(slotOne, slotTwo),
        "2026-01-01",
        "2026-06-30",
        "REMOTE",
        presentation);
    return new RegisteredMissionQuery.MissionReadView(
        301L,
        "OPEN",
        specification,
        List.of(
            new RegisteredMissionQuery.MissionSlotReadView(901L, 301L, 1, slotOne),
            new RegisteredMissionQuery.MissionSlotReadView(902L, 301L, 2, slotTwo)));
  }
}
