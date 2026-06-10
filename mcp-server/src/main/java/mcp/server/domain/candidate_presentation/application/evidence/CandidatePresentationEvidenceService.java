package mcp.server.domain.candidate_presentation.application.evidence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import mcp.server.domain.missions.application.MissionSpecification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.candidate_profiles.api.CandidateProfileEvidence;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.candidate_profiles.api.CandidateProfileSkillView;
import mcp.server.domain.matching.api.CandidateToSlotMatchEvidence;
import mcp.server.domain.matching.api.CandidateToSlotMatchQuery;
import mcp.server.domain.matching.api.MatchScoreBreakdownView;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.missions.application.RegisteredMissionQuery;

import mcp.server.domain.candidate_presentation.exception.CandidatePresentationException;

@Service
public class CandidatePresentationEvidenceService {

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

  private final CandidateToSlotMatchQuery matchQuery;
  private final MissionQueryService missionQueryService;
  private final CandidateProfileQuery candidateProfileQuery;

  public CandidatePresentationEvidenceService(
      CandidateToSlotMatchQuery matchQuery,
      MissionQueryService missionQueryService,
      CandidateProfileQuery candidateProfileQuery) {
    this.matchQuery = Objects.requireNonNull(matchQuery, "matchQuery");
    this.missionQueryService = Objects.requireNonNull(missionQueryService, "missionQueryService");
    this.candidateProfileQuery = Objects.requireNonNull(candidateProfileQuery, "candidateProfileQuery");
  }

  @Transactional(readOnly = true)
  public CandidatePresentationEvidenceView collectEvidence(long matchId) {
    CandidateToSlotMatchEvidence match = matchQuery.findMatchEvidence(matchId)
        .orElseThrow(() -> notFound("candToSlotMatch not found"));
    RegisteredMissionQuery.MissionSlotReadView missionSlot = missionQueryService
        .requireMissionSlot(match.missionSlotId());
    RegisteredMissionQuery.MissionReadView mission = missionQueryService
        .requireMissionForSlot(match.missionSlotId());
    CandidateProfileEvidence candidateProfile = candidateProfileQuery.findEvidenceProfile(match.candidateProfileId())
        .orElseThrow(() -> notFound("candidateProfile not found"));

    MissionSpecification.SpecificationView missionSpecification = mission.specification();
    MissionSpecification.SlotSpecificationView slotSpecification = missionSlot.specification();

    MatchScoreBreakdownView scoreBreakdown = matchQuery.inspectScoreBreakdown(matchId);

    return new CandidatePresentationEvidenceView(
        matchContext(match, slotSpecification.requiredSkills().size()),
        missionContext(mission, missionSpecification, missionSlot, slotSpecification),
        candContext(candidateProfile),
        skillEvidence(slotSpecification, candidateProfile, match),
        experienceEvidence(candidateProfile),
        internalEvidenceTrace(scoreBreakdown));
  }

  private static CandidatePresentationEvidenceView.MatchContext matchContext(
      CandidateToSlotMatchEvidence match,
      int requiredSkillCount) {
    return new CandidatePresentationEvidenceView.MatchContext(
        match.matchId(),
        safeText(match.scoreLabel()),
        match.roleMatched(),
        match.workModeMatched(),
        match.matchedSkillCount(),
        requiredSkillCount,
        formatInstant(match.matchedAt()));
  }

  private static CandidatePresentationEvidenceView.MissionContext missionContext(
      RegisteredMissionQuery.MissionReadView mission,
      MissionSpecification.SpecificationView missionSpecification,
      RegisteredMissionQuery.MissionSlotReadView missionSlot,
      MissionSpecification.SlotSpecificationView slotSpecification) {
    return new CandidatePresentationEvidenceView.MissionContext(
        mission.missionId(),
        safeText(missionSpecification.customerName()),
        safeText(missionSpecification.missionTitle()),
        missionSlot.missionSlotId(),
        missionSlot.missionSlotNumber(),
        safeText(slotSpecification.roleTitle()),
        slotSpecification.requiredRoleExperienceYears(),
        safeText(missionSpecification.workMode()),
        workModeDisplay(missionSpecification.workMode()),
        safeText(missionSpecification.startDate()),
        safeText(missionSpecification.endDate()));
  }

