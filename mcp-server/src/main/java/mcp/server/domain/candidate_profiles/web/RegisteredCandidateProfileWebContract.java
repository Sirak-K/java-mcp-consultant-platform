package mcp.server.domain.candidate_profiles.web;

import java.util.List;

public final class RegisteredCandidateProfileWebContract {

  private RegisteredCandidateProfileWebContract() {
  }

  public record RegisteredCandidateProfileEditInput(
      String contactEmail,
      String cvFileName,
      String cvContentType,
      Long cvSizeBytes,
      CandidateCvWebContract.CandidateCvProfileWorkingCopyInput profileWorkingCopy,
      CandidateApplicationWebContract.CandidateProfileSummaryInput generatedSummary,
      String outcome) {
  }

  public record RegisteredCandidateProfileCardSkillView(
      String title,
      String skillLevel) {
  }

  public record RegisteredCandidateProfileCardView(
      long candidateProfileId,
      String candidateName,
      String roleTitle,
      String roleExperienceLevel,
      Integer roleExperienceYears,
      String availabilityStatus,
      String country,
      String locationFlexibility,
      String workMode,
      List<RegisteredCandidateProfileCardSkillView> primarySkills,
      List<RegisteredCandidateProfileCardSkillView> secondarySkills) {
  }
}
