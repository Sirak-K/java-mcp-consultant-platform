package mcp.server.domain.missions.application.intake;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupEntity;
import mcp.server.domain.reference_data.persistence.RoleEntity;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.RoleJpaRepo;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class MissionProposalWorkingCopyTextEvidenceDetectorTest {

  @Test
  void detectsMissionProposalWorkingCopySignalsFromCatalogAndLookupData() {
    MissionProposalWorkingCopyTextEvidenceDetector detector = detectorWithLookupData();

    MissionProposalWorkingCopyTextEvidenceDetector.WorkingCopyDetectionResult result = detector.detect(String.join(
        "\n",
        "F\u00F6retag: Acme AB",
        "Kontakt: anna@example.com",
        "Uppdrag: Backend utveckling i Java och Spring Boot",
        "Vi beh\u00F6ver tv\u00E5 konsulter remote fr\u00E5n 1 januari 2026 i 6 m\u00E5nader.",
        "Senior niv\u00E5."));

    assertThat(result.customerName()).isEqualTo("Acme AB");
    assertThat(result.customerEmail()).isEqualTo("anna@example.com");
    assertThat(result.missionTitle()).isEqualTo("Backend utveckling i Java och Spring Boot");
    assertThat(result.roleDetected()).isTrue();
    assertThat(result.roleId()).isEqualTo(11L);
    assertThat(result.slotCountDetected()).isTrue();
    assertThat(result.slotCount()).isEqualTo(2);
    assertThat(result.workMode()).isEqualTo("REMOTE");
    assertThat(result.startDate()).isEqualTo("2026-01-01");
    assertThat(result.endDate()).isEqualTo("2026-06-30");
    assertThat(result.roleExperienceYearsDetected()).isTrue();
    assertThat(result.roleExperienceYears()).isEqualTo(5);
    assertThat(result.skillLevelDetected()).isTrue();
    assertThat(result.skillLevelId()).isEqualTo((short) 3);
    assertThat(result.requiredSkills())
        .extracting(MissionProposalWorkingCopyBuilder.RequiredSkill::skillId)
        .containsExactlyInAnyOrder(101L, 102L);
    assertThat(result.evidence())
        .extracting(MissionProposalIntake.WorkingCopyEvidenceView::field)
        .contains(
            "customerName",
            "customerEmail",
            "missionTitle",
            "missionSlots",
            "missionSlots[0].roleId",
            "missionSlots.requiredSkills",
            "missionSlots[0].requiredSkills.skillLevelId");
  }

  private MissionProposalWorkingCopyTextEvidenceDetector detectorWithLookupData() {
    RoleJpaRepo roleRepo = mock(RoleJpaRepo.class);
    SkillCatalogLookup skillLookup = mock(SkillCatalogLookup.class);
    CompetencyLevelLookupJpaRepo skillLevelRepo = mock(CompetencyLevelLookupJpaRepo.class);

    when(roleRepo.findAll()).thenReturn(List.of(new RoleEntity(11L, "Backend Developer")));
    when(skillLookup.findAllPrimarySkills()).thenReturn(List.of(
        new SkillCatalogLookup.SkillRef(101L, "Java", SkillCatalogLookup.CATEGORY_PRIMARY),
        new SkillCatalogLookup.SkillRef(102L, "Spring Boot", SkillCatalogLookup.CATEGORY_PRIMARY)));
    when(skillLookup.findAllSecondarySkills()).thenReturn(List.of());
    when(skillLevelRepo.findAll()).thenReturn(List.of(
        new CompetencyLevelLookupEntity((short) 1, "JUNIOR", (short) 0),
        new CompetencyLevelLookupEntity((short) 2, "MEDIOR", (short) 3),
        new CompetencyLevelLookupEntity((short) 3, "SENIOR", (short) 5)));

    MissionProposalTextDetectionCatalogService catalogService = new MissionProposalTextDetectionCatalogService(
        new ProjectCatalogJsonLoader(new ObjectMapper()));
    MissionProposalTextDetectionPatternFactory patternFactory = new MissionProposalTextDetectionPatternFactory(
        catalogService);
    MissionProposalTextEvidenceRecorder evidenceRecorder = new MissionProposalTextEvidenceRecorder();
    MissionProposalTextMatcher textMatcher = new MissionProposalTextMatcher();

    return new MissionProposalWorkingCopyTextEvidenceDetector(
        new MissionProposalCustomerTextDetector(textMatcher, evidenceRecorder),
        new MissionProposalTitleTextDetector(textMatcher, evidenceRecorder),
        new MissionProposalScheduleTextDetector(catalogService, patternFactory, evidenceRecorder),
        new MissionProposalRequirementTextDetector(
            roleRepo,
            skillLookup,
            skillLevelRepo,
            catalogService,
            patternFactory,
            textMatcher,
            evidenceRecorder));
  }
}
