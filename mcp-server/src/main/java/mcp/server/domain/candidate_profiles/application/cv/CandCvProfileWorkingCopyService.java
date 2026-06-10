package mcp.server.domain.candidate_profiles.application.cv;

import org.springframework.stereotype.Service;

import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileCertificationEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEducationEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfilePrimarySkillEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileRoleEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileSecondarySkillEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileWorkExperienceEntity;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupEntity;
import mcp.server.domain.reference_data.persistence.RoleEntity;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.RoleJpaRepo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Service
public class CandCvProfileWorkingCopyService {

  private static final int MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS = 100;

  private final RoleJpaRepo roleRepo;
  private final SkillCatalogLookup skillLookup;
  private final CompetencyLevelLookupJpaRepo skillLevelRepo;
  private final CandidateCvWorkingCopyTextDetector workingCopyTextDetector;

  public CandCvProfileWorkingCopyService(
      RoleJpaRepo roleRepo,
      SkillCatalogLookup skillLookup,
      CompetencyLevelLookupJpaRepo skillLevelRepo,
      CandidateCvWorkingCopyTextDetector workingCopyTextDetector) {
    this.roleRepo = roleRepo;
    this.skillLookup = skillLookup;
    this.skillLevelRepo = skillLevelRepo;
    this.workingCopyTextDetector = workingCopyTextDetector;
  }

  public CandidateCvWebContract.CandidateCvProfileWorkingCopyView toWorkingCopy(
      CandCvTextExtractionService.ExtractionResult extraction) {

    return workingCopyTextDetector.toWorkingCopy(extraction);
  }

  public CandidateCvWebContract.CandidateCvProfileWorkingCopyView toWorkingCopy(
      CandidateCvWebContract.CandidateCvProfileWorkingCopyInput input,
      String contactEmail) {

    if (input == null) {
      return emptyWorkingCopy(safeText(contactEmail).trim());
    }

    List<CandidateCvWebContract.CandidateRoleWorkingCopyView> candidateRoles = toCandRoleWorkingCopyViews(
        input.candidateRoles());
    List<CandidateCvWebContract.CandidateSkillWorkingCopyView> candidateSkills = toCandSkillWorkingCopyViews(
        input.candidateSkills());
    String yearsOfExperience = safeText(input.yearsOfExperience()).trim();
    if (yearsOfExperience.isBlank() && !candidateRoles.isEmpty()) {
      yearsOfExperience = Integer.toString(candidateRoles.get(0).roleExperienceYears());
    }
    String candidateWorkMode = normalizeCandidateWorkMode(input.workMode());

    return new CandidateCvWebContract.CandidateCvProfileWorkingCopyView(
        safeText(contactEmail).trim(),
        safeText(input.firstName()).trim(),
        safeText(input.lastName()).trim(),
        safeText(input.phoneNumber()).trim(),
        safeText(input.country()).trim(),
        safeText(input.city()).trim(),
        safeText(input.workStatus()).trim(),
        safeText(input.languages()).trim(),
        "",
        candidateRoles,
        safeText(input.profileSummary()).trim(),
        yearsOfExperience,
        safeText(input.expectedSalary()).trim(),
        safeText(input.hourlyRate()).trim(),
        "",
        candidateSkills,
        toWorkExperienceWorkingCopyViews(input.workExperiences()),
        candidateWorkMode,
        safeText(input.locationFlexibility()).trim(),
        safeText(input.preferredLocation()).trim(),
        input.willingToRelocate(),
        toEducationWorkingCopyViews(input.educations()),
        toCertificationWorkingCopyViews(input.certifications()),
        input.gdprConsent());
  }

