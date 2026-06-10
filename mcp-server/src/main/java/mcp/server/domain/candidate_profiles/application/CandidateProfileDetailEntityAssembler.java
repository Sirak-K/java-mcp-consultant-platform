package mcp.server.domain.candidate_profiles.application;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileCertificationEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEducationEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfilePrimarySkillEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileRoleEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileSecondarySkillEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileWorkExperienceEntity;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.parseOptionalDate;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.toShort;

@Component
public class CandidateProfileDetailEntityAssembler {

  private static final int MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS = 100;
  public List<CandidateProfileWorkExperienceEntity> toWorkExperienceEntities(
      List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> workExperiences) {

    if (workExperiences == null || workExperiences.isEmpty()) {
      return List.of();
    }

    List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> filteredWorkExperiences = workExperiences.stream()
        .filter(this::hasWorkExperienceData)
        .toList();

    return java.util.stream.IntStream.range(0, filteredWorkExperiences.size())
        .mapToObj(index -> {
          CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView workExperience = filteredWorkExperiences
              .get(index);
          return new CandidateProfileWorkExperienceEntity(
              null,
              index + 1,
              safeText(workExperience.jobTitle()).trim(),
              safeText(workExperience.workExpCompany()).trim(),
              safeText(workExperience.workExpCompanyOrgNr()).trim(),
              safeText(workExperience.city()).trim(),
              safeText(workExperience.country()).trim(),
              parseOptionalDate(
                  workExperience.startDate(),
                  "workExperiences.startDate"),
              parseOptionalDate(
                  workExperience.endDate(),
                  "workExperiences.endDate"),
              workExperience.currentlyHere());
        })
        .toList();
  }

  public List<CandidateProfileEducationEntity> toEducationEntities(
      List<CandidateCvWebContract.CandidateEducationWorkingCopyView> educations) {

    if (educations == null || educations.isEmpty()) {
      return List.of();
    }

    List<CandidateCvWebContract.CandidateEducationWorkingCopyView> filteredEducations = educations.stream()
        .filter(this::hasEducationData)
        .toList();

    return java.util.stream.IntStream.range(0, filteredEducations.size())
        .mapToObj(index -> toEducationEntity(index + 1, filteredEducations.get(index)))
        .toList();
  }

