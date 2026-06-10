package mcp.server.domain.candidate_profiles.application;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import mcp.server.domain.candidate_profiles.application.cv.CandCvProfileWorkingCopyService;
import mcp.server.domain.candidate_profiles.application.cv.CandCvTextExtractionService;
import mcp.server.domain.candidate_profiles.application.intake.CandApplicationValidationService;
import mcp.server.domain.candidate_profiles.application.registered_profiles.CandProfileValidationService;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEntity;
import mcp.server.domain.candidate_profiles.web.CandidateApplicationWebContract;
import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import mcp.server.domain.candidate_profiles.web.RegisteredCandidateProfileWebContract;

import java.time.Instant;
import java.util.List;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
public class CandidateProfileEntityAssembler {

  private static final String CV_SUMMARY_NOT_GENERATED = "NOT_GENERATED";
  private static final String CV_SUMMARY_GENERATED = "GENERATED";

  private final CandApplicationValidationService candidateApplicationValidationService;
  private final CandProfileValidationService candidateProfileValidationService;
  private final CandCvProfileWorkingCopyService cvProfileWorkingCopyService;
  private final CandidateProfileDetailEntityAssembler detailEntityAssembler;

  public CandidateProfileEntityAssembler(
      CandApplicationValidationService candidateApplicationValidationService,
      CandProfileValidationService candidateProfileValidationService,
      CandCvProfileWorkingCopyService cvProfileWorkingCopyService,
      CandidateProfileDetailEntityAssembler detailEntityAssembler) {
    this.candidateApplicationValidationService = candidateApplicationValidationService;
    this.candidateProfileValidationService = candidateProfileValidationService;
    this.cvProfileWorkingCopyService = cvProfileWorkingCopyService;
    this.detailEntityAssembler = detailEntityAssembler;
  }