  public CandidateCvWebContract.CandidateCvProfileWorkingCopyView toWorkingCopy(
      CandidateProfileEntity entity) {

    List<CandidateCvWebContract.CandidateRoleWorkingCopyView> candidateRoles = entity.getCandidateRoles().stream()
        .sorted(Comparator.comparing(CandidateProfileRoleEntity::getRoleNumber))
        .map(this::toCandRoleWorkingCopyView)
        .toList();
    candidateRoles = applyExactPrimaryExperienceYears(
        candidateRoles,
        safeText(entity.getYearsOfExperience()));

    return new CandidateCvWebContract.CandidateCvProfileWorkingCopyView(
        safeText(entity.getContactEmail()),
        safeText(entity.getFirstName()),
        safeText(entity.getLastName()),
        safeText(entity.getPhoneNumber()),
        safeText(entity.getCountry()),
        safeText(entity.getCity()),
        safeText(entity.getWorkStatus()),
        safeText(entity.getLanguages()),
        "",
        candidateRoles,
        safeText(entity.getProfileSummary()),
        safeText(entity.getYearsOfExperience()),
        safeText(entity.getExpectedSalary()),
        safeText(entity.getHourlyRate()),
        "",
        candidateSkillWorkingCopyViews(entity),
        entity.getWorkExperiences().stream()
            .sorted(Comparator.comparing(CandidateProfileWorkExperienceEntity::getWorkExperienceNumber))
            .map(this::toWorkExperienceWorkingCopyView)
            .toList(),
        normalizeCandidateWorkMode(entity.getWorkMode()),
        safeText(entity.getLocationFlexibility()),
        safeText(entity.getPreferredLocation()),
        Boolean.TRUE.equals(entity.getWillingToRelocate()),
        entity.getEducations().stream()
            .sorted(Comparator.comparing(CandidateProfileEducationEntity::getEducationNumber))
            .map(this::toEducationWorkingCopyView)
            .toList(),
        entity.getCertifications().stream()
            .sorted(Comparator.comparing(CandidateProfileCertificationEntity::getCertificationNumber))
            .map(this::toCertificationWorkingCopyView)
            .toList(),
        Boolean.TRUE.equals(entity.getGdprConsent()));
  }

