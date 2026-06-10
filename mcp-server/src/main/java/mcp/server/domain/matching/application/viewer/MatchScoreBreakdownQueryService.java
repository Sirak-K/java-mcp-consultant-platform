package mcp.server.domain.matching.application.viewer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.matching.api.MatchScoreBreakdownFactorView;
import mcp.server.domain.matching.api.MatchScoreBreakdownView;
import mcp.server.domain.matching.exception.CandidateToSlotMatchNotFoundException;
import mcp.server.domain.matching.model.CandidateToMissionSlotMatchScorer;
import mcp.server.domain.matching.persistence.CandidateToSlotMatchEntity;
import mcp.server.domain.matching.persistence.CandidateToSlotMatchJpaRepository;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.missions.application.MissionSpecification;
import mcp.server.domain.missions.application.RegisteredMissionQuery;

@Service
public class MatchScoreBreakdownQueryService {

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

  private final CandidateToSlotMatchJpaRepository matchRepo;
  private final MissionQueryService missionQueryService;

  public MatchScoreBreakdownQueryService(
      CandidateToSlotMatchJpaRepository matchRepo,
      MissionQueryService missionQueryService) {
    this.matchRepo = matchRepo;
    this.missionQueryService = missionQueryService;
  }

  @Transactional(readOnly = true)
  public MatchScoreBreakdownView inspectMatchScoreBreakdown(long matchId) {
    CandidateToSlotMatchEntity match = matchRepo.findById(matchId)
        .orElseThrow(() -> new CandidateToSlotMatchNotFoundException(matchId));
    RegisteredMissionQuery.MissionReadView mission = missionQueryService.requireOpenMissionForSlot(
        match.getMissionSlotId());
    RegisteredMissionQuery.MissionSlotReadView slotRead = missionQueryService.requireMissionSlot(
        match.getMissionSlotId());
    MissionSpecification.SpecificationView specification = mission.specification();
    MissionSpecification.SlotSpecificationView slot = slotRead.specification();

    CandidateToMissionSlotMatchScorer.ScoreBreakdown breakdown = CandidateToMissionSlotMatchScorer
        .scoreBreakdownFromSnapshot(
            new CandidateToMissionSlotMatchScorer.PersistedScoreSnapshot(
                safeInt(match.getMatchScore()),
                scoreLabel(match),
                Boolean.TRUE.equals(match.getRoleMatched()),
                Boolean.TRUE.equals(match.getWorkModeMatched()),
                requiredSkillEvidence(slot.requiredSkills()),
                matchedSkills(match),
                List.of(slot.roleTitle() + " / " + slot.requiredRoleExperienceYears() + " years"),
                List.of(specification.workMode())));

    return new MatchScoreBreakdownView(
        match.getId(),
        breakdown.score(),
        breakdown.scoreLabel(),
        breakdown.discoveryThreshold(),
        breakdown.passedDiscoveryThreshold(),
        breakdown.decision(),
        breakdown.factors().stream()
            .map(MatchScoreBreakdownQueryService::factor)
            .toList(),
        breakdown.matchedSkillTitles(),
        breakdown.missingOrWeakFactors(),
        formatInstant(match.getCreatedAt()));
  }

  @Transactional(readOnly = true)
  public MatchScoreBreakdownView inspectScoreBreakdown(long matchId) {
    return inspectMatchScoreBreakdown(matchId);
  }

  private static List<String> matchedSkills(CandidateToSlotMatchEntity match) {
    String titles = match.getMatchedSkillTitles();
    if (titles == null || titles.isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(titles.split("\\|"))
        .map(String::trim)
        .filter(title -> !title.isBlank())
        .toList();
  }

  private static List<CandidateToMissionSlotMatchScorer.RequiredSkillEvidence> requiredSkillEvidence(
      List<MissionSpecification.SkillRequirementView> requiredSkills) {
    return requiredSkills.stream()
        .map(skill -> new CandidateToMissionSlotMatchScorer.RequiredSkillEvidence(
            skill.skillTitle(),
            skill.skillCategory(),
            skill.skillLevelId(),
            skill.skillLevelName()))
        .toList();
  }

  private static MatchScoreBreakdownFactorView factor(
      CandidateToMissionSlotMatchScorer.ScoreFactor factor) {
    return new MatchScoreBreakdownFactorView(
        factor.factor(),
        factor.matched(),
        factor.matchedCount(),
        factor.requiredCount(),
        factor.scorePerInstance(),
        factor.points(),
        factor.evidence(),
        factor.note());
  }

  private static int safeInt(Integer value) {
    return value == null ? 0 : value;
  }

  private static String scoreLabel(CandidateToSlotMatchEntity match) {
    return CandidateToMissionSlotMatchScorer.scoreLabel(safeInt(match.getMatchScore()));
  }

  private static String formatInstant(Instant instant) {
    return instant == null
        ? null
        : TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
  }
}
