package mcp.server.domain.candidate_profiles.application.cv;

import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import mcp.server.domain.reference_data.application.CompanyIdentityLookupService;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupEntity;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.RoleEntity;
import mcp.server.domain.reference_data.persistence.RoleJpaRepo;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class CandidateCvWorkingCopyTextDetectorTest {

  @Test
  void returnsEmptyWorkingCopyForBlankExtraction() {
    CandidateCvWorkingCopyTextDetector detector = detectorWithLookupData();

    CandidateCvWebContract.CandidateCvProfileWorkingCopyView view = detector.toWorkingCopy(
        new CandCvTextExtractionService.ExtractionResult("EXTRACTED", "", "", null));

    assertThat(view.contactEmail()).isBlank();
    assertThat(view.candidateRoles()).isEmpty();
    assertThat(view.candidateSkills()).isEmpty();
    assertThat(view.workExperiences()).isEmpty();
    assertThat(view.educations()).isEmpty();
  }

  @Test
  void detectsCandidateCvWorkingCopySignalsFromCatalogAndLookupData() {
    CandidateCvWorkingCopyTextDetector detector = detectorWithLookupData();

    CandidateCvWebContract.CandidateCvProfileWorkingCopyView view = detector.toWorkingCopy(
        new CandCvTextExtractionService.ExtractionResult(
            "EXTRACTED",
            String.join(
                "\n",
                "ADA LOVELACE",
                "ada@example.com",
                "+46 70 123 45 67",
                "Ort: Stockholm",
                "PROFIL",
                "Fullstack Developer with 6 years of experience. Available immediately and remote. Can relocate.",
                "UTBILDNING",
                "Computer Science \u2013 KTH",
                "2018 \u2013 2020",
                "ARBETSLIVSERFARENHET",
                "Backend Developer \u2013 Acme AB",
                "2020 \u2013 p\u00E5g\u00E5ende",
                "KOMPETENSER",
                "Java (SENIOR)",
                "Spring Boot",
                "SPR\u00C5KKUNSKAPER",
                "Svenska \u2013 flytande",
                "REFERENSER"),
            "",
            null));

    assertThat(view.contactEmail()).isEqualTo("ada@example.com");
    assertThat(view.phoneNumber()).isEqualTo("46 70 123 45 67");
    assertThat(view.firstName()).isEqualTo("Ada");
    assertThat(view.lastName()).isEqualTo("Lovelace");
    assertThat(view.city()).isEqualTo("Stockholm");
    assertThat(view.country()).isEqualTo("Sweden");
    assertThat(view.workStatus()).isEqualTo("Available");
    assertThat(view.workMode()).isEqualTo("REMOTE");
    assertThat(view.locationFlexibility()).isEqualTo("Remote");
    assertThat(view.languages()).isEqualTo("Svenska (flytande)");

    assertThat(view.candidateRoles()).singleElement()
        .satisfies(role -> {
          assertThat(role.roleId()).isEqualTo(11L);
          assertThat(role.roleTitle()).isEqualTo("Full Stack Developer");
          assertThat(role.roleExperienceYears()).isEqualTo(6);
        });

    assertThat(view.candidateSkills())
        .extracting(CandidateCvWebContract.CandidateSkillWorkingCopyView::skillTitle)
        .contains("Java", "Spring Framework");
    assertThat(view.candidateSkills())
        .filteredOn(skill -> skill.skillTitle().equals("Java"))
        .singleElement()
        .satisfies(skill -> {
          assertThat(skill.skillLevelId()).isEqualTo((short) 3);
          assertThat(skill.skillLevelName()).isEqualTo("SENIOR");
        });

    assertThat(view.educations()).singleElement()
        .satisfies(education -> {
          assertThat(education.fieldOfStudy()).isEqualTo("Computer Science");
          assertThat(education.institution()).isEqualTo("KTH");
          assertThat(education.startDate()).isEqualTo("2018-01-01");
          assertThat(education.endDate()).isEqualTo("2020-12-31");
        });
    assertThat(view.workExperiences()).singleElement()
        .satisfies(workExperience -> {
          assertThat(workExperience.jobTitle()).isEqualTo("Backend Developer");
          assertThat(workExperience.workExpCompany()).isEqualTo("Acme Corporation");
          assertThat(workExperience.workExpCompanyOrgNr()).isEqualTo("556000-0000");
          assertThat(workExperience.city()).isEqualTo("Stockholm");
          assertThat(workExperience.country()).isEqualTo("Sweden");
          assertThat(workExperience.startDate()).isEqualTo("2020-01-01");
          assertThat(workExperience.endDate()).isBlank();
          assertThat(workExperience.currentlyHere()).isTrue();
        });
  }

  private CandidateCvWorkingCopyTextDetector detectorWithLookupData() {
    RoleJpaRepo roleRepo = mock(RoleJpaRepo.class);
    SkillCatalogLookup skillLookup = mock(SkillCatalogLookup.class);
    CompetencyLevelLookupJpaRepo skillLevelRepo = mock(CompetencyLevelLookupJpaRepo.class);
    CompanyIdentityLookupService companyIdentityLookupService = mock(CompanyIdentityLookupService.class);

    when(roleRepo.findAll()).thenReturn(List.of(new RoleEntity(11L, "Full Stack Developer")));
    when(skillLookup.findAllSkills()).thenReturn(List.of(
        new SkillCatalogLookup.SkillRef(101L, "Java", SkillCatalogLookup.CATEGORY_PRIMARY),
        new SkillCatalogLookup.SkillRef(102L, "Spring Framework", SkillCatalogLookup.CATEGORY_PRIMARY)));
    when(skillLevelRepo.findAll()).thenReturn(List.of(
        new CompetencyLevelLookupEntity((short) 1, "JUNIOR", (short) 0),
        new CompetencyLevelLookupEntity((short) 2, "MEDIOR", (short) 3),
        new CompetencyLevelLookupEntity((short) 3, "SENIOR", (short) 5)));
    when(companyIdentityLookupService.resolve("Acme AB")).thenReturn(
        new CompanyIdentityLookupService.CompanyIdentityResolution(
            "Acme Corporation",
            "556000-0000",
            "Stockholm",
            List.of()));

    CandidateCvExtractionCatalogService catalogService = new CandidateCvExtractionCatalogService(
        new ProjectCatalogJsonLoader(new ObjectMapper()));
    assertThat(catalogService.candidateDoNotInferFields())
        .contains("age", "salary_expectation", "consent_to_contact_references");
    CandidateCvTextMatcher textMatcher = new CandidateCvTextMatcher();
    CandidateCvDateRangeParser dateRangeParser = new CandidateCvDateRangeParser();
    CandidateCvSectionExtractor sectionExtractor = new CandidateCvSectionExtractor(catalogService, textMatcher);
    CandidateCvLocationTextDetector locationTextDetector = new CandidateCvLocationTextDetector(
        catalogService,
        textMatcher);
    return new CandidateCvWorkingCopyTextDetector(
        textMatcher,
        sectionExtractor,
        new CandidateCvContactTextDetector(textMatcher, sectionExtractor, dateRangeParser),
        locationTextDetector,
        new CandidateCvExperienceTextDetector(companyIdentityLookupService, locationTextDetector, dateRangeParser),
        new CandidateCvRoleTextDetector(roleRepo, catalogService, textMatcher),
        new CandidateCvSkillTextDetector(skillLookup, skillLevelRepo, catalogService, textMatcher),
        new CandidateCvProfileAttributeTextDetector(catalogService, textMatcher),
        catalogService);
  }
}
