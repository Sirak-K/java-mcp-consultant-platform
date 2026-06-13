package mcp.server.domain.candidate_profiles.application.cv;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class CandidateCvLocationTextDetector {

  private final CandidateCvExtractionCatalogService textDetectionCatalogService;
  private final CandidateCvTextMatcher textMatcher;

  CandidateCvLocationTextDetector(
      CandidateCvExtractionCatalogService textDetectionCatalogService,
      CandidateCvTextMatcher textMatcher) {
    this.textDetectionCatalogService = textDetectionCatalogService;
    this.textMatcher = textMatcher;
  }

  DetectedLocation detectCandidateLocation(List<String> lines, String extractedText) {
    for (String line : lines) {
      String lowerLine = safeText(line).toLowerCase(Locale.ROOT);
      if (textMatcher.containsAnyIgnoreCase(lowerLine, textDetectionCatalogService.candidateLocationHeadingTerms())) {
        DetectedLocation location = detectLocationFromText(line);
        if (!location.isEmpty()) {
          return location;
        }
      }
    }
    return looksSwedish(extractedText) ? new DetectedLocation("", "Sweden") : DetectedLocation.empty();
  }

  DetectedLocation detectLocationFromText(String text) {
    String safeText = safeText(text);
    for (String city : textDetectionCatalogService.candidateSwedishCityNames()) {
      if (textMatcher.containsTerm(safeText, city)) {
        return new DetectedLocation(city, "Sweden");
      }
    }
    for (CandidateCvExtractionCatalogService.CountryTermSignal signal : textDetectionCatalogService
        .candidateCountryTermSignals()) {
      if (textMatcher.containsAnyIgnoreCase(safeText, signal.terms())) {
        return new DetectedLocation("", signal.country());
      }
    }
    return DetectedLocation.empty();
  }

  private boolean looksSwedish(String text) {
    return textMatcher.containsAnyIgnoreCase(text, textDetectionCatalogService.candidateSwedishLanguageClues());
  }

  record DetectedLocation(String city, String country) {
    static DetectedLocation empty() {
      return new DetectedLocation("", "");
    }

    boolean isEmpty() {
      return city.isBlank() && country.isBlank();
    }
  }
}