  public CandidateCvWebContract.CandidateCvProfileWorkingCopyView emptyWorkingCopy(String contactEmail) {
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

  public CandidateCvWebContract.CandidateCvProfileWorkingCopyView mergeWorkingCopy(
      CandidateCvWebContract.CandidateCvProfileWorkingCopyView current,
      CandidateCvWebContract.CandidateCvProfileWorkingCopyView extracted) {

    List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> workExperiences = current.workExperiences() == null
        || current.workExperiences().isEmpty()
            ? extracted.workExperiences()
            : current.workExperiences();
    List<CandidateCvWebContract.CandidateCertificationWorkingCopyView> certifications = current.certifications() == null
        || current.certifications().isEmpty()
            ? extracted.certifications()
            : current.certifications();
    List<CandidateCvWebContract.CandidateEducationWorkingCopyView> educations = current.educations() == null
        || current.educations().isEmpty()
            ? extracted.educations()
            : current.educations();

    return new CandidateCvWebContract.CandidateCvProfileWorkingCopyView(
        mergeWorkingCopyText(current.contactEmail(), extracted.contactEmail()),
        mergeWorkingCopyText(current.firstName(), extracted.firstName()),
        mergeWorkingCopyText(current.lastName(), extracted.lastName()),
        mergeWorkingCopyText(current.phoneNumber(), extracted.phoneNumber()),
        mergeWorkingCopyText(current.country(), extracted.country()),
        mergeWorkingCopyText(current.city(), extracted.city()),
        mergeWorkingCopyText(current.workStatus(), extracted.workStatus()),
        mergeWorkingCopyText(current.languages(), extracted.languages()),
        "",
        mergeCandRoles(current.candidateRoles(), extracted.candidateRoles()),
        mergeWorkingCopyText(current.profileSummary(), extracted.profileSummary()),
        mergeWorkingCopyText(current.yearsOfExperience(), extracted.yearsOfExperience()),
        mergeWorkingCopyText(current.expectedSalary(), extracted.expectedSalary()),
        mergeWorkingCopyText(current.hourlyRate(), extracted.hourlyRate()),
        "",
        mergeCandSkills(current.candidateSkills(), extracted.candidateSkills()),
        workExperiences,
        normalizeCandidateWorkMode(
            mergeWorkingCopyText(current.workMode(), extracted.workMode())),
        mergeWorkingCopyText(current.locationFlexibility(), extracted.locationFlexibility()),
        mergeWorkingCopyText(current.preferredLocation(), extracted.preferredLocation()),
        Boolean.TRUE.equals(current.willingToRelocate())
            || Boolean.TRUE.equals(extracted.willingToRelocate()),
        educations,
        certifications,
        Boolean.TRUE.equals(current.gdprConsent()) || Boolean.TRUE.equals(extracted.gdprConsent()));
  }

  public List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> toWorkExperienceWorkingCopyViews(
      List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyInput> workExperiences) {

    if (workExperiences == null || workExperiences.isEmpty()) {
      return List.of();
    }

    return workExperiences.stream()
        .filter(this::hasWorkExperienceData)
        .map(workExperience -> new CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView(
            safeText(workExperience.jobTitle()).trim(),
            safeText(workExperience.workExpCompany()).trim(),
            safeText(workExperience.workExpCompanyOrgNr()).trim(),
            List.of(),
            safeText(workExperience.city()).trim(),
            safeText(workExperience.country()).trim(),
            safeText(workExperience.startDate()).trim(),
            safeText(workExperience.endDate()).trim(),
            workExperience.currentlyHere()))
        .toList();
  }

  public List<CandidateCvWebContract.CandidateCertificationWorkingCopyView> toCertificationWorkingCopyViews(
      List<CandidateCvWebContract.CandidateCertificationWorkingCopyInput> certifications) {

    if (certifications == null || certifications.isEmpty()) {
      return List.of();
    }

    return certifications.stream()
        .filter(this::hasCertificationData)
        .map(certification -> new CandidateCvWebContract.CandidateCertificationWorkingCopyView(
            safeText(certification.name()).trim(),
            certification.documentAttached(),
            safeText(certification.documentFileName()).trim(),
            safeText(certification.documentContentType()).trim(),
            certification.documentSizeBytes()))
        .toList();
  }

  public List<CandidateCvWebContract.CandidateEducationWorkingCopyView> toEducationWorkingCopyViews(
      List<CandidateCvWebContract.CandidateEducationWorkingCopyInput> educations) {

    if (educations == null || educations.isEmpty()) {
      return List.of();
    }

    return educations.stream()
        .filter(this::hasEducationData)
        .map(education -> new CandidateCvWebContract.CandidateEducationWorkingCopyView(
            safeText(education.institution()).trim(),
            safeText(education.fieldOfStudy()).trim(),
            safeText(education.startDate()).trim(),
            safeText(education.endDate()).trim(),
            education.currentlyStudying()))
        .toList();
  }

  public List<CandidateCvWebContract.CandidateRoleWorkingCopyView> toCandRoleWorkingCopyViews(
      List<CandidateCvWebContract.CandidateRoleWorkingCopyInput> candidateRoles) {

    if (candidateRoles == null || candidateRoles.isEmpty()) {
      return List.of();
    }

    java.util.LinkedHashMap<Long, CandidateCvWebContract.CandidateRoleWorkingCopyView> rolesById = new java.util.LinkedHashMap<>();
    for (CandidateCvWebContract.CandidateRoleWorkingCopyInput candidateRole : candidateRoles) {
      if (candidateRole == null || candidateRole.roleId() <= 0) {
        continue;
      }
      if (candidateRole.roleExperienceYears() < 0) {
        throw reject("candidateRoles.roleExperienceYears must be zero or positive");
      }
      if (candidateRole.roleExperienceYears() > MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS) {
        throw reject("candidateRoles.roleExperienceYears must be 100 or less");
      }
      RoleEntity role = roleRepo.findById(candidateRole.roleId())
          .orElseThrow(() -> reject("candidateRoles contains an unknown roleId"));
      rolesById.putIfAbsent(
          role.getId(),
          new CandidateCvWebContract.CandidateRoleWorkingCopyView(
              role.getId(),
              role.getRoleTitle(),
              candidateRole.roleExperienceYears()));
    }
    return List.copyOf(rolesById.values());
  }

  public List<CandidateCvWebContract.CandidateSkillWorkingCopyView> toCandSkillWorkingCopyViews(
      List<CandidateCvWebContract.CandidateSkillWorkingCopyInput> candidateSkills) {

    if (candidateSkills == null || candidateSkills.isEmpty()) {
      return List.of();
    }

    java.util.LinkedHashMap<String, CandidateCvWebContract.CandidateSkillWorkingCopyView> skillsById = new java.util.LinkedHashMap<>();
    for (CandidateCvWebContract.CandidateSkillWorkingCopyInput candidateSkill : candidateSkills) {
      if (candidateSkill == null || candidateSkill.skillId() <= 0) {
        continue;
      }
      SkillCatalogLookup.SkillRef skill = resolveCandSkill(candidateSkill.skillId(),
          candidateSkill.skillCategory());
      CompetencyLevelLookupEntity skillLevel = skillLevelRepo.findById(candidateSkill.skillLevelId())
          .orElseThrow(() -> reject("candidateSkills contains an unknown skillLevelId"));
      skillsById.putIfAbsent(
          skillKey(skill),
          new CandidateCvWebContract.CandidateSkillWorkingCopyView(
              skill.id(),
              skill.title(),
              skill.category(),
              skillLevel.getCompetencyLevelLookupId(),
              skillLevel.getCompetencyLevelName()));
    }
    return List.copyOf(skillsById.values());
  }

  private SkillCatalogLookup.SkillRef resolveCandSkill(long skillId, String rawCategory) {
    String category = safeText(rawCategory).trim();
    if (SkillCatalogLookup.CATEGORY_PRIMARY.equalsIgnoreCase(category)) {
      return skillLookup.findPrimarySkillById(skillId)
          .orElseThrow(() -> reject("candidateSkills contains an unknown primary skillId"));
    }
    if (SkillCatalogLookup.CATEGORY_SECONDARY.equalsIgnoreCase(category)) {
      return skillLookup.findSecondarySkillById(skillId)
          .orElseThrow(() -> reject("candidateSkills contains an unknown secondary skillId"));
    }
    return skillLookup.findAnySkillByIdPreferPrimary(skillId)
        .orElseThrow(() -> reject("candidateSkills contains an unknown skillId"));
  }

  private String skillKey(SkillCatalogLookup.SkillRef skill) {
    return skill.category() + ":" + skill.id();
  }

  private String mergeWorkingCopyText(String current, String extracted) {
    String currentValue = safeText(current).trim();
    return currentValue.isBlank() ? safeText(extracted).trim() : currentValue;
  }

  private String normalizeCandidateWorkMode(String value) {
    String normalized = safeText(value).trim().toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      return "";
    }
    if (normalized.contains("REMOTE")) {
      return "REMOTE";
    }
    if (normalized.contains("HYBRID")) {
      return "HYBRID";
    }
    if (normalized.contains("ON_PREMISE")
        || normalized.contains("ON-PREMISE")
        || normalized.contains("ON PREMISE")
        || normalized.contains("ON_SITE")
        || normalized.contains("ON-SITE")
        || normalized.contains("ON SITE")
        || normalized.contains("ONSITE")
        || normalized.contains("SITE")) {
      return "ON_PREMISE";
    }
    return normalized;
  }