  public List<CandidateProfileCertificationEntity> toCertificationEntities(
      List<CandidateCvWebContract.CandidateCertificationWorkingCopyView> certifications,
      List<MultipartFile> uploadedCertificateFiles,
      List<CandidateProfileCertificationEntity> existingCertifications) {

    if (certifications == null || certifications.isEmpty()) {
      return List.of();
    }

    List<CandidateCvWebContract.CandidateCertificationWorkingCopyView> filteredCertifications = certifications.stream()
        .filter(this::hasCertificationData)
        .toList();
    List<CandidateProfileCertificationEntity> sortedExistingCertifications = existingCertifications == null
        ? List.of()
        : existingCertifications.stream()
            .sorted(Comparator.comparing(CandidateProfileCertificationEntity::getCertificationNumber))
            .toList();

    List<CandidateProfileCertificationEntity> mapped = new ArrayList<>();
    int uploadIndex = 0;
    for (int index = 0; index < filteredCertifications.size(); index++) {
      CandidateCvWebContract.CandidateCertificationWorkingCopyView certification = filteredCertifications.get(index);
      CandidateProfileCertificationEntity existing = index < sortedExistingCertifications.size()
          ? sortedExistingCertifications.get(index)
          : null;
      CandidateProfileCertificationEntity existingDocument = null;
      if (certification.documentAttached()
          && (uploadedCertificateFiles == null || uploadIndex >= uploadedCertificateFiles.size())) {
        existingDocument = existing;
      }

      String documentFileName = certification.documentAttached()
          ? safeText(certification.documentFileName()).trim()
          : "";
      String documentContentType = certification.documentAttached()
          ? safeText(certification.documentContentType()).trim()
          : "";
      Long documentSizeBytes = certification.documentAttached()
          ? certification.documentSizeBytes()
          : null;
      byte[] documentBytes = null;
      Instant documentUploadedAt = null;

      if (certification.documentAttached()) {
        MultipartFile uploadedFile = uploadedCertificateFiles != null && uploadIndex < uploadedCertificateFiles.size()
            ? uploadedCertificateFiles.get(uploadIndex)
            : null;
        if (uploadedFile != null && !uploadedFile.isEmpty()) {
          uploadIndex++;
          documentFileName = safeText(uploadedFile.getOriginalFilename()).trim();
          documentContentType = safeText(uploadedFile.getContentType()).trim();
          if (documentContentType.isBlank()) {
            documentContentType = "application/octet-stream";
          }
          documentSizeBytes = uploadedFile.getSize();
          documentBytes = readMultipartBytes(uploadedFile, "certificateFiles");
          documentUploadedAt = Instant.now();
        } else if (existingDocument != null) {
          documentFileName = safeText(existingDocument.getDocumentFileName());
          documentContentType = safeText(existingDocument.getDocumentContentType());
          documentSizeBytes = existingDocument.getDocumentSizeBytes();
          documentBytes = existingDocument.getDocumentBytes();
          documentUploadedAt = existingDocument.getDocumentUploadedAt();
        }
      }

      mapped.add(new CandidateProfileCertificationEntity(
          null,
          index + 1,
          safeText(certification.name()).trim(),
          documentFileName,
          documentContentType,
          documentSizeBytes,
          documentBytes,
          documentUploadedAt));
    }
    return mapped;
  }

  public List<CandidateProfileRoleEntity> toCandidateRoleEntities(
      List<CandidateCvWebContract.CandidateRoleWorkingCopyView> roles,
      List<CandidateProfileRoleEntity> existingRoles) {

    if (roles == null || roles.isEmpty()) {
      return List.of();
    }

    List<CandidateProfileRoleEntity> sortedExistingRoles = existingRoles == null
        ? List.of()
        : existingRoles.stream()
            .sorted(Comparator.comparing(CandidateProfileRoleEntity::getRoleNumber))
            .toList();

    return java.util.stream.IntStream.range(0, roles.size())
        .mapToObj(index -> {
          CandidateCvWebContract.CandidateRoleWorkingCopyView role = roles.get(index);
          CandidateProfileRoleEntity existing = index < sortedExistingRoles.size() ? sortedExistingRoles.get(index) : null;
          requireCandRole(role);
          Short roleExperienceYears = toShort(
              Math.max(0, role.roleExperienceYears()),
              "candidateRoles.roleExperienceYears");
          if (existing != null) {
            existing.setRoleNumber(index + 1);
            existing.setRoleId(role.roleId());
            existing.setRoleTitle(safeText(role.roleTitle()).trim());
            existing.setRoleExperienceYears(roleExperienceYears);
            return existing;
          }
          return new CandidateProfileRoleEntity(
              null,
              index + 1,
              role.roleId(),
              safeText(role.roleTitle()).trim(),
              roleExperienceYears);
        })
        .toList();
  }