  public CandidateProfileEntity toEntity(
      CandidateApplicationWebContract.CandidateApplicationInput request,
      CandCvTextExtractionService.ExtractionResult extraction,
      List<MultipartFile> certificateFiles) {

    CandidateApplicationWebContract.CandidateProfileSpecificationView specification = toSpecification(request);
    CandidateCvWebContract.CandidateCvProfileWorkingCopyView profileWorkingCopy = cvProfileWorkingCopyService
        .mergeWorkingCopy(
            specification.profileWorkingCopy(),
            cvProfileWorkingCopyService.toWorkingCopy(extraction));
    boolean extractionPending = CandCvTextExtractionService.METADATA_ONLY.equals(extraction.status());
    CandidateApplicationWebContract.CandidateProfileSummaryInput generatedSummary = request.generatedSummary();
    boolean hasGeneratedSummary = hasCandidateProfileSummary(generatedSummary);
    Instant now = Instant.now();
    CandidateProfileEntity entity = new CandidateProfileEntity(
        null,
        specification.contactEmail(),
        specification.cvFileName(),
        specification.cvContentType(),
        specification.cvSizeBytes(),
        extractionPending,
        extraction.status(),
        extraction.extractedText(),
        extraction.error(),
        extraction.extractedAt(),
        profileWorkingCopy.firstName(),
        profileWorkingCopy.lastName(),
        profileWorkingCopy.phoneNumber(),
        profileWorkingCopy.country(),
        profileWorkingCopy.city(),
        profileWorkingCopy.workStatus(),
        profileWorkingCopy.languages(),
        "",
        profileWorkingCopy.profileSummary(),
        profileWorkingCopy.yearsOfExperience(),
        profileWorkingCopy.expectedSalary(),
        profileWorkingCopy.hourlyRate(),
        "",
        safeText(profileWorkingCopy.workMode()).trim(),
        profileWorkingCopy.locationFlexibility(),
        profileWorkingCopy.preferredLocation(),
        profileWorkingCopy.willingToRelocate(),
        profileWorkingCopy.gdprConsent(),
        hasGeneratedSummary ? CV_SUMMARY_GENERATED : CV_SUMMARY_NOT_GENERATED,
        hasGeneratedSummary ? safeText(generatedSummary.coreCompetenceOverview()).trim() : "",
        hasGeneratedSummary ? safeText(generatedSummary.location()).trim() : "",
        hasGeneratedSummary ? safeText(generatedSummary.otherDetails()).trim() : "",
        hasGeneratedSummary ? now : null,
        now,
        now,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
    entity.replaceWorkExperiences(
        detailEntityAssembler.toWorkExperienceEntities(profileWorkingCopy.workExperiences()));
    entity.replaceEducations(detailEntityAssembler.toEducationEntities(profileWorkingCopy.educations()));
    entity.replaceCandidateRoles(
        detailEntityAssembler.toCandidateRoleEntities(profileWorkingCopy.candidateRoles(), entity.getCandidateRoles()));
    entity.replacePrimarySkills(
        detailEntityAssembler.toPrimarySkillEntities(profileWorkingCopy.candidateSkills(), entity.getPrimarySkills()));
    entity.replaceSecondarySkills(
        detailEntityAssembler.toSecondarySkillEntities(
            profileWorkingCopy.candidateSkills(),
            entity.getSecondarySkills()));
    entity.replaceCertifications(
        detailEntityAssembler.toCertificationEntities(
            profileWorkingCopy.certifications(),
            certificateFiles,
            List.of()));
    return entity;
  }

  public void applyRegisteredProfileEdit(
      CandidateProfileEntity entity,
      RegisteredCandidateProfileWebContract.RegisteredCandidateProfileEditInput request) {

    CandidateApplicationWebContract.CandidateProfileSpecificationView specification = toSpecification(request);
    CandidateCvWebContract.CandidateCvProfileWorkingCopyView profileWorkingCopy = specification.profileWorkingCopy();
    entity.setContactEmail(specification.contactEmail());
    entity.setCvFileName(specification.cvFileName());
    entity.setCvContentType(specification.cvContentType());
    entity.setCvSizeBytes(specification.cvSizeBytes());
    entity.setCvExtractionPending(Boolean.TRUE.equals(entity.getCvExtractionPending()));
    entity.setFirstName(profileWorkingCopy.firstName());
    entity.setLastName(profileWorkingCopy.lastName());
    entity.setPhoneNumber(profileWorkingCopy.phoneNumber());
    entity.setCountry(profileWorkingCopy.country());
    entity.setCity(profileWorkingCopy.city());
    entity.setWorkStatus(profileWorkingCopy.workStatus());
    entity.setLanguages(profileWorkingCopy.languages());
    entity.setRoleTitle("");
    entity.setProfileSummary(profileWorkingCopy.profileSummary());
    entity.setYearsOfExperience(profileWorkingCopy.yearsOfExperience());
    entity.setExpectedSalary(profileWorkingCopy.expectedSalary());
    entity.setHourlyRate(profileWorkingCopy.hourlyRate());
    entity.setSkills("");
    entity.setWorkMode(safeText(profileWorkingCopy.workMode()).trim());
    entity.setLocationFlexibility(profileWorkingCopy.locationFlexibility());
    entity.setPreferredLocation(profileWorkingCopy.preferredLocation());
    entity.setWillingToRelocate(profileWorkingCopy.willingToRelocate());
    entity.setGdprConsent(profileWorkingCopy.gdprConsent());
    applyProfileSummary(entity, request.generatedSummary());
    detailEntityAssembler.syncWorkExperiences(entity, profileWorkingCopy.workExperiences());
    detailEntityAssembler.syncEducations(entity, profileWorkingCopy.educations());
    detailEntityAssembler.syncCandidateRoles(entity, profileWorkingCopy.candidateRoles());
    detailEntityAssembler.syncCandSkills(entity, profileWorkingCopy.candidateSkills());
    detailEntityAssembler.syncCertifications(entity, profileWorkingCopy.certifications());
    entity.setUpdatedAt(Instant.now());
  }

  private CandidateApplicationWebContract.CandidateProfileSpecificationView toSpecification(
      CandidateApplicationWebContract.CandidateApplicationInput request) {

    candidateApplicationValidationService.requireCandidateProfileRequest(request);
    CandidateCvWebContract.CandidateCvProfileWorkingCopyView profileWorkingCopy = cvProfileWorkingCopyService
        .toWorkingCopy(request.profileWorkingCopy(), request.contactEmail());
    candidateProfileValidationService.requireCandidateRegistrationCore(profileWorkingCopy);
    return new CandidateApplicationWebContract.CandidateProfileSpecificationView(
        request.contactEmail().trim(),
        request.cvFileName().trim(),
        request.cvContentType().trim(),
        request.cvSizeBytes(),
        true,
        profileWorkingCopy);
  }

  private CandidateApplicationWebContract.CandidateProfileSpecificationView toSpecification(
      RegisteredCandidateProfileWebContract.RegisteredCandidateProfileEditInput request) {

    return toSpecification(new CandidateApplicationWebContract.CandidateApplicationInput(
        request.contactEmail(),
        request.cvFileName(),
        request.cvContentType(),
        request.cvSizeBytes(),
        request.profileWorkingCopy(),
        request.generatedSummary()));
  }

  private boolean hasCandidateProfileSummary(CandidateApplicationWebContract.CandidateProfileSummaryInput summary) {
    return summary != null
        && (!safeText(summary.coreCompetenceOverview()).trim().isBlank()
            || !safeText(summary.location()).trim().isBlank()
            || !safeText(summary.otherDetails()).trim().isBlank());
  }

  private void applyProfileSummary(
      CandidateProfileEntity entity,
      CandidateApplicationWebContract.CandidateProfileSummaryInput summary) {

    if (summary == null) {
      return;
    }

    boolean hasSummary = hasCandidateProfileSummary(summary);
    entity.setCvSummaryStatus(hasSummary ? CV_SUMMARY_GENERATED : CV_SUMMARY_NOT_GENERATED);
    entity.setCvSummaryCoreCompetenceOverview(hasSummary ? safeText(summary.coreCompetenceOverview()).trim() : "");
    entity.setCvSummaryLocation(hasSummary ? safeText(summary.location()).trim() : "");
    entity.setCvSummaryOtherDetails(hasSummary ? safeText(summary.otherDetails()).trim() : "");
    entity.setCvSummaryGeneratedAt(hasSummary ? Instant.now() : null);
  }
}
