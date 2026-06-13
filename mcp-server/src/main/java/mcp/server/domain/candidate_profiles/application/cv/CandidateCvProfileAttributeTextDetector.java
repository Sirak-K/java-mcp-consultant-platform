package mcp.server.domain.candidate_profiles.application.cv;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class CandidateCvProfileAttributeTextDetector {

  private final CandidateCvExtractionCatalogService textDetectionCatalogService;
  private final CandidateCvTextMatcher textMatcher;

  CandidateCvProfileAttributeTextDetector(
      CandidateCvExtractionCatalogService textDetectionCatalogService,
      CandidateCvTextMatcher textMatcher) {
    this.textDetectionCatalogService = textDetectionCatalogService;
    this.textMatcher = textMatcher;
  }

  String detectWorkStatus(String profileSummary) {
    if (textMatcher.containsIgnoreCase(profileSummary, "open to work")) {
      return "Open to work";
    }
    if (textMatcher.containsIgnoreCase(profileSummary, "available immediately")
        || textMatcher.containsIgnoreCase(profileSummary, "available now")) {
      return "Available";
    }
    if (textMatcher.containsIgnoreCase(profileSummary, "f\u00F6rsta jobb inom it")
        || textMatcher.containsIgnoreCase(profileSummary, "f\u00F6rsta jobbet inom it")) {
      return "Seeking first IT role";
    }
    return "";
  }

  String detectWorkModeText(String extractedText, String profileSummary) {
    for (CandidateCvExtractionCatalogService.CandidateWorkModeSignal signal : textDetectionCatalogService
        .candidateWorkModeSignals()) {
      if (textMatcher.containsAnyIgnoreCase(extractedText, signal.terms())
          || textMatcher.containsAnyIgnoreCase(profileSummary, signal.profileSummaryTerms())) {
        return signal.displayValue();
      }
    }
    return "";
  }

  String normalizeCandidateWorkMode(String value) {
    String normalized = safeText(value).trim().toUpperCase(Locale.ROOT);
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
        || normalized.contains("ONSITE")
        || normalized.contains("SITE")) {
      return "ON_PREMISE";
    }
    return normalized;
  }

  String detectLanguages(List<String> languageLines) {
    if (languageLines.isEmpty()) {
      return "";
    }
    return languageLines.stream()
        .map(line -> {
          String[] parts = line.split("\\s+[\\u2013-]\\s+", 2);
          if (parts.length == 2) {
            return parts[0].trim() + " (" + parts[1].trim() + ")";
          }
          return line.trim();
        })
        .collect(Collectors.joining(", "));
  }
}
