package mcp.server.domain.missions.application.intake;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupEntity;
import mcp.server.domain.reference_data.persistence.RoleEntity;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.RoleJpaRepo;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;

import org.springframework.stereotype.Component;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class MissionProposalRequirementTextDetector {

  private static final int MAX_REQUIRED_SKILLS = 12;
  private static final int MAX_MISSION_SLOTS = 5;
  private static final Pattern YEARS_EXPERIENCE_PATTERN = Pattern
      .compile("(?iu)\\b(\\d{1,2})\\+?\\s*(?:years?|yrs?|\\u00E5r)\\s*(?:experience|erfarenhet)?\\b");
  private static final String SINGLE_SLOT_SKILL_CUE_REGEX = "varav\\s+en|en\\s+av\\s+dem|minst\\s+en|den\\s+andra|en\\s+(?:b\\u00F6r|bor|m\\u00E5ste|maste|ska|skall)";
  private static final String SECOND_SLOT_SKILL_CUE_REGEX = "den\\s+andra";
  private static final String ALL_SLOTS_SKILL_CUE_REGEX = "b\\u00E5da|bada";

  private final RoleJpaRepo roleRepo;
  private final SkillCatalogLookup skillLookup;
  private final CompetencyLevelLookupJpaRepo skillLevelRepo;
  private final MissionProposalTextDetectionCatalogService textDetectionCatalogService;
  private final MissionProposalTextDetectionPatternFactory patternFactory;
  private final MissionProposalTextMatcher textMatcher;
  private final MissionProposalTextEvidenceRecorder evidenceRecorder;
  private final Pattern slotCountPattern;

  MissionProposalRequirementTextDetector(
      RoleJpaRepo roleRepo,
      SkillCatalogLookup skillLookup,
      CompetencyLevelLookupJpaRepo skillLevelRepo,
      MissionProposalTextDetectionCatalogService textDetectionCatalogService,
      MissionProposalTextDetectionPatternFactory patternFactory,
      MissionProposalTextMatcher textMatcher,
      MissionProposalTextEvidenceRecorder evidenceRecorder) {
    this.roleRepo = roleRepo;
    this.skillLookup = skillLookup;
    this.skillLevelRepo = skillLevelRepo;
    this.textDetectionCatalogService = textDetectionCatalogService;
    this.patternFactory = patternFactory;
    this.textMatcher = textMatcher;
    this.evidenceRecorder = evidenceRecorder;
    this.slotCountPattern = patternFactory.numericSlotCountPattern();
  }

  String detectWorkMode(
      String sourceText,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    for (MissionProposalTextDetectionCatalogService.WorkModeSignal signal : textDetectionCatalogService
        .missionWorkModeSignals()) {
      String term = textMatcher.firstContainedTerm(sourceText, signal.terms().toArray(String[]::new));
      if (!term.isBlank()) {
        evidenceRecorder.add(evidence, "workMode", signal.workMode(), bestWorkModeSource(sourceText, term));
        return signal.workMode();
      }
    }
    return "";
  }

  DetectedYears detectExperienceYears(
      String sourceText,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    Matcher matcher = YEARS_EXPERIENCE_PATTERN.matcher(sourceText);
    if (matcher.find()) {
      int years = Integer.parseInt(matcher.group(1));
      evidenceRecorder.add(
          evidence,
          "missionSlots[0].requiredRoleExperienceYears",
          Integer.toString(years),
          matcher.group());
      return new DetectedYears(years, true);
    }

    for (MissionProposalTextDetectionCatalogService.ExperienceLevelSignal signal : textDetectionCatalogService
        .missionExperienceLevelSignals()) {
      String term = textMatcher.firstContainedTerm(sourceText, signal.terms().toArray(String[]::new));
      if (!term.isBlank()) {
        return inferredExperienceYears(sourceText, evidence, signal.inferredYears(), term);
      }
    }
    return new DetectedYears(0, false);
  }

  DetectedSlotCount detectMissionSlotCount(
      String sourceText,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    Matcher matcher = slotCountPattern.matcher(sourceText);
    if (matcher.find()) {
      return detectedSlotCount(evidence, Integer.parseInt(matcher.group(1)), matcher.group());
    }

    for (CountWord countWord : slotCountWords()) {
      Pattern pattern = patternFactory.countWordSlotCountPattern(countWord.word());
      Matcher wordMatcher = pattern.matcher(sourceText);
      if (wordMatcher.find()) {
        return detectedSlotCount(evidence, countWord.count(), wordMatcher.group());
      }
    }

    return new DetectedSlotCount(1, false);
  }

  MatchedRole detectRole(
      String sourceText,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    List<RoleEntity> roles = roleRepo.findAll().stream()
        .filter(role -> !safeText(role.getRoleTitle()).isBlank())
        .sorted(Comparator.comparingInt((RoleEntity role) -> role.getRoleTitle().length()).reversed())
        .toList();

    MatchedRole exactRole = roles.stream()
        .filter(role -> textMatcher.containsTerm(sourceText, role.getRoleTitle()))
        .findFirst()
        .map(role -> {
          String source = evidenceRecorder.excerptAround(sourceText, role.getRoleTitle());
          evidenceRecorder.add(evidence, "missionSlots[0].roleId", role.getRoleTitle(), source);
          return new MatchedRole(role, source);
        })
        .orElse(null);
    if (exactRole != null) {
      return exactRole;
    }

    return inferredRole(sourceText, roles, evidence);
  }

  List<MatchedSkill> detectSkills(
      String sourceText,
      DetectedSlotCount slotCount,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    LinkedHashMap<String, MatchedSkill> skillsByKey = new LinkedHashMap<>();
    java.util.stream.Stream.concat(
        skillLookup.findAllPrimarySkills().stream()
            .map(skill -> new SkillCandidate(SkillCatalogLookup.CATEGORY_PRIMARY, skill)),
        skillLookup.findAllSecondarySkills().stream()
            .map(skill -> new SkillCandidate(SkillCatalogLookup.CATEGORY_SECONDARY, skill)))
        .filter(skill -> !safeText(skill.skill().title()).isBlank())
        .sorted(Comparator.comparingInt((SkillCandidate skill) -> skill.skill().title().length()).reversed())
        .forEach(skill -> {
          if (skillsByKey.size() < MAX_REQUIRED_SKILLS
              && textMatcher.containsTerm(sourceText, skill.skill().title())) {
            SkillDistribution distribution = detectSkillDistribution(sourceText, skill.skill().title(), slotCount);
            String source = distribution.sourceText().isBlank()
                ? evidenceRecorder.excerptAround(sourceText, skill.skill().title())
                : distribution.sourceText();
            skillsByKey.putIfAbsent(
                skill.skillCategory() + ":" + skill.skill().id(),
                new MatchedSkill(skill.skillCategory(), skill.skill(), source, distribution.slotIndex()));
          }
        });
    skillsByKey.values().forEach(skill -> evidenceRecorder.add(
        evidence,
        skill.slotIndex() == null
            ? "missionSlots.requiredSkills"
            : "missionSlots[" + skill.slotIndex() + "].requiredSkills",
        skill.skill().title(),
        skill.sourceText()));
    return List.copyOf(skillsByKey.values());
  }

  DetectedSkillLevel detectSkillLevel(
      String sourceText,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    List<CompetencyLevelLookupEntity> skillLevels = skillLevelRepo.findAll().stream()
        .sorted(Comparator.comparing(CompetencyLevelLookupEntity::getCompetencyLevelLookupId))
        .toList();
    CompetencyLevelLookupEntity detected = explicitSkillLevel(sourceText, skillLevels);
    if (detected != null) {
      evidenceRecorder.add(
          evidence,
          "missionSlots[0].requiredSkills.skillLevelId",
          detected.getCompetencyLevelName(),
          evidenceRecorder.excerptAround(sourceText, detected.getCompetencyLevelName()));
      return new DetectedSkillLevel(
          detected.getCompetencyLevelLookupId(),
          detected.getCompetencyLevelName(),
          true);
    }

    CompetencyLevelLookupEntity fallback = fallbackSkillLevel(skillLevels);
    return new DetectedSkillLevel(
        fallback == null ? 0 : fallback.getCompetencyLevelLookupId(),
        fallback == null ? "" : fallback.getCompetencyLevelName(),
        false);
  }

  private DetectedYears inferredExperienceYears(
      String sourceText,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence,
      int years,
      String term) {

    evidenceRecorder.add(
        evidence,
        "missionSlots[0].requiredRoleExperienceYears",
        Integer.toString(years),
        evidenceRecorder.excerptAround(sourceText, term));
    return new DetectedYears(years, true);
  }

  private DetectedSlotCount detectedSlotCount(
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence,
      int requestedCount,
      String sourceText) {

    int count = Math.max(1, Math.min(requestedCount, MAX_MISSION_SLOTS));
    evidenceRecorder.add(evidence, "missionSlots", Integer.toString(count), sourceText);
    return new DetectedSlotCount(count, true);
  }

  private List<CountWord> slotCountWords() {
    return textDetectionCatalogService.missionSlotCountWords().stream()
        .map(signal -> new CountWord(signal.word(), signal.count()))
        .toList();
  }

  private MatchedRole inferredRole(
      String sourceText,
      List<RoleEntity> roles,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    List<RoleSignal> signals = textDetectionCatalogService.missionRoleSignals().stream()
        .map(signal -> new RoleSignal(signal.roleTitle(), signal.terms()))
        .toList();

    for (RoleSignal signal : signals) {
      RoleEntity role = findRoleByTitle(roles, signal.roleTitle());
      if (role == null) {
        continue;
      }
      for (String term : signal.terms()) {
        if (textMatcher.containsTerm(sourceText, term)) {
          String source = evidenceRecorder.excerptAround(sourceText, term);
          evidenceRecorder.add(evidence, "missionSlots[0].roleId", role.getRoleTitle(), source);
          return new MatchedRole(role, source);
        }
      }
    }
    return null;
  }

  private RoleEntity findRoleByTitle(List<RoleEntity> roles, String roleTitle) {
    return roles.stream()
        .filter(role -> role.getRoleTitle().equalsIgnoreCase(roleTitle))
        .findFirst()
        .orElse(null);
  }

  private SkillDistribution detectSkillDistribution(
      String sourceText,
      String skillTitle,
      DetectedSlotCount slotCount) {

    if (slotCount.count() <= 1) {
      return SkillDistribution.common();
    }

    DistributedSkillScope allSlotsScope = distributedSkillScope(
        sourceText,
        skillTitle,
        ALL_SLOTS_SKILL_CUE_REGEX);
    if (allSlotsScope.detected()) {
      return SkillDistribution.common(allSlotsScope.sourceText());
    }

    DistributedSkillScope secondSlotScope = distributedSkillScope(
        sourceText,
        skillTitle,
        SECOND_SLOT_SKILL_CUE_REGEX);
    if (secondSlotScope.detected()) {
      return SkillDistribution.singleSlot(Math.min(1, slotCount.count() - 1), secondSlotScope.sourceText());
    }

    DistributedSkillScope singleSlotScope = distributedSkillScope(
        sourceText,
        skillTitle,
        SINGLE_SLOT_SKILL_CUE_REGEX);
    if (singleSlotScope.detected()) {
      return SkillDistribution.singleSlot(Math.min(1, slotCount.count() - 1), singleSlotScope.sourceText());
    }

    return SkillDistribution.common();
  }

  private DistributedSkillScope distributedSkillScope(
      String sourceText,
      String skillTitle,
      String cueRegex) {

    Pattern pattern = Pattern.compile(
        "(?iu)\\b(?:"
            + cueRegex
            + ")\\b[^\\n.]{0,180}"
            + textMatcher.termBoundaryRegex(skillTitle));
    Matcher matcher = pattern.matcher(sourceText);
    if (!matcher.find()) {
      return DistributedSkillScope.notDetected();
    }
    return new DistributedSkillScope(
        evidenceRecorder.limitLength(matcher.group().replaceAll("\\s+", " ").trim(), 180),
        true);
  }

  private CompetencyLevelLookupEntity explicitSkillLevel(
      String sourceText,
      List<CompetencyLevelLookupEntity> skillLevels) {

    for (CompetencyLevelLookupEntity skillLevel : skillLevels.stream()
        .sorted(Comparator.comparingInt((CompetencyLevelLookupEntity level) -> level.getCompetencyLevelName().length())
            .reversed())
        .toList()) {
      if (textMatcher.containsTerm(sourceText, skillLevel.getCompetencyLevelName())) {
        return skillLevel;
      }
    }
    for (MissionProposalTextDetectionCatalogService.ExperienceLevelSignal signal : textDetectionCatalogService
        .missionExperienceLevelSignals()) {
      if (!textMatcher.firstContainedTerm(sourceText, signal.terms().toArray(String[]::new)).isBlank()) {
        return findSkillLevel(skillLevels, signal.skillLevelNames().toArray(String[]::new));
      }
    }
    return null;
  }

  private CompetencyLevelLookupEntity fallbackSkillLevel(List<CompetencyLevelLookupEntity> skillLevels) {
    CompetencyLevelLookupEntity mid = findSkillLevel(
        skillLevels,
        textDetectionCatalogService.missionFallbackSkillLevelNames().toArray(String[]::new));
    return mid == null && !skillLevels.isEmpty() ? skillLevels.get(0) : mid;
  }

  private CompetencyLevelLookupEntity findSkillLevel(List<CompetencyLevelLookupEntity> skillLevels, String... names) {
    for (String name : names) {
      for (CompetencyLevelLookupEntity skillLevel : skillLevels) {
        if (skillLevel.getCompetencyLevelName().toUpperCase(Locale.ROOT).contains(name)) {
          return skillLevel;
        }
      }
    }
    return null;
  }

  private String bestWorkModeSource(String sourceText, String... terms) {
    for (String term : terms) {
      String excerpt = evidenceRecorder.excerptAround(sourceText, term);
      if (!excerpt.isBlank()) {
        return excerpt;
      }
    }
    return "";
  }

  record DetectedYears(int years, boolean detected) {
  }

  record DetectedSlotCount(int count, boolean detected) {
  }

  record MatchedRole(RoleEntity role, String sourceText) {
  }

  record MatchedSkill(
      String skillCategory,
      SkillCatalogLookup.SkillRef skill,
      String sourceText,
      Integer slotIndex) {
  }

  record DetectedSkillLevel(short skillLevelId, String skillLevelName, boolean detected) {
  }

  private record SkillCandidate(String skillCategory, SkillCatalogLookup.SkillRef skill) {
  }

  private record SkillDistribution(Integer slotIndex, String sourceText) {
    private static SkillDistribution common() {
      return new SkillDistribution(null, "");
    }

    private static SkillDistribution common(String sourceText) {
      return new SkillDistribution(null, sourceText);
    }

    private static SkillDistribution singleSlot(int slotIndex, String sourceText) {
      return new SkillDistribution(slotIndex, sourceText);
    }
  }

  private record DistributedSkillScope(String sourceText, boolean detected) {
    private static DistributedSkillScope notDetected() {
      return new DistributedSkillScope("", false);
    }
  }

  private record RoleSignal(String roleTitle, List<String> terms) {
  }

  private record CountWord(String word, int count) {
  }
}
