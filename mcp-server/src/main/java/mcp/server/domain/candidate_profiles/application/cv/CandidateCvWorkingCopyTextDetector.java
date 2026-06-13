package mcp.server.domain.candidate_profiles.application.cv;

import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
final class CandidateCvWorkingCopyTextDetector {

  private final CandidateCvTextMatcher textMatcher;
  private final CandidateCvSectionExtractor sectionExtractor;
  private final CandidateCvContactTextDetector contactTextDetector;
  private final CandidateCvLocationTextDetector locationTextDetector;
  private final CandidateCvExperienceTextDetector experienceTextDetector;
  private final CandidateCvRoleTextDetector roleTextDetector;
  private final CandidateCvSkillTextDetector skillTextDetector;
  private final CandidateCvProfileAttributeTextDetector profileAttributeTextDetector;
  private final CandidateCvExtractionCatalogService extractionCatalogService;

  CandidateCvWorkingCopyTextDetector(
      CandidateCvTextMatcher textMatcher,
      CandidateCvSectionExtractor sectionExtractor,
      CandidateCvContactTextDetector contactTextDetector,
      CandidateCvLocationTextDetector locationTextDetector,
      CandidateCvExperienceTextDetector experienceTextDetector,
      CandidateCvRoleTextDetector roleTextDetector,
      CandidateCvSkillTextDetector skillTextDetector,
      CandidateCvProfileAttributeTextDetector profileAttributeTextDetector,
      CandidateCvExtractionCatalogService extractionCatalogService) {
    this.textMatcher = textMatcher;
    this.sectionExtractor = sectionExtractor;
    this.contactTextDetector = contactTextDetector;
    this.locationTextDetector = locationTextDetector;
    this.experienceTextDetector = experienceTextDetector;
    this.roleTextDetector = roleTextDetector;
    this.skillTextDetector = skillTextDetector;
    this.profileAttributeTextDetector = profileAttributeTextDetector;
    this.extractionCatalogService = extractionCatalogService;
  }

  CandidateCvWebContract.CandidateCvProfileWorkingCopyView toWorkingCopy(
      CandCvTextExtractionService.ExtractionResult extraction) {

    String extractedText = mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText(
        extraction.extractedText());
    if (extractedText.isBlank()) {
      return emptyWorkingCopy("");
    }

    List<String> lines = textMatcher.normalizedLines(extractedText);
    CandidateCvSectionExtractor.Sections sections = sectionExtractor.extract(lines);
    CandidateCvContactTextDetector.ContactInfo contact = contactTextDetector.detect(lines, extractedText);
    CandidateCvLocationTextDetector.DetectedLocation candidateLocation = locationTextDetector.detectCandidateLocation(
        lines,
        extractedText);
    List<CandidateCvWebContract.CandidateEducationWorkingCopyView> educations = experienceTextDetector
        .detectEducations(sections.educationLines());
    List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> workExperiences = experienceTextDetector
        .detectWorkExperiences(sections.workExperienceLines(), candidateLocation);
    String yearsOfExperience = experienceTextDetector.detectYearsOfExperience(
        extractedText,
        educations,
        workExperiences);
    CandidateCvRoleTextDetector.DetectedRole detectedRole = roleTextDetector.detectRole(
        extractedText,
        sections.profileLines());
    List<CandidateCvWebContract.CandidateRoleWorkingCopyView> candidateRoles = detectedRole.toCandidateRoles(
        roleTextDetector.parseRoleExperienceYears(yearsOfExperience));
    String profileSummary = textMatcher.joinLines(sections.profileLines());
    boolean willingToRelocate = textMatcher.containsIgnoreCase(extractedText, "relocate")
        || textMatcher.containsIgnoreCase(extractedText, "relocation")
        || textMatcher.containsIgnoreCase(extractedText, "flytta");
    String detectedWorkModeText = profileAttributeTextDetector.detectWorkModeText(extractedText, profileSummary);
    String workMode = profileAttributeTextDetector.normalizeCandidateWorkMode(detectedWorkModeText);
    String locationFlexibility = !detectedWorkModeText.isBlank()
        ? detectedWorkModeText
        : willingToRelocate ? "Can relocate" : "";
    List<CandidateCvWebContract.CandidateSkillWorkingCopyView> candidateSkills = skillTextDetector
        .detectCandidateSkills(sections.skillLines(), extractedText);
    String skillsText = skillTextDetector.detectSkillsText(sections.skillLines(), extractedText);
    if (skillsText.isBlank() && !candidateSkills.isEmpty()) {
      skillsText = candidateSkills.stream()
          .map(CandidateCvWebContract.CandidateSkillWorkingCopyView::skillTitle)
          .collect(Collectors.joining(", "));
    }

    return applyDoNotInferConstraints(new CandidateCvWebContract.CandidateCvProfileWorkingCopyView(
        contact.contactEmail(),
        contact.firstName(),
        contact.lastName(),
        contact.phoneNumber(),
        candidateLocation.country(),
        candidateLocation.city(),
        profileAttributeTextDetector.detectWorkStatus(profileSummary),
        profileAttributeTextDetector.detectLanguages(sections.languageLines()),
        detectedRole.roleTitle(),
        candidateRoles,
        profileSummary,
        yearsOfExperience,
        "",
        "",
        skillsText,
        candidateSkills,
        workExperiences,
        workMode,
        locationFlexibility,
        "",
        willingToRelocate,
        educations,
        List.of(),
        false));
  }

  private CandidateCvWebContract.CandidateCvProfileWorkingCopyView emptyWorkingCopy(String contactEmail) {
    return new CandidateCvWebContract.CandidateCvProfileWorkingCopyView(
        contactEmail,
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        List.of(),
        "",
        "",
        "",
        "",
        "",
        List.of(),
        List.of(),
        "",
        "",
        "",
        false,
        List.of(),
        List.of(),
        false);
  }

  private CandidateCvWebContract.CandidateCvProfileWorkingCopyView applyDoNotInferConstraints(
      CandidateCvWebContract.CandidateCvProfileWorkingCopyView view) {

    List<String> doNotInferFields = extractionCatalogService.candidateDoNotInferFields();
    boolean salaryConstrained = doNotInferFields.contains("salary_expectation");
    boolean referenceConsentConstrained = doNotInferFields.contains("consent_to_contact_references");
    if (!salaryConstrained && !referenceConsentConstrained) {
      return view;
    }
    return new CandidateCvWebContract.CandidateCvProfileWorkingCopyView(
        view.contactEmail(),
        view.firstName(),
        view.lastName(),
        view.phoneNumber(),
        view.country(),
        view.city(),
        view.workStatus(),
        view.languages(),
        view.roleTitle(),
        view.candidateRoles(),
        view.profileSummary(),
        view.yearsOfExperience(),
        salaryConstrained ? "" : view.expectedSalary(),
        salaryConstrained ? "" : view.hourlyRate(),
        view.skills(),
        view.candidateSkills(),
        view.workExperiences(),
        view.workMode(),
        view.locationFlexibility(),
        view.preferredLocation(),
        view.willingToRelocate(),
        view.educations(),
        view.certifications(),
        referenceConsentConstrained ? false : view.gdprConsent());
  }
}
