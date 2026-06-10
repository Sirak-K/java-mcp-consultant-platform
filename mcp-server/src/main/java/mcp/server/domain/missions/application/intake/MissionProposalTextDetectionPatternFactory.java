package mcp.server.domain.missions.application.intake;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
final class MissionProposalTextDetectionPatternFactory {

  private final MissionProposalTextDetectionCatalogService textDetectionCatalogService;

  MissionProposalTextDetectionPatternFactory(
      MissionProposalTextDetectionCatalogService textDetectionCatalogService) {
    this.textDetectionCatalogService = textDetectionCatalogService;
  }

  Pattern writtenDatePattern() {
    return Pattern.compile(
        "(?iu)\\b(\\d{1,2})\\s+("
            + monthRegexAlternation()
            + ")\\s+(\\d{4})\\b");
  }

  Pattern monthYearDatePattern() {
    return Pattern.compile(
        "(?iu)\\b(?:fr\\u00E5n|from|start|startar|startdatum|b\\u00F6rjar|starting)\\s+(?:i\\s+)?("
            + monthRegexAlternation()
            + ")\\s+(\\d{4})\\b");
  }

  Pattern numericSlotCountPattern() {
    return Pattern.compile(
        "(?iu)\\b(\\d{1,2})(?:\\s+[\\p{L}\\p{N}+#./-]+){0,6}\\s+(?:"
            + slotResourceRegexAlternation()
            + ")\\b");
  }

  Pattern countWordSlotCountPattern(String countWord) {
    return Pattern.compile(
        "(?iu)\\b"
            + Pattern.quote(countWord)
            + "(?:\\s+[\\p{L}\\p{N}+#./-]+){0,6}\\s+"
            + "(?:"
            + slotResourceRegexAlternation()
            + ")"
            + "\\b");
  }

  private String monthRegexAlternation() {
    return textDetectionCatalogService.missionMonthTerms().stream()
        .map(Pattern::quote)
        .collect(Collectors.joining("|"));
  }

  private String slotResourceRegexAlternation() {
    return textDetectionCatalogService.missionSlotResourceTerms().stream()
        .map(this::slotResourceRegexTerm)
        .collect(Collectors.joining("|"));
  }

  private String slotResourceRegexTerm(String term) {
    if ("utvecklare".equalsIgnoreCase(term)) {
      return "(?:[\\p{L}\\p{N}+#./]+-)?" + Pattern.quote(term);
    }
    return Pattern.quote(term);
  }
}