  public List<CandidateProfilePrimarySkillEntity> toPrimarySkillEntities(
      List<CandidateCvWebContract.CandidateSkillWorkingCopyView> skills,
      List<CandidateProfilePrimarySkillEntity> existingSkills) {

    List<CandidateCvWebContract.CandidateSkillWorkingCopyView> primarySkills = filterSkills(
        skills,
        SkillCatalogLookup.CATEGORY_PRIMARY);
    if (primarySkills.isEmpty()) {
      return List.of();
    }

    List<CandidateProfilePrimarySkillEntity> sortedExistingSkills = existingSkills == null
        ? List.of()
        : existingSkills.stream()
            .sorted(Comparator.comparing(CandidateProfilePrimarySkillEntity::getSkillNumber))
            .toList();

    return java.util.stream.IntStream.range(0, primarySkills.size())
        .mapToObj(index -> {
          CandidateCvWebContract.CandidateSkillWorkingCopyView skill = primarySkills.get(index);
          CandidateProfilePrimarySkillEntity existing = index < sortedExistingSkills.size() ? sortedExistingSkills.get(index)
              : null;
          requireCandSkill(skill);
          if (existing != null) {
            existing.setSkillNumber(index + 1);
            existing.setSkillId(skill.skillId());
            existing.setSkillTitle(safeText(skill.skillTitle()).trim());
            existing.setSkillLevelId(skill.skillLevelId());
            existing.setSkillLevelName(safeText(skill.skillLevelName()).trim());
            return existing;
          }
          return new CandidateProfilePrimarySkillEntity(
              null,
              index + 1,
              skill.skillId(),
              safeText(skill.skillTitle()).trim(),
              skill.skillLevelId(),
              safeText(skill.skillLevelName()).trim());
        })
        .toList();
  }

  public List<CandidateProfileSecondarySkillEntity> toSecondarySkillEntities(
      List<CandidateCvWebContract.CandidateSkillWorkingCopyView> skills,
      List<CandidateProfileSecondarySkillEntity> existingSkills) {

    List<CandidateCvWebContract.CandidateSkillWorkingCopyView> secondarySkills = filterSkills(
        skills,
        SkillCatalogLookup.CATEGORY_SECONDARY);
    if (secondarySkills.isEmpty()) {
      return List.of();
    }

    List<CandidateProfileSecondarySkillEntity> sortedExistingSkills = existingSkills == null
        ? List.of()
        : existingSkills.stream()
            .sorted(Comparator.comparing(CandidateProfileSecondarySkillEntity::getSkillNumber))
            .toList();

    return java.util.stream.IntStream.range(0, secondarySkills.size())
        .mapToObj(index -> {
          CandidateCvWebContract.CandidateSkillWorkingCopyView skill = secondarySkills.get(index);
          CandidateProfileSecondarySkillEntity existing = index < sortedExistingSkills.size()
              ? sortedExistingSkills.get(index)
              : null;
          requireCandSkill(skill);
          if (existing != null) {
            existing.setSkillNumber(index + 1);
            existing.setSkillId(skill.skillId());
            existing.setSkillTitle(safeText(skill.skillTitle()).trim());
            existing.setSkillLevelId(skill.skillLevelId());
            existing.setSkillLevelName(safeText(skill.skillLevelName()).trim());
            return existing;
          }
          return new CandidateProfileSecondarySkillEntity(
              null,
              index + 1,
              skill.skillId(),
              safeText(skill.skillTitle()).trim(),
              skill.skillLevelId(),
              safeText(skill.skillLevelName()).trim());
        })
        .toList();
  }

  private void requireCandRole(CandidateCvWebContract.CandidateRoleWorkingCopyView role) {
    if (role == null) {
      throw reject("candidateRoles must not contain null entries");
    }
    if (role.roleId() <= 0) {
      throw reject("candidateRoles.roleId is required");
    }
    if (safeText(role.roleTitle()).trim().isBlank()) {
      throw reject("candidateRoles.roleTitle is required");
    }
    if (role.roleExperienceYears() < 0) {
      throw reject("candidateRoles.roleExperienceYears must be zero or positive");
    }
    if (role.roleExperienceYears() > MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS) {
      throw reject("candidateRoles.roleExperienceYears must be 100 or less");
    }
  }

