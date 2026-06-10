package mcp.server.domain.missions.application.intake;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
final class MissionProposalTitleTextDetector {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b");
  private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
  private static final Pattern LABELED_TITLE_PATTERN = Pattern
      .compile("(?imu)^\\s*(?:mission|uppdrag|title|rubrik)\\s*[:\\-]\\s*(.+)$");

  private final MissionProposalTextMatcher textMatcher;
  private final MissionProposalTextEvidenceRecorder evidenceRecorder;

  MissionProposalTitleTextDetector(
      MissionProposalTextMatcher textMatcher,
      MissionProposalTextEvidenceRecorder evidenceRecorder) {
    this.textMatcher = textMatcher;
    this.evidenceRecorder = evidenceRecorder;
  }

  DetectedTitle detectMissionTitle(
      String sourceText,
      String roleTitle,
      String roleSourceText,
      int slotCount,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    Matcher matcher = LABELED_TITLE_PATTERN.matcher(sourceText);
    while (matcher.find()) {
      String title = textMatcher.cleanLine(matcher.group(1));
      if (isUsefulTitle(title)) {
        title = evidenceRecorder.limitLength(title, 200);
        evidenceRecorder.add(evidence, "missionTitle", title, matcher.group());
        return new DetectedTitle(title, true);
      }
    }

    for (String line : textMatcher.normalizedLines(sourceText)) {
      if (isUsefulTitle(line)) {
        String title = evidenceRecorder.limitLength(line, 200);
        evidenceRecorder.add(evidence, "missionTitle", title, line);
        return new DetectedTitle(title, true);
      }
    }

    if (!roleTitle.isBlank()) {
      String title = generatedMissionTitle(roleTitle, slotCount);
      evidenceRecorder.add(evidence, "missionTitle", title, roleSourceText);
      return new DetectedTitle(title, true);
    }
    return new DetectedTitle("", false);
  }

  private String generatedMissionTitle(String roleTitle, int slotCount) {
    if (slotCount > 1) {
      return "Uppdrag - " + slotCount + "x " + pluralizeRoleTitle(roleTitle);
    }
    return "Uppdrag - " + roleTitle;
  }

  private String pluralizeRoleTitle(String roleTitle) {
    String normalized = textMatcher.cleanLine(roleTitle);
    if (normalized.toLowerCase(Locale.ROOT).endsWith("s")) {
      return normalized;
    }
    return normalized + "s";
  }

  private boolean isUsefulTitle(String title) {
    String cleaned = textMatcher.cleanLine(title);
    if (cleaned.length() < 8 || cleaned.length() > 240) {
      return false;
    }
    String normalized = cleaned.toLowerCase(Locale.ROOT);
    if (normalized.startsWith("vi beh\u00F6ver") || normalized.startsWith("we need")) {
      return false;
    }
    if (EMAIL_PATTERN.matcher(cleaned).find() || ISO_DATE_PATTERN.matcher(cleaned).find()) {
      return false;
    }
    return cleaned.chars().anyMatch(Character::isLetter);
  }

  record DetectedTitle(String value, boolean confident) {
  }
}