  private List<CandidateCvWebContract.CandidateRoleWorkingCopyView> mergeCandRoles(
      List<CandidateCvWebContract.CandidateRoleWorkingCopyView> current,
      List<CandidateCvWebContract.CandidateRoleWorkingCopyView> extracted) {

    return current == null || current.isEmpty() ? safeWorkingCopyViews(extracted) : safeWorkingCopyViews(current);
  }

  private List<CandidateCvWebContract.CandidateSkillWorkingCopyView> mergeCandSkills(
      List<CandidateCvWebContract.CandidateSkillWorkingCopyView> current,
      List<CandidateCvWebContract.CandidateSkillWorkingCopyView> extracted) {

    return current == null || current.isEmpty() ? safeWorkingCopyViews(extracted) : safeWorkingCopyViews(current);
  }

  private <T> List<T> safeWorkingCopyViews(List<T> value) {

    return value == null ? List.of() : value;
  }

  private List<CandidateCvWebContract.CandidateSkillWorkingCopyView> candidateSkillWorkingCopyViews(
      CandidateProfileEntity entity) {

    List<CandidateCvWebContract.CandidateSkillWorkingCopyView> primarySkills = entity.getPrimarySkills().stream()
        .sorted(Comparator.comparing(CandidateProfilePrimarySkillEntity::getSkillNumber))
        .map(this::toCandSkillWorkingCopyView)
        .toList();
    List<CandidateCvWebContract.CandidateSkillWorkingCopyView> secondarySkills = entity.getSecondarySkills().stream()
        .sorted(Comparator.comparing(CandidateProfileSecondarySkillEntity::getSkillNumber))
        .map(this::toCandSkillWorkingCopyView)
        .toList();
    List<CandidateCvWebContract.CandidateSkillWorkingCopyView> combined = new ArrayList<>(
        primarySkills.size() + secondarySkills.size());
    combined.addAll(primarySkills);
    combined.addAll(secondarySkills);
    return List.copyOf(combined);
  }