  private void requireCandSkill(CandidateCvWebContract.CandidateSkillWorkingCopyView skill) {
    if (skill == null) {
      throw reject("candidateSkills must not contain null entries");
    }
    if (skill.skillId() <= 0) {
      throw reject("candidateSkills.skillId is required");
    }
    if (safeText(skill.skillTitle()).trim().isBlank()) {
      throw reject("candidateSkills.skillTitle is required");
    }
    if (skill.skillLevelId() <= 0) {
      throw reject("candidateSkills.skillLevelId is required");
    }
    if (safeText(skill.skillLevelName()).trim().isBlank()) {
      throw reject("candidateSkills.skillLevelName is required");
    }
    String skillCategory = safeText(skill.skillCategory()).trim();
    if (!SkillCatalogLookup.CATEGORY_PRIMARY.equals(skillCategory)
        && !SkillCatalogLookup.CATEGORY_SECONDARY.equals(skillCategory)) {
      throw reject("candidateSkills.skillCategory must be PRIMARY or SECONDARY");
    }
  }

  public void syncCandidateRoles(
      CandidateProfileEntity entity,
      List<CandidateCvWebContract.CandidateRoleWorkingCopyView> roles) {

    entity.replaceCandidateRoles(toCandidateRoleEntities(roles, entity.getCandidateRoles()));
  }

  public void syncCandSkills(
      CandidateProfileEntity entity,
      List<CandidateCvWebContract.CandidateSkillWorkingCopyView> skills) {

    entity.replacePrimarySkills(toPrimarySkillEntities(skills, entity.getPrimarySkills()));
    entity.replaceSecondarySkills(toSecondarySkillEntities(skills, entity.getSecondarySkills()));
  }

  private List<CandidateCvWebContract.CandidateSkillWorkingCopyView> filterSkills(
      List<CandidateCvWebContract.CandidateSkillWorkingCopyView> skills,
      String category) {

    if (skills == null || skills.isEmpty()) {
      return List.of();
    }
    return skills.stream()
        .filter(skill -> skill != null && category.equals(safeText(skill.skillCategory()).trim()))
        .toList();
  }

  public void syncWorkExperiences(
      CandidateProfileEntity entity,
      List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> workExperiences) {

    List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> targetWorkExperiences = workExperiences == null
        ? List.of()
        : workExperiences.stream()
            .filter(this::hasWorkExperienceData)
            .toList();
    List<CandidateProfileWorkExperienceEntity> existingWorkExperiences = entity.getWorkExperiences().stream()
        .sorted(Comparator.comparing(CandidateProfileWorkExperienceEntity::getWorkExperienceNumber))
        .toList();

    for (int index = 0; index < targetWorkExperiences.size(); index++) {
      CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView workExperience = targetWorkExperiences.get(index);
      if (index < existingWorkExperiences.size()) {
        updateWorkExperience(existingWorkExperiences.get(index), index + 1, workExperience);
      } else {
        entity.addWorkExperience(new CandidateProfileWorkExperienceEntity(
            null,
            index + 1,
            safeText(workExperience.jobTitle()).trim(),
            safeText(workExperience.workExpCompany()).trim(),
            safeText(workExperience.workExpCompanyOrgNr()).trim(),
            safeText(workExperience.city()).trim(),
            safeText(workExperience.country()).trim(),
            parseOptionalDate(workExperience.startDate(), "workExperiences.startDate"),
            parseOptionalDate(workExperience.endDate(), "workExperiences.endDate"),
            workExperience.currentlyHere()));
      }
    }

    for (int index = existingWorkExperiences.size() - 1; index >= targetWorkExperiences.size(); index--) {
      entity.getWorkExperiences().remove(existingWorkExperiences.get(index));
    }
  }

