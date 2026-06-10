package mcp.server.domain.candidate_presentation.application.evidence;

import java.util.List;

import mcp.server.domain.matching.api.MatchScoreBreakdownFactorView;

public record CandidatePresentationEvidenceView(
    MatchContext matchContext,
    MissionContext missionContext,
    CandidateContext candidateContext,
    SkillEvidence skillEvidence,
    ExperienceEvidence experienceEvidence,
    InternalEvidenceTrace internalEvidenceTrace) {

  public record MatchContext(
      long matchId,
      String scoreLabel,
      boolean roleMatched,
      boolean workModeMatched,
      int matchedSkillCount,
      int requiredSkillCount,
      String matchedAt) {
  }

  public record MissionContext(
      long missionId,
      String customerName,
      String missionTitle,
      long missionSlotId,
      int missionSlotNumber,
      String missionSlotRoleTitle,
      int requiredRoleExperienceYears,
      String missionWorkMode,
      String missionWorkModeDisplay,
      String startDate,
      String endDate) {
  }

  public record CandidateContext(
      long candidateProfileId,
      String candidateName,
      String roleTitle,
      String roleExperienceLevel,
      Integer roleExperienceYears,
      String availabilityStatus,
      String workStatus,
      String workMode,
      String workModeDisplay,
      String city,
      String country,
      String locationFlexibility,
      String profileSummary,
      String yearsOfExperience,
      String generatedSummaryStatus,
      String generatedSummaryCoreCompetenceOverview,
      String generatedSummaryLocation,
      String generatedSummaryOtherDetails) {
  }

  public record SkillEvidence(
      List<RequiredSkillEvidence> requiredSkills,
      List<CandidateSkillEvidence> candidatePrimarySkills,
      List<CandidateSkillEvidence> candidateSecondarySkills,
      List<String> matchedSkills,
      List<String> matchedPrimarySkills,
      List<String> matchedSecondarySkills) {
  }

  public record RequiredSkillEvidence(
      long skillId,
      String skillTitle,
      short skillLevelId,
      String skillLevelName,
      String skillCategory) {
  }

  public record CandidateSkillEvidence(
      Long skillId,
      String skillTitle,
      Short skillLevelId,
      String skillLevelName,
      String skillCategory) {
  }

  public record ExperienceEvidence(
      List<WorkExperienceEvidence> workExperiences,
      List<EducationEvidence> educations,
      List<CertificationEvidence> certifications) {
  }

  public record WorkExperienceEvidence(
      Integer workExperienceNumber,
      String jobTitle,
      String workExpCompany,
      String workExpCompanyOrgNr,
      String city,
      String country,
      String startDate,
      String endDate,
      Boolean currentlyHere) {
  }

  public record EducationEvidence(
      Integer educationNumber,
      String institution,
      String fieldOfStudy,
      String startDate,
      String endDate,
      Boolean currentlyStudying) {
  }

  public record CertificationEvidence(
      Integer certificationNumber,
      String certificationName) {
  }

  public record InternalEvidenceTrace(
      int score,
      int discoveryThreshold,
      boolean passedDiscoveryThreshold,
      String decision,
      List<MatchScoreBreakdownFactorView> matchFactors,
      List<String> missingOrWeakFactors) {
  }
}
