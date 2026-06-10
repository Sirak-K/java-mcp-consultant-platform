package mcp.server.domain.candidate_profiles.application.cv;

import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupEntity;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class CandidateCvSkillTextDetector {

  private final SkillCatalogLookup skillLookup;
  private final CompetencyLevelLookupJpaRepo skillLevelRepo;
  private final CandidateProfileTextDetectionCatalogService textDetectionCatalogService;
  private final CandidateCvTextMatcher textMatcher;

  CandidateCvSkillTextDetector(
      SkillCatalogLookup skillLookup,
      CompetencyLevelLookupJpaRepo skillLevelRepo,
      CandidateProfileTextDetectionCatalogService textDetectionCatalogService,
      CandidateCvTextMatcher textMatcher) {
    this.skillLookup = skillLookup;
    this.skillLevelRepo = skillLevelRepo;
    this.textDetectionCatalogService = textDetectionCatalogService;
    this.textMatcher = textMatcher;
  }

  String detectSkillsText(List<String> skillLines, String extractedText) {
    List<String> sectionSkills = skillLines.stream()
        .map(textMatcher::cleanLine)
        .filter(line -> !line.isBlank())
        .toList();
    if (!sectionSkills.isEmpty()) {
      return String.join(", ", sectionSkills);
    }

    LinkedHashSet<String> matchedSkills = new LinkedHashSet<>();
    skillLookup.findAllSkills().stream()
        .map(SkillCatalogLookup.SkillRef::title)
        .filter(Objects::nonNull)
        .distinct()
        .sorted(Comparator.comparingInt(String::length).reversed())
        .forEach(skillTitle -> {
          if (matchedSkills.size() < 12 && containsSkillTerm(extractedText, skillTitle)) {
            matchedSkills.add(skillTitle);
          }
        });
    return String.join(", ", matchedSkills);
  }

  List<CandidateCvWebContract.CandidateSkillWorkingCopyView> detectCandidateSkills(
      List<String> skillLines,
      String extractedText) {

    String detectionText = skillLines == null || skillLines.isEmpty()
        ? safeText(extractedText)
        : String.join("\n", skillLines);
    if (detectionText.isBlank()) {
      return List.of();
    }

    List<CompetencyLevelLookupEntity> skillLevels = skillLevelRepo.findAll().stream()
        .sorted(Comparator.comparing(CompetencyLevelLookupEntity::getCompetencyLevelLookupId))
        .toList();
    if (skillLevels.isEmpty()) {
      return List.of();
    }
    CompetencyLevelLookupEntity defaultLevel = skillLevels.get(0);

    List<String> contextLines = skillLines == null || skillLines.isEmpty()
        ? safeText(extractedText).lines()
            .map(textMatcher::cleanLine)
            .filter(line -> !line.isBlank())
            .toList()
        : skillLines;

    return skillLookup.findAllSkills().stream()
        .filter(skill -> skill.title() != null)
        .distinct()
        .sorted(Comparator.comparingInt((SkillCatalogLookup.SkillRef skill) -> skill.title().length()).reversed())
        .filter(skill -> containsSkillTerm(detectionText, skill.title()))
        .limit(12)
        .map(skill -> {
          CompetencyLevelLookupEntity level = detectSkillLevelForSkill(
              skill.title(),
              detectionText,
              contextLines,
              skillLevels,
              defaultLevel);
          return new CandidateCvWebContract.CandidateSkillWorkingCopyView(
              skill.id(),
              skill.title(),
              skill.category(),
              level.getCompetencyLevelLookupId(),
              level.getCompetencyLevelName());
        })
        .toList();
  }

  private CompetencyLevelLookupEntity detectSkillLevelForSkill(
      String skillTitle,
      String detectionText,
      List<String> contextLines,
      List<CompetencyLevelLookupEntity> skillLevels,
      CompetencyLevelLookupEntity defaultLevel) {

    String exactContext = exactSkillLevelContext(skillTitle, detectionText);
    if (!exactContext.isBlank()) {
      return skillLevelFromContext(exactContext, skillLevels, defaultLevel);
    }
    return contextLines.stream()
        .filter(line -> containsSkillTerm(line, skillTitle))
        .map(line -> skillLevelFromContext(line, skillLevels, defaultLevel))
        .filter(level -> level != defaultLevel)
        .findFirst()
        .orElse(defaultLevel);
  }

  private String exactSkillLevelContext(String skillTitle, String detectionText) {
    for (String term : skillDetectionTerms(skillTitle)) {
      Matcher matcher = Pattern
          .compile("(?iu)" + Pattern.quote(term) + "\\s*\\(([^)]{1,40})\\)")
          .matcher(safeText(detectionText));
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    return "";
  }

  private CompetencyLevelLookupEntity skillLevelFromContext(
      String context,
      List<CompetencyLevelLookupEntity> skillLevels,
      CompetencyLevelLookupEntity defaultLevel) {

    String normalizedContext = safeText(context).toUpperCase(Locale.ROOT);
    return skillLevels.stream()
        .filter(level -> normalizedContext.contains(
            safeText(level.getCompetencyLevelName()).toUpperCase(Locale.ROOT)))
        .findFirst()
        .orElse(defaultLevel);
  }

  private boolean containsSkillTerm(String text, String skillTitle) {
    if (textMatcher.containsTerm(text, skillTitle)) {
      return true;
    }
    return skillAliases(skillTitle).stream()
        .anyMatch(alias -> textMatcher.containsCatalogAlias(text, alias));
  }

  private List<String> skillDetectionTerms(String skillTitle) {
    String canonicalTitle = safeText(skillTitle).trim();
    if (canonicalTitle.isBlank()) {
      return List.of();
    }
    List<String> aliases = skillAliases(canonicalTitle);
    if (aliases.isEmpty()) {
      return List.of(canonicalTitle);
    }
    List<String> terms = new ArrayList<>(1 + aliases.size());
    terms.add(canonicalTitle);
    terms.addAll(aliases);
    return terms;
  }

  private List<String> skillAliases(String skillTitle) {
    return textDetectionCatalogService.candidateSkillTitleAliases().getOrDefault(
        safeText(skillTitle).trim(),
        List.of());
  }
}