  private CandidateCvWebContract.CandidateRoleWorkingCopyView toCandRoleWorkingCopyView(
      CandidateProfileRoleEntity entity) {

    return new CandidateCvWebContract.CandidateRoleWorkingCopyView(
        entity.getRoleId(),
        safeText(entity.getRoleTitle()),
        entity.getRoleExperienceYears() == null ? 0 : entity.getRoleExperienceYears());
  }

  private List<CandidateCvWebContract.CandidateRoleWorkingCopyView> applyExactPrimaryExperienceYears(
      List<CandidateCvWebContract.CandidateRoleWorkingCopyView> roles,
      String yearsOfExperience) {

    if (roles == null || roles.isEmpty()) {
      return roles == null ? List.of() : roles;
    }
    String trimmedYears = safeText(yearsOfExperience).trim();
    if (!trimmedYears.matches("\\d+")) {
      return roles;
    }
    int exactYears = Integer.parseInt(trimmedYears);
    if (exactYears <= roles.get(0).roleExperienceYears()) {
      return roles;
    }

    List<CandidateCvWebContract.CandidateRoleWorkingCopyView> adjustedRoles = new ArrayList<>(roles);
    CandidateCvWebContract.CandidateRoleWorkingCopyView primaryRole = adjustedRoles.get(0);
    adjustedRoles.set(0, new CandidateCvWebContract.CandidateRoleWorkingCopyView(
        primaryRole.roleId(),
        primaryRole.roleTitle(),
        exactYears));
    return adjustedRoles;
  }

  private CandidateCvWebContract.CandidateSkillWorkingCopyView toCandSkillWorkingCopyView(
      CandidateProfilePrimarySkillEntity entity) {

    return new CandidateCvWebContract.CandidateSkillWorkingCopyView(
        entity.getSkillId(),
        safeText(entity.getSkillTitle()),
        SkillCatalogLookup.CATEGORY_PRIMARY,
        entity.getSkillLevelId() == null ? 0 : entity.getSkillLevelId(),
        safeText(entity.getCompetencyLevelName()));
  }