  private static CandidatePresentationEvidenceView.CandidateContext candContext(
      CandidateProfileEvidence candidateProfile) {
    CandidateProfileEvidence.GeneratedSummary generatedSummary = candidateProfile.generatedSummary();
    return new CandidatePresentationEvidenceView.CandidateContext(
        candidateProfile.candidateProfileId(),
        safeText(candidateProfile.displayName()),
        safeText(candidateProfile.primaryRoleTitle()),
        safeText(candidateProfile.primaryRoleExperienceLevel()),
        candidateProfile.primaryRoleExperienceYears(),
        safeText(candidateProfile.workStatus()),
        safeText(candidateProfile.workStatus()),
        safeText(candidateProfile.workMode()),
        workModeDisplay(candidateProfile.workMode()),
        safeText(candidateProfile.city()),
        safeText(candidateProfile.country()),
        safeText(candidateProfile.locationFlexibility()),
        safeText(candidateProfile.profileSummary()),
        safeText(candidateProfile.yearsOfExperience()),
        safeText(generatedSummary.status()),
        safeText(generatedSummary.coreCompetenceOverview()),
        safeText(generatedSummary.location()),
        safeText(generatedSummary.otherDetails()));
  }

  private static CandidatePresentationEvidenceView.SkillEvidence skillEvidence(
      MissionSpecification.SlotSpecificationView slotSpecification,
      CandidateProfileEvidence candidateProfile,
      CandidateToSlotMatchEvidence match) {
    List<String> matchedSkills = matchedSkills(match);
    return new CandidatePresentationEvidenceView.SkillEvidence(
        slotSpecification.requiredSkills().stream()
            .map(CandidatePresentationEvidenceService::requiredSkillEvidence)
            .toList(),
        candidateProfile.primarySkills().stream()
            .map(CandidatePresentationEvidenceService::primarySkillEvidence)
            .toList(),
        candidateProfile.secondarySkills().stream()
            .map(CandidatePresentationEvidenceService::secondarySkillEvidence)
            .toList(),
        matchedSkills,
        matchedRequiredSkills(slotSpecification, matchedSkills, "PRIMARY"),
        matchedRequiredSkills(slotSpecification, matchedSkills, "SECONDARY"));
  }

  private static CandidatePresentationEvidenceView.RequiredSkillEvidence requiredSkillEvidence(
      MissionSpecification.SkillRequirementView skill) {
    return new CandidatePresentationEvidenceView.RequiredSkillEvidence(
        skill.skillId(),
        safeText(skill.skillTitle()),
        skill.skillLevelId(),
        safeText(skill.skillLevelName()),
        safeText(skill.skillCategory()));
  }

  private static CandidatePresentationEvidenceView.CandidateSkillEvidence primarySkillEvidence(
      CandidateProfileSkillView skill) {
    return new CandidatePresentationEvidenceView.CandidateSkillEvidence(
        skill.skillId(),
        safeText(skill.skillTitle()),
        skill.skillLevelId(),
        safeText(skill.skillLevelName()),
        safeText(skill.skillCategory()));
  }

  private static CandidatePresentationEvidenceView.CandidateSkillEvidence secondarySkillEvidence(
      CandidateProfileSkillView skill) {
    return new CandidatePresentationEvidenceView.CandidateSkillEvidence(
        skill.skillId(),
        safeText(skill.skillTitle()),
        skill.skillLevelId(),
        safeText(skill.skillLevelName()),
        safeText(skill.skillCategory()));
  }

  private static CandidatePresentationEvidenceView.ExperienceEvidence experienceEvidence(CandidateProfileEvidence candProfile) {
    return new CandidatePresentationEvidenceView.ExperienceEvidence(
        candProfile.workExperiences().stream()
            .map(CandidatePresentationEvidenceService::workExperienceEvidence)
            .toList(),
        candProfile.educations().stream()
            .map(CandidatePresentationEvidenceService::educationEvidence)
            .toList(),
        candProfile.certifications().stream()
            .map(CandidatePresentationEvidenceService::certificationEvidence)
            .toList());
  }

