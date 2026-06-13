package mcp.server.domain.candidate_profiles.application.cv;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class CandidateCvSectionExtractor {

  private final CandidateCvExtractionCatalogService textDetectionCatalogService;
  private final CandidateCvTextMatcher textMatcher;

  CandidateCvSectionExtractor(
      CandidateCvExtractionCatalogService textDetectionCatalogService,
      CandidateCvTextMatcher textMatcher) {
    this.textDetectionCatalogService = textDetectionCatalogService;
    this.textMatcher = textMatcher;
  }

  Sections extract(List<String> lines) {
    String profileHeading = textDetectionCatalogService.candidateCvSectionHeading("profile");
    String educationHeading = textDetectionCatalogService.candidateCvSectionHeading("education");
    String workExperienceHeading = textDetectionCatalogService.candidateCvSectionHeading("work_experience");
    String skillsHeading = textDetectionCatalogService.candidateCvSectionHeading("skills");
    String languagesHeading = textDetectionCatalogService.candidateCvSectionHeading("languages");
    String referencesHeading = textDetectionCatalogService.candidateCvSectionHeading("references");
    return new Sections(
        sectionLines(lines, profileHeading, educationHeading),
        sectionLines(lines, educationHeading, workExperienceHeading),
        sectionLines(lines, workExperienceHeading, skillsHeading),
        sectionLines(lines, skillsHeading, languagesHeading),
        sectionLines(lines, languagesHeading, referencesHeading));
  }

  boolean isSectionHeading(String value) {
    String normalizedValue = normalizeHeadingKey(value);
    return List.of("profile", "education", "work_experience", "skills", "languages", "references").stream()
        .map(textDetectionCatalogService::candidateCvSectionHeading)
        .map(this::normalizeHeadingKey)
        .anyMatch(normalizedValue::equals);
  }

  private List<String> sectionLines(List<String> lines, String startHeading, String endHeading) {
    int startIndex = findHeadingIndex(lines, startHeading);
    if (startIndex < 0) {
      return List.of();
    }
    int endIndex = endHeading == null ? lines.size() : findHeadingIndexAfter(lines, endHeading, startIndex + 1);
    if (endIndex < 0) {
      endIndex = lines.size();
    }
    return lines.subList(startIndex + 1, endIndex).stream()
        .map(textMatcher::cleanLine)
        .filter(line -> !line.isBlank())
        .toList();
  }

  private int findHeadingIndex(List<String> lines, String heading) {
    String wanted = normalizeHeadingKey(heading);
    for (int index = 0; index < lines.size(); index++) {
      if (normalizeHeadingKey(lines.get(index)).equals(wanted)) {
        return index;
      }
    }
    return -1;
  }

  private int findHeadingIndexAfter(List<String> lines, String heading, int startIndex) {
    String wanted = normalizeHeadingKey(heading);
    for (int index = startIndex; index < lines.size(); index++) {
      if (normalizeHeadingKey(lines.get(index)).equals(wanted)) {
        return index;
      }
    }
    return -1;
  }

  private String normalizeHeadingKey(String value) {
    return safeText(value)
        .toUpperCase(Locale.ROOT)
        .replaceAll("[^\\p{L}]", "");
  }

  record Sections(
      List<String> profileLines,
      List<String> educationLines,
      List<String> workExperienceLines,
      List<String> skillLines,
      List<String> languageLines) {
  }
}
