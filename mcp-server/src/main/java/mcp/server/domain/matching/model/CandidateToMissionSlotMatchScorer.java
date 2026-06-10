package mcp.server.domain.matching.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CandidateToMissionSlotMatchScorer {

  public static final int QUALIFIED_MATCH_SCORE = 70;

  public static final int ROLE_MATCH_SCORE = 50;
  public static final int SENIOR_SKILL_SCORE = 5;
  public static final int JUNIOR_SKILL_SCORE = 2;
  public static final int WORK_MODE_MATCH_SCORE = 20;
  private static final int SENIOR_SKILL_LEVEL_RANK = 2;

  private CandidateToMissionSlotMatchScorer() {
  }

  public static Result score(
      RequiredRole requiredRole,
      List<CandidateRole> candidateRoles,
      List<RequiredSkill> requiredSkills,
      List<CandidateSkill> candidateSkills,
      String requiredWorkMode,
      String candidateWorkMode) {

    Objects.requireNonNull(requiredRole, "requiredRole");
    List<CandidateRole> safeCandidateRoles = candidateRoles == null ? List.of() : candidateRoles;
    List<RequiredSkill> safeRequiredSkills = requiredSkills == null ? List.of() : requiredSkills;
    List<CandidateSkill> safeCandidateSkills = candidateSkills == null ? List.of() : candidateSkills;

    boolean roleMatched = safeCandidateRoles.stream()
        .anyMatch(role -> role.roleId() == requiredRole.roleId()
            && role.experienceYears() >= requiredRole.requiredExperienceYears());

    Map<SkillKey, Integer> requiredLevelBySkill = highestRequiredLevelBySkill(safeRequiredSkills);
    Map<SkillKey, CandidateSkill> strongestCandidateSkillBySkill = strongestCandidateSkillBySkill(safeCandidateSkills);

    int skillScore = 0;
    Map<SkillKey, CandidateSkill> matchedSkillsBySkill = new LinkedHashMap<>();
    for (Map.Entry<SkillKey, Integer> requiredSkill : requiredLevelBySkill.entrySet()) {
      CandidateSkill candidateSkill = strongestCandidateSkillBySkill.get(requiredSkill.getKey());
      if (candidateSkill == null || candidateSkill.skillLevel() < requiredSkill.getValue()) {
        continue;
      }
      skillScore += scoreForRequiredSkillLevel(requiredSkill.getValue());
      matchedSkillsBySkill.put(requiredSkill.getKey(), candidateSkill);
    }

    List<String> matchedSkillTitles = matchedSkillsBySkill.values().stream()
        .map(CandidateSkill::skillTitle)
        .filter(title -> title != null && !title.isBlank())
        .distinct()
        .sorted()
        .toList();

    boolean workModeMatched = workModeMatches(requiredWorkMode, candidateWorkMode);
    int roleScore = roleMatched ? ROLE_MATCH_SCORE : 0;
    int workModeScore = workModeMatched ? WORK_MODE_MATCH_SCORE : 0;
    int totalScore = roleScore + skillScore + workModeScore;

    return new Result(
        totalScore,
        scoreLabel(totalScore),
        roleMatched,
        workModeMatched,
        matchedSkillsBySkill.size(),
        requiredLevelBySkill.size(),
        matchedSkillsBySkill.keySet().stream()
            .map(SkillKey::skillId)
            .toList(),
        matchedSkillTitles);
  }

  public static String scoreLabel(int score) {
    if (score > 100) {
      return "Överkvalificerad Matchning";
    }
    if (score >= 90) {
      return "Utmärkt Matchning";
    }
    if (score >= 80) {
      return "Bra Matchning";
    }
    if (score >= 75) {
      return "OK Matchning";
    }
    if (score >= QUALIFIED_MATCH_SCORE) {
      return "Svag Matchning";
    }
    return "Otillräckligt";
  }

  public static ScoreBreakdown scoreBreakdownFromSnapshot(PersistedScoreSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    SkillBreakdown skillBreakdown = buildSkillBreakdown(
        snapshot.requiredSkills(),
        snapshot.matchedSkillTitles());

    int rolePoints = snapshot.roleMatched() ? ROLE_MATCH_SCORE : 0;
    int workModePoints = snapshot.workModeMatched() ? WORK_MODE_MATCH_SCORE : 0;
    boolean passedThreshold = qualifiedScore(snapshot.score());

    return new ScoreBreakdown(
        snapshot.score(),
        snapshot.scoreLabel(),
        QUALIFIED_MATCH_SCORE,
        passedThreshold,
        decision(snapshot.score(), snapshot.scoreLabel(), passedThreshold),
        List.of(
            factor(
                "Role",
                snapshot.roleMatched(),
                snapshot.roleMatched() ? 1 : 0,
                1,
                ROLE_MATCH_SCORE,
                rolePoints,
                snapshot.roleEvidence(),
                snapshot.roleMatched()
                    ? "Candidate profile role and role experience fulfilled the mission slot requirement."
                    : "Candidate profile role or role experience did not fulfill the mission slot requirement."),
            factor(
                "Senior Skills",
                skillBreakdown.seniorMatchedCount() > 0,
                skillBreakdown.seniorMatchedCount(),
                skillBreakdown.seniorRequiredCount(),
                SENIOR_SKILL_SCORE,
                skillBreakdown.seniorPoints(),
                skillBreakdown.seniorEvidence(),
                "Required skill levels that use the canonical +5 skill score."),
            factor(
                "Junior Skills",
                skillBreakdown.juniorMatchedCount() > 0,
                skillBreakdown.juniorMatchedCount(),
                skillBreakdown.juniorRequiredCount(),
                JUNIOR_SKILL_SCORE,
                skillBreakdown.juniorPoints(),
                skillBreakdown.juniorEvidence(),
                "Required junior-level skill matches that use the canonical +2 skill score."),
            factor(
                "Work Mode",
                snapshot.workModeMatched(),
                snapshot.workModeMatched() ? 1 : 0,
                1,
                WORK_MODE_MATCH_SCORE,
                workModePoints,
                snapshot.workModeEvidence(),
                snapshot.workModeMatched()
                    ? "Candidate profile work mode matched the mission work mode."
                    : "Candidate profile work mode did not match the mission work mode.")),
        snapshot.matchedSkillTitles(),
        missingOrWeakFactors(snapshot, skillBreakdown));
  }

  private static Map<SkillKey, Integer> highestRequiredLevelBySkill(List<RequiredSkill> requiredSkills) {
    Map<SkillKey, Integer> requiredLevelBySkill = new LinkedHashMap<>();
    for (RequiredSkill requiredSkill : requiredSkills) {
      SkillKey skillKey = new SkillKey(requiredSkill.skillCategory(), requiredSkill.skillId());
      int requiredLevel = requiredSkill.requiredSkillLevel();
      Integer currentRequiredLevel = requiredLevelBySkill.get(skillKey);
      if (currentRequiredLevel == null || requiredLevel > currentRequiredLevel) {
        requiredLevelBySkill.put(skillKey, requiredLevel);
      }
    }
    return requiredLevelBySkill;
  }

  private static Map<SkillKey, CandidateSkill> strongestCandidateSkillBySkill(
      List<CandidateSkill> candidateSkills) {
    Map<SkillKey, CandidateSkill> candidateSkillBySkill = new LinkedHashMap<>();
    for (CandidateSkill candidateSkill : candidateSkills) {
      SkillKey skillKey = new SkillKey(candidateSkill.skillCategory(), candidateSkill.skillId());
      CandidateSkill currentCandidateSkill = candidateSkillBySkill.get(skillKey);
      if (currentCandidateSkill == null || candidateSkill.skillLevel() > currentCandidateSkill.skillLevel()) {
        candidateSkillBySkill.put(skillKey, candidateSkill);
      }
    }
    return candidateSkillBySkill;
  }

  public static int scoreForRequiredSkillLevel(int requiredSkillLevel) {
    return requiredSkillLevel >= SENIOR_SKILL_LEVEL_RANK
        ? SENIOR_SKILL_SCORE
        : JUNIOR_SKILL_SCORE;
  }

  public static boolean qualifiedScore(int score) {
    return score >= QUALIFIED_MATCH_SCORE;
  }

  private static SkillBreakdown buildSkillBreakdown(
      List<RequiredSkillEvidence> requiredSkills,
      List<String> matchedSkillTitles) {
    Set<String> matchedTitleSet = normalizedSkillTitles(matchedSkillTitles);
    int seniorRequiredCount = 0;
    int seniorMatchedCount = 0;
    int juniorRequiredCount = 0;
    int juniorMatchedCount = 0;
    List<String> seniorEvidence = new ArrayList<>();
    List<String> juniorEvidence = new ArrayList<>();
    List<String> unmatchedEvidence = new ArrayList<>();

    for (RequiredSkillEvidence requiredSkill : requiredSkills) {
      boolean seniorScored = scoreForRequiredSkillLevel(requiredSkill.skillLevelId()) == SENIOR_SKILL_SCORE;
      boolean matched = matchedTitleSet.contains(normalizeSkillTitle(requiredSkill.skillTitle()));
      String evidence = skillEvidence(requiredSkill);
      if (seniorScored) {
        seniorRequiredCount++;
        if (matched) {
          seniorMatchedCount++;
          seniorEvidence.add(evidence);
        }
      } else {
        juniorRequiredCount++;
        if (matched) {
          juniorMatchedCount++;
          juniorEvidence.add(evidence);
        }
      }
      if (!matched) {
        unmatchedEvidence.add(evidence);
      }
    }

    return new SkillBreakdown(
        seniorRequiredCount,
        seniorMatchedCount,
        seniorMatchedCount * SENIOR_SKILL_SCORE,
        juniorRequiredCount,
        juniorMatchedCount,
        juniorMatchedCount * JUNIOR_SKILL_SCORE,
        List.copyOf(seniorEvidence),
        List.copyOf(juniorEvidence),
        List.copyOf(unmatchedEvidence));
  }

  private static List<String> missingOrWeakFactors(
      PersistedScoreSnapshot snapshot,
      SkillBreakdown skillBreakdown) {
    List<String> missingOrWeak = new ArrayList<>();
    if (!snapshot.roleMatched()) {
      missingOrWeak.add("Role match or required role experience was not fulfilled.");
    }
    if (!snapshot.workModeMatched()) {
      missingOrWeak.add("Work mode was not matched.");
    }
    missingOrWeak.addAll(skillBreakdown.unmatchedEvidence().stream()
        .map(skill -> "Required skill not matched: " + skill)
        .toList());

    int rolePoints = snapshot.roleMatched() ? ROLE_MATCH_SCORE : 0;
    int workModePoints = snapshot.workModeMatched() ? WORK_MODE_MATCH_SCORE : 0;
    int expectedSkillPointsFromSnapshot = snapshot.score() - rolePoints - workModePoints;
    int attributedSkillPoints = skillBreakdown.seniorPoints() + skillBreakdown.juniorPoints();
    if (expectedSkillPointsFromSnapshot != attributedSkillPoints) {
      missingOrWeak.add("Skill score attribution is partially limited by the persisted snapshot format.");
    }
    if (missingOrWeak.isEmpty()) {
      missingOrWeak.add("No missing score factors in the available match snapshot.");
    }
    return List.copyOf(missingOrWeak);
  }

  private static ScoreFactor factor(
      String factor,
      boolean matched,
      int matchedCount,
      int requiredCount,
      int scorePerInstance,
      int points,
      List<String> evidence,
      String note) {
    return new ScoreFactor(
        factor,
        matched,
        matchedCount,
        requiredCount,
        scorePerInstance,
        points,
        evidence == null ? List.of() : List.copyOf(evidence),
        note);
  }

  private static Set<String> normalizedSkillTitles(List<String> matchedSkillTitles) {
    Set<String> normalized = new HashSet<>();
    if (matchedSkillTitles == null) {
      return normalized;
    }
    matchedSkillTitles.stream()
        .map(CandidateToMissionSlotMatchScorer::normalizeSkillTitle)
        .filter(title -> !title.isBlank())
        .forEach(normalized::add);
    return normalized;
  }

  private static String normalizeSkillTitle(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private static String skillEvidence(RequiredSkillEvidence skill) {
    return skill.skillTitle() + " / " + skill.skillCategory() + " / " + skill.skillLevelName();
  }

  private static String decision(int score, String label, boolean passedThreshold) {
    if (passedThreshold) {
      return "Match exists because persisted canonical score "
          + score
          + " meets discovery threshold "
          + QUALIFIED_MATCH_SCORE
          + " (" + label + ").";
    }
    return "Match does not meet discovery threshold "
        + QUALIFIED_MATCH_SCORE
        + " and should not be treated as discoverable.";
  }

  private static boolean workModeMatches(String requiredWorkMode, String candidateWorkMode) {
    String required = normalizeWorkMode(requiredWorkMode);
    String candidate = normalizeWorkMode(candidateWorkMode);
    return !required.isBlank() && required.equals(candidate);
  }

  private static String normalizeWorkMode(String value) {
    String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
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
        || normalized.contains("ONSITE")) {
      return "ON_PREMISE";
    }
    return normalized;
  }

  public record RequiredRole(long roleId, int requiredExperienceYears) {
  }

  public record CandidateRole(long roleId, int experienceYears) {
  }

  private record SkillKey(String skillCategory, long skillId) {
    private SkillKey {
      skillCategory = skillCategory == null ? "" : skillCategory.trim().toUpperCase(Locale.ROOT);
    }
  }

  public record RequiredSkill(String skillCategory, long skillId, int requiredSkillLevel) {
  }

  public record CandidateSkill(String skillCategory, long skillId, int skillLevel, String skillTitle) {
  }

  public record RequiredSkillEvidence(
      String skillTitle,
      String skillCategory,
      int skillLevelId,
      String skillLevelName) {
  }

  public record PersistedScoreSnapshot(
      int score,
      String scoreLabel,
      boolean roleMatched,
      boolean workModeMatched,
      List<RequiredSkillEvidence> requiredSkills,
      List<String> matchedSkillTitles,
      List<String> roleEvidence,
      List<String> workModeEvidence) {

    public PersistedScoreSnapshot {
      scoreLabel = scoreLabel == null || scoreLabel.isBlank()
          ? CandidateToMissionSlotMatchScorer.scoreLabel(score)
          : scoreLabel.trim();
      requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
      matchedSkillTitles = matchedSkillTitles == null ? List.of() : List.copyOf(matchedSkillTitles);
      roleEvidence = roleEvidence == null ? List.of() : List.copyOf(roleEvidence);
      workModeEvidence = workModeEvidence == null ? List.of() : List.copyOf(workModeEvidence);
    }
  }

  public record ScoreFactor(
      String factor,
      boolean matched,
      int matchedCount,
      int requiredCount,
      int scorePerInstance,
      int points,
      List<String> evidence,
      String note) {
  }

  public record ScoreBreakdown(
      int score,
      String scoreLabel,
      int discoveryThreshold,
      boolean passedDiscoveryThreshold,
      String decision,
      List<ScoreFactor> factors,
      List<String> matchedSkillTitles,
      List<String> missingOrWeakFactors) {
  }

  private record SkillBreakdown(
      int seniorRequiredCount,
      int seniorMatchedCount,
      int seniorPoints,
      int juniorRequiredCount,
      int juniorMatchedCount,
      int juniorPoints,
      List<String> seniorEvidence,
      List<String> juniorEvidence,
      List<String> unmatchedEvidence) {
  }

  public record Result(
      int score,
      String scoreLabel,
      boolean roleMatched,
      boolean workModeMatched,
      int matchedSkillCount,
      int requiredSkillCount,
      List<Long> matchedSkillIds,
      List<String> matchedSkillTitles) {

    public boolean qualifiedMatch() {
      return qualifiedScore(score);
    }
  }
}