  private static CandidatePresentationEvidenceView.WorkExperienceEvidence workExperienceEvidence(
      CandidateProfileEvidence.WorkExperience workExperience) {
    return new CandidatePresentationEvidenceView.WorkExperienceEvidence(
        workExperience.order(),
        safeText(workExperience.jobTitle()),
        safeText(workExperience.organizationName()),
        safeText(workExperience.organizationRegistrationNumber()),
        safeText(workExperience.city()),
        safeText(workExperience.country()),
        formatDate(workExperience.startDate()),
        formatDate(workExperience.endDate()),
        workExperience.current());
  }

  private static CandidatePresentationEvidenceView.EducationEvidence educationEvidence(
      CandidateProfileEvidence.Education education) {
    return new CandidatePresentationEvidenceView.EducationEvidence(
        education.order(),
        safeText(education.institution()),
        safeText(education.fieldOfStudy()),
        formatDate(education.startDate()),
        formatDate(education.endDate()),
        education.current());
  }

  private static CandidatePresentationEvidenceView.CertificationEvidence certificationEvidence(
      CandidateProfileEvidence.Certification certification) {
    return new CandidatePresentationEvidenceView.CertificationEvidence(
        certification.order(),
        safeText(certification.certificationName()));
  }

  private static CandidatePresentationEvidenceView.InternalEvidenceTrace internalEvidenceTrace(
      MatchScoreBreakdownView scoreBreakdown) {
    return new CandidatePresentationEvidenceView.InternalEvidenceTrace(
        scoreBreakdown.score(),
        scoreBreakdown.discoveryThreshold(),
        scoreBreakdown.passedDiscoveryThreshold(),
        safeText(scoreBreakdown.decision()),
        scoreBreakdown.factors() == null ? List.of() : scoreBreakdown.factors(),
        scoreBreakdown.missingOrWeakFactors() == null ? List.of() : scoreBreakdown.missingOrWeakFactors());
  }

  private static List<String> matchedSkills(CandidateToSlotMatchEvidence match) {
    return match.matchedSkillTitles().stream()
        .map(CandidatePresentationEvidenceService::safeText)
        .map(String::trim)
        .filter(title -> !title.isBlank())
        .toList();
  }

  private static List<String> matchedRequiredSkills(
      MissionSpecification.SlotSpecificationView slotSpecification,
      List<String> matchedSkillTitles,
      String skillCategory) {
    List<String> normalizedMatchedTitles = matchedSkillTitles.stream()
        .map(CandidatePresentationEvidenceService::normalizeSkillTitle)
        .toList();
    return slotSpecification.requiredSkills().stream()
        .filter(skill -> skillCategory.equalsIgnoreCase(safeText(skill.skillCategory())))
        .filter(skill -> normalizedMatchedTitles.contains(normalizeSkillTitle(skill.skillTitle())))
        .map(skill -> safeText(skill.skillTitle()) + " (" + safeText(skill.skillLevelName()) + ")")
        .toList();
  }

  private static String normalizeSkillTitle(String value) {
    return safeText(value).trim().toLowerCase(java.util.Locale.ROOT);
  }

  private static String workModeDisplay(String value) {
    return switch (safeText(value).trim().toUpperCase(java.util.Locale.ROOT)) {
      case "ON_PREMISE", "ON_SITE" -> "on-premise";
      case "REMOTE" -> "remote";
      case "HYBRID" -> "hybrid";
      default -> safeText(value).trim();
    };
  }

  private static CandidatePresentationException notFound(String message) {
    return CandidatePresentationException.notFound(message);
  }

  private static String formatInstant(Instant instant) {
    return instant == null
        ? null
        : TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
  }

  private static String formatDate(LocalDate date) {
    return date == null ? null : date.toString();
  }

  private static String safeText(String value) {
    return value == null ? "" : value;
  }

}