  private CandidateCvWebContract.CandidateSkillWorkingCopyView toCandSkillWorkingCopyView(
      CandidateProfileSecondarySkillEntity entity) {

    return new CandidateCvWebContract.CandidateSkillWorkingCopyView(
        entity.getSkillId(),
        safeText(entity.getSkillTitle()),
        SkillCatalogLookup.CATEGORY_SECONDARY,
        entity.getSkillLevelId() == null ? 0 : entity.getSkillLevelId(),
        safeText(entity.getCompetencyLevelName()));
  }

  private CandidateCvWebContract.CandidateCertificationWorkingCopyView toCertificationWorkingCopyView(
      CandidateProfileCertificationEntity entity) {

    boolean documentAttached = entity.getDocumentBytes() != null
        || !safeText(entity.getDocumentFileName()).trim().isBlank()
        || !safeText(entity.getDocumentContentType()).trim().isBlank()
        || entity.getDocumentSizeBytes() != null;
    return new CandidateCvWebContract.CandidateCertificationWorkingCopyView(
        safeText(entity.getCertificationName()),
        documentAttached,
        safeText(entity.getDocumentFileName()),
        safeText(entity.getDocumentContentType()),
        entity.getDocumentSizeBytes());
  }

  private CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView toWorkExperienceWorkingCopyView(
      CandidateProfileWorkExperienceEntity entity) {

    return new CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView(
        safeText(entity.getJobTitle()),
        safeText(entity.getWorkExpCompany()),
        safeText(entity.getWorkExpCompanyOrgNr()),
        List.of(),
        safeText(entity.getCity()),
        safeText(entity.getCountry()),
        formatLocalDate(entity.getStartDate()),
        formatLocalDate(entity.getEndDate()),
        Boolean.TRUE.equals(entity.getCurrentlyHere()));
  }

  private CandidateCvWebContract.CandidateEducationWorkingCopyView toEducationWorkingCopyView(
      CandidateProfileEducationEntity entity) {

    return new CandidateCvWebContract.CandidateEducationWorkingCopyView(
        safeText(entity.getInstitution()),
        safeText(entity.getFieldOfStudy()),
        formatLocalDate(entity.getStartDate()),
        formatLocalDate(entity.getEndDate()),
        Boolean.TRUE.equals(entity.getCurrentlyStudying()));
  }

  private boolean hasWorkExperienceData(
      CandidateCvWebContract.CandidateWorkExperienceWorkingCopyInput workExperience) {

    return workExperience != null
        && (Boolean.TRUE.equals(workExperience.currentlyHere())
            || !safeText(workExperience.jobTitle()).trim().isBlank()
            || !safeText(workExperience.workExpCompany()).trim().isBlank()
            || !safeText(workExperience.workExpCompanyOrgNr()).trim().isBlank()
            || !safeText(workExperience.city()).trim().isBlank()
            || !safeText(workExperience.country()).trim().isBlank()
            || !safeText(workExperience.startDate()).trim().isBlank()
            || !safeText(workExperience.endDate()).trim().isBlank());
  }

  private boolean hasCertificationData(
      CandidateCvWebContract.CandidateCertificationWorkingCopyInput certification) {

    return certification != null
        && (certification.documentAttached()
            || !safeText(certification.name()).trim().isBlank()
            || !safeText(certification.documentFileName()).trim().isBlank()
            || !safeText(certification.documentContentType()).trim().isBlank()
            || certification.documentSizeBytes() != null);
  }

  private boolean hasEducationData(
      CandidateCvWebContract.CandidateEducationWorkingCopyInput education) {

    return education != null
        && (education.currentlyStudying()
            || !safeText(education.institution()).trim().isBlank()
            || !safeText(education.fieldOfStudy()).trim().isBlank()
            || !safeText(education.startDate()).trim().isBlank()
            || !safeText(education.endDate()).trim().isBlank());
  }

  private String formatLocalDate(LocalDate value) {
    return value == null ? "" : value.toString();
  }
}