  public void syncEducations(
      CandidateProfileEntity entity,
      List<CandidateCvWebContract.CandidateEducationWorkingCopyView> educations) {

    List<CandidateCvWebContract.CandidateEducationWorkingCopyView> targetEducations = educations == null
        ? List.of()
        : educations.stream()
            .filter(this::hasEducationData)
            .toList();
    List<CandidateProfileEducationEntity> existingEducations = entity.getEducations().stream()
        .sorted(Comparator.comparing(CandidateProfileEducationEntity::getEducationNumber))
        .toList();

    for (int index = 0; index < targetEducations.size(); index++) {
      CandidateCvWebContract.CandidateEducationWorkingCopyView education = targetEducations.get(index);
      if (index < existingEducations.size()) {
        updateEducation(existingEducations.get(index), index + 1, education);
      } else {
        entity.addEducation(toEducationEntity(index + 1, education));
      }
    }

    for (int index = existingEducations.size() - 1; index >= targetEducations.size(); index--) {
      entity.getEducations().remove(existingEducations.get(index));
    }
  }

  public void syncCertifications(
      CandidateProfileEntity entity,
      List<CandidateCvWebContract.CandidateCertificationWorkingCopyView> certifications) {

    List<CandidateCvWebContract.CandidateCertificationWorkingCopyView> targetCertifications = certifications == null
        ? List.of()
        : certifications.stream()
            .filter(this::hasCertificationData)
            .toList();
    List<CandidateProfileCertificationEntity> existingCertifications = entity.getCertifications().stream()
        .sorted(Comparator.comparing(CandidateProfileCertificationEntity::getCertificationNumber))
        .toList();

    for (int index = 0; index < targetCertifications.size(); index++) {
      CandidateCvWebContract.CandidateCertificationWorkingCopyView certification = targetCertifications.get(index);
      if (index < existingCertifications.size()) {
        updateCertification(existingCertifications.get(index), index + 1, certification);
      } else {
        entity.addCertification(toCertificationEntity(index + 1, certification, null));
      }
    }

    for (int index = existingCertifications.size() - 1; index >= targetCertifications.size(); index--) {
      entity.getCertifications().remove(existingCertifications.get(index));
    }
  }

  private void updateWorkExperience(
      CandidateProfileWorkExperienceEntity entity,
      int workExperienceNumber,
      CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView workExperience) {

    entity.setWorkExperienceNumber(workExperienceNumber);
    entity.setJobTitle(safeText(workExperience.jobTitle()).trim());
    entity.setWorkExpCompany(safeText(workExperience.workExpCompany()).trim());
    entity.setWorkExpCompanyOrgNr(safeText(workExperience.workExpCompanyOrgNr()).trim());
    entity.setCity(safeText(workExperience.city()).trim());
    entity.setCountry(safeText(workExperience.country()).trim());
    entity.setStartDate(parseOptionalDate(workExperience.startDate(), "workExperiences.startDate"));
    entity.setEndDate(parseOptionalDate(workExperience.endDate(), "workExperiences.endDate"));
    entity.setCurrentlyHere(workExperience.currentlyHere());
  }

  private void updateCertification(
      CandidateProfileCertificationEntity entity,
      int certificationNumber,
      CandidateCvWebContract.CandidateCertificationWorkingCopyView certification) {

    CandidateProfileCertificationEntity updated = toCertificationEntity(certificationNumber, certification, entity);
    entity.setCertificationNumber(updated.getCertificationNumber());
    entity.setCertificationName(updated.getCertificationName());
    entity.setDocumentFileName(updated.getDocumentFileName());
    entity.setDocumentContentType(updated.getDocumentContentType());
    entity.setDocumentSizeBytes(updated.getDocumentSizeBytes());
    entity.setDocumentBytes(updated.getDocumentBytes());
    entity.setDocumentUploadedAt(updated.getDocumentUploadedAt());
  }

  private void updateEducation(
      CandidateProfileEducationEntity entity,
      int educationNumber,
      CandidateCvWebContract.CandidateEducationWorkingCopyView education) {

    CandidateProfileEducationEntity updated = toEducationEntity(educationNumber, education);
    entity.setEducationNumber(updated.getEducationNumber());
    entity.setInstitution(updated.getInstitution());
    entity.setFieldOfStudy(updated.getFieldOfStudy());
    entity.setStartDate(updated.getStartDate());
    entity.setEndDate(updated.getEndDate());
    entity.setCurrentlyStudying(updated.getCurrentlyStudying());
  }

