package mcp.server.domain.candidate_profiles.web;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public final class CandidateCvWebContract {

  private CandidateCvWebContract() {
  }

  public record CandidateCvExtractionView(
      String status,
      String extractedTextPreview,
      String error,
      String extractedAt) {
  }

  public record CandidateCompanyIdentityOptionView(
      String organisationName,
      String organisationNumber,
      String organisationCity) {
  }

  public record CandidateWorkExperienceWorkingCopyView(
      String jobTitle,
      @JsonAlias("company") String workExpCompany,
      String workExpCompanyOrgNr,
      List<CandidateCompanyIdentityOptionView> companyIdentityOptions,
      String city,
      String country,
      String startDate,
      String endDate,
      boolean currentlyHere) {
  }

  public record CandidateWorkExperienceWorkingCopyInput(
      String jobTitle,
      @JsonAlias("company") String workExpCompany,
      String workExpCompanyOrgNr,
      List<CandidateCompanyIdentityOptionView> companyIdentityOptions,
      String city,
      String country,
      String startDate,
      String endDate,
      boolean currentlyHere) {
  }

  public record CandidateCertificationWorkingCopyInput(
      String name,
      boolean documentAttached,
      String documentFileName,
      String documentContentType,
      Long documentSizeBytes) {
  }

  public record CandidateEducationWorkingCopyInput(
      String institution,
      String fieldOfStudy,
      String startDate,
      String endDate,
      boolean currentlyStudying) {
  }

  public record CandidateRoleWorkingCopyInput(
      long roleId,
      String roleTitle,
      int roleExperienceYears) {
  }

  public record CandidateSkillWorkingCopyInput(
      long skillId,
      String skillTitle,
      String skillCategory,
      short skillLevelId,
      String skillLevelName) {
  }

  public record CandidateCvProfileWorkingCopyInput(
      String contactEmail,
      String firstName,
      String lastName,
      String phoneNumber,
      String country,
      String city,
      String workStatus,
      String languages,
      String roleTitle,
      List<CandidateRoleWorkingCopyInput> candidateRoles,
      String profileSummary,
      String yearsOfExperience,
      String expectedSalary,
      String hourlyRate,
      String skills,
      List<CandidateSkillWorkingCopyInput> candidateSkills,
      List<CandidateWorkExperienceWorkingCopyInput> workExperiences,
      String workMode,
      String locationFlexibility,
      String preferredLocation,
      boolean willingToRelocate,
      List<CandidateEducationWorkingCopyInput> educations,
      List<CandidateCertificationWorkingCopyInput> certifications,
      boolean gdprConsent) {
  }

  public record CandidateCertificationWorkingCopyView(
      String name,
      boolean documentAttached,
      String documentFileName,
      String documentContentType,
      Long documentSizeBytes) {
  }

  public record CandidateEducationWorkingCopyView(
      String institution,
      String fieldOfStudy,
      String startDate,
      String endDate,
      boolean currentlyStudying) {
  }

  public record CandidateRoleWorkingCopyView(
      long roleId,
      String roleTitle,
      int roleExperienceYears) {
  }

  public record CandidateSkillWorkingCopyView(
      long skillId,
      String skillTitle,
      String skillCategory,
      short skillLevelId,
      String skillLevelName) {
  }

  public record CandidateCvProfileWorkingCopyView(
      String contactEmail,
      String firstName,
      String lastName,
      String phoneNumber,
      String country,
      String city,
      String workStatus,
      String languages,
      String roleTitle,
      List<CandidateRoleWorkingCopyView> candidateRoles,
      String profileSummary,
      String yearsOfExperience,
      String expectedSalary,
      String hourlyRate,
      String skills,
      List<CandidateSkillWorkingCopyView> candidateSkills,
      List<CandidateWorkExperienceWorkingCopyView> workExperiences,
      String workMode,
      String locationFlexibility,
      String preferredLocation,
      boolean willingToRelocate,
      List<CandidateEducationWorkingCopyView> educations,
      List<CandidateCertificationWorkingCopyView> certifications,
      boolean gdprConsent) {
  }

  public record CandidateCvPreviewView(
      CandidateCvExtractionView cvExtraction,
      CandidateCvProfileWorkingCopyView profileWorkingCopy) {
  }
}
