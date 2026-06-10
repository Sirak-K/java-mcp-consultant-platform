package mcp.server.domain.candidate_profiles.application;

import org.springframework.stereotype.Component;

import mcp.server.domain.candidate_profiles.application.cv.CandCvPreviewService;
import mcp.server.domain.candidate_profiles.application.cv.CandCvProfileWorkingCopyService;
import mcp.server.domain.candidate_profiles.application.cv.CandCvTextExtractionService;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfilePrimarySkillEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileRoleEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileSecondarySkillEntity;
import mcp.server.domain.candidate_profiles.web.CandidateApplicationWebContract;
import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import mcp.server.domain.candidate_profiles.web.RegisteredCandidateProfileWebContract;
import mcp.server.domain.matching.api.MissionMatchDiscoveryResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
public class CandidateProfileWebViewAssembler {

  private static final String CV_SUMMARY_NOT_GENERATED = "NOT_GENERATED";
  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

  private final CandCvPreviewService cvPreviewService;
  private final CandCvProfileWorkingCopyService cvProfileWorkingCopyService;

  public CandidateProfileWebViewAssembler(
      CandCvPreviewService cvPreviewService,
      CandCvProfileWorkingCopyService cvProfileWorkingCopyService) {
    this.cvPreviewService = cvPreviewService;
    this.cvProfileWorkingCopyService = cvProfileWorkingCopyService;
  }

  public CandidateApplicationWebContract.CandidateApplicationView toApplicationView(
      CandidateProfileEntity entity,
      List<MissionMatchDiscoveryResult> findMissionResults) {

    return new CandidateApplicationWebContract.CandidateApplicationView(
        entity.getId(),
        toSpecification(entity),
        toCvExtraction(entity),
        toProfileSummary(entity),
        findMissionResults == null ? List.of() : findMissionResults,
        "",
        formatInstant(entity.getCreatedAt()),
        formatInstant(entity.getUpdatedAt()));
  }

  public RegisteredCandidateProfileWebContract.RegisteredCandidateProfileCardView toRegisteredProfileCardView(
      CandidateProfileEntity entity) {

    CandidateProfileRoleEntity primaryRole = entity.getCandidateRoles().stream()
        .min(Comparator.comparing(CandidateProfileRoleEntity::getRoleNumber))
        .orElse(null);
    Integer roleExperienceYears = primaryRole == null ? null : (int) primaryRole.getRoleExperienceYears();
    List<CandidateProfilePrimarySkillEntity> primarySkills = entity.getPrimarySkills().stream()
        .sorted(Comparator.comparing(CandidateProfilePrimarySkillEntity::getSkillNumber))
        .toList();
    List<CandidateProfileSecondarySkillEntity> secondarySkills = entity.getSecondarySkills().stream()
        .sorted(Comparator.comparing(CandidateProfileSecondarySkillEntity::getSkillNumber))
        .toList();
    return new RegisteredCandidateProfileWebContract.RegisteredCandidateProfileCardView(
        entity.getId(),
        displayName(entity),
        primaryRole == null ? "" : safeText(primaryRole.getRoleTitle()),
        roleExperienceLevel(roleExperienceYears),
        roleExperienceYears,
        safeText(entity.getWorkStatus()),
        safeText(entity.getCountry()),
        locationFlexibility(entity),
        safeText(entity.getWorkMode()),
        primarySkills.stream()
            .map(this::toCardSkillView)
            .toList(),
        secondarySkills.stream()
            .map(this::toCardSkillView)
            .toList());
  }

  private CandidateApplicationWebContract.CandidateProfileSpecificationView toSpecification(
      CandidateProfileEntity entity) {

    return new CandidateApplicationWebContract.CandidateProfileSpecificationView(
        entity.getContactEmail(),
        entity.getCvFileName(),
        entity.getCvContentType(),
        entity.getCvSizeBytes(),
        Boolean.TRUE.equals(entity.getCvExtractionPending()),
        cvProfileWorkingCopyService.toWorkingCopy(entity));
  }

  private CandidateCvWebContract.CandidateCvExtractionView toCvExtraction(CandidateProfileEntity entity) {
    return cvPreviewService.toExtractionView(
        safeText(entity.getCvExtractionStatus()).isBlank()
            ? CandCvTextExtractionService.METADATA_ONLY
            : entity.getCvExtractionStatus(),
        entity.getCvExtractedText(),
        entity.getCvExtractionError(),
        entity.getCvExtractedAt());
  }

  private CandidateApplicationWebContract.CandidateProfileSummaryView toProfileSummary(CandidateProfileEntity entity) {
    return new CandidateApplicationWebContract.CandidateProfileSummaryView(
        safeText(entity.getCvSummaryStatus()).isBlank()
            ? CV_SUMMARY_NOT_GENERATED
            : entity.getCvSummaryStatus(),
        safeText(entity.getCvSummaryCoreCompetenceOverview()),
        safeText(entity.getCvSummaryLocation()),
        safeText(entity.getCvSummaryOtherDetails()),
        formatInstant(entity.getCvSummaryGeneratedAt()));
  }

  private String displayName(CandidateProfileEntity entity) {
    String firstName = safeText(entity.getFirstName()).trim();
    String lastName = safeText(entity.getLastName()).trim();
    String fullName = (firstName + " " + lastName).trim();
    return fullName.isBlank() ? safeText(entity.getContactEmail()).trim() : fullName;
  }

  private String roleExperienceLevel(Integer years) {
    if (years == null) {
      return "";
    }
    if (years >= 5) {
      return "SENIOR";
    }
    if (years >= 3) {
      return "INTERMEDIATE";
    }
    return "JUNIOR";
  }

  private String locationFlexibility(CandidateProfileEntity entity) {
    String explicitFlexibility = safeText(entity.getLocationFlexibility()).trim();
    if (!explicitFlexibility.isBlank()) {
      return explicitFlexibility;
    }
    return Boolean.TRUE.equals(entity.getWillingToRelocate())
        ? "Willing to Relocate"
        : "Unwilling to Relocate";
  }

  private RegisteredCandidateProfileWebContract.RegisteredCandidateProfileCardSkillView toCardSkillView(
      CandidateProfilePrimarySkillEntity skill) {

    return toCardSkillView(skill.getSkillTitle(), skill.getCompetencyLevelName());
  }

  private RegisteredCandidateProfileWebContract.RegisteredCandidateProfileCardSkillView toCardSkillView(
      CandidateProfileSecondarySkillEntity skill) {

    return toCardSkillView(skill.getSkillTitle(), skill.getCompetencyLevelName());
  }

  private RegisteredCandidateProfileWebContract.RegisteredCandidateProfileCardSkillView toCardSkillView(
      String skillTitle,
      String competencyLevelName) {

    String title = safeText(skillTitle).trim();
    String skillLevel = safeText(competencyLevelName).trim();
    if (skillLevel.isBlank()) {
      skillLevel = "N/A";
    }
    return new RegisteredCandidateProfileWebContract.RegisteredCandidateProfileCardSkillView(title, skillLevel);
  }

  private String formatInstant(Instant instant) {
    return instant == null ? null : TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
  }
}