  private CandidateProfileEducationEntity toEducationEntity(
      int educationNumber,
      CandidateCvWebContract.CandidateEducationWorkingCopyView education) {

    return new CandidateProfileEducationEntity(
        null,
        educationNumber,
        safeText(education.institution()).trim(),
        safeText(education.fieldOfStudy()).trim(),
        parseOptionalDate(education.startDate(), "educations.startDate"),
        parseOptionalDate(education.endDate(), "educations.endDate"),
        education.currentlyStudying());
  }

  private CandidateProfileCertificationEntity toCertificationEntity(
      int certificationNumber,
      CandidateCvWebContract.CandidateCertificationWorkingCopyView certification,
      CandidateProfileCertificationEntity existing) {

    CandidateProfileCertificationEntity existingDocument = null;
    if (certification.documentAttached()) {
      existingDocument = existing;
    }
    String documentFileName = certification.documentAttached()
        ? safeText(certification.documentFileName()).trim()
        : "";
    String documentContentType = certification.documentAttached()
        ? safeText(certification.documentContentType()).trim()
        : "";
    Long documentSizeBytes = certification.documentAttached()
        ? certification.documentSizeBytes()
        : null;
    byte[] documentBytes = null;
    Instant documentUploadedAt = null;

    if (existingDocument != null) {
      if (documentFileName.isBlank()) {
        documentFileName = safeText(existingDocument.getDocumentFileName());
      }
      if (documentContentType.isBlank()) {
        documentContentType = safeText(existingDocument.getDocumentContentType());
      }
      documentSizeBytes = documentSizeBytes == null ? existingDocument.getDocumentSizeBytes() : documentSizeBytes;
      documentBytes = existingDocument.getDocumentBytes();
      documentUploadedAt = existingDocument.getDocumentUploadedAt();
    }

    return new CandidateProfileCertificationEntity(
        null,
        certificationNumber,
        safeText(certification.name()).trim(),
        documentFileName,
        documentContentType,
        documentSizeBytes,
        documentBytes,
        documentUploadedAt);
  }

  private boolean hasWorkExperienceData(
      CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView workExperience) {

    return workExperience != null
        && (workExperience.currentlyHere()
            || !safeText(workExperience.jobTitle()).trim().isBlank()
            || !safeText(workExperience.workExpCompany()).trim().isBlank()
            || !safeText(workExperience.workExpCompanyOrgNr()).trim().isBlank()
            || !safeText(workExperience.city()).trim().isBlank()
            || !safeText(workExperience.country()).trim().isBlank()
            || !safeText(workExperience.startDate()).trim().isBlank()
            || !safeText(workExperience.endDate()).trim().isBlank());
  }

  private boolean hasEducationData(
      CandidateCvWebContract.CandidateEducationWorkingCopyView education) {

    return education != null
        && (education.currentlyStudying()
            || !safeText(education.institution()).trim().isBlank()
            || !safeText(education.fieldOfStudy()).trim().isBlank()
            || !safeText(education.startDate()).trim().isBlank()
            || !safeText(education.endDate()).trim().isBlank());
  }

  private boolean hasCertificationData(
      CandidateCvWebContract.CandidateCertificationWorkingCopyView certification) {

    return certification != null
        && (certification.documentAttached()
            || !safeText(certification.name()).trim().isBlank()
            || !safeText(certification.documentFileName()).trim().isBlank()
            || !safeText(certification.documentContentType()).trim().isBlank()
            || certification.documentSizeBytes() != null);
  }

  private byte[] readMultipartBytes(MultipartFile file, String fieldName) {
    try {
      return file.getBytes();
    } catch (IOException exception) {
      throw reject(fieldName + " could not be read");
    }
  }
}
