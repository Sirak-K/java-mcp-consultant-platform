package mcp.server.domain.candidate_profiles.application.cv;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
final class CandidateCvContactTextDetector {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b");
  private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d\\s().-]{6,}\\d)(?!\\d)");

  private final CandidateCvTextMatcher textMatcher;
  private final CandidateCvSectionExtractor sectionExtractor;
  private final CandidateCvDateRangeParser dateRangeParser;

  CandidateCvContactTextDetector(
      CandidateCvTextMatcher textMatcher,
      CandidateCvSectionExtractor sectionExtractor,
      CandidateCvDateRangeParser dateRangeParser) {
    this.textMatcher = textMatcher;
    this.sectionExtractor = sectionExtractor;
    this.dateRangeParser = dateRangeParser;
  }

  ContactInfo detect(List<String> lines, String extractedText) {
    String contactEmail = textMatcher.firstMatch(EMAIL_PATTERN, extractedText).toLowerCase(Locale.ROOT);
    String phoneNumber = detectPhoneNumber(extractedText);
    String[] nameParts = splitName(detectNameLine(lines, contactEmail));
    return new ContactInfo(contactEmail, phoneNumber, nameParts[0], nameParts[1]);
  }

  private String detectPhoneNumber(String extractedText) {
    Matcher matcher = PHONE_PATTERN.matcher(extractedText);
    while (matcher.find()) {
      String candidate = textMatcher.cleanLine(matcher.group()).replaceAll("\\s{2,}", " ").trim();
      int digitCount = textMatcher.countDigits(candidate);
      if (digitCount < 8) {
        continue;
      }
      if (!dateRangeParser.parse(candidate).isEmpty()) {
        continue;
      }
      return candidate;
    }
    return "";
  }

  private String[] splitName(String fullName) {
    String trimmed = mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText(fullName).trim();
    if (trimmed.isBlank()) {
      return new String[] { "", "" };
    }
    String[] parts = trimmed.split("\\s+");
    if (parts.length == 1) {
      return new String[] { parts[0], "" };
    }
    return new String[] { parts[0], parts[parts.length - 1] };
  }

  private String detectNameLine(List<String> lines, String contactEmail) {
    for (int index = lines.size() - 1; index >= 0; index--) {
      String line = lines.get(index);
      String headingKey = line.toUpperCase(Locale.ROOT).replaceAll("[^\\p{L}]", "");
      if (headingKey.isBlank()
          || sectionExtractor.isSectionHeading(line)
          || headingKey.equals("MINPROFIL")
          || headingKey.equals("PROFIL")
          || line.contains("@")
          || (!contactEmail.isBlank() && line.equalsIgnoreCase(contactEmail))
          || textMatcher.countDigits(line) > 0) {
        continue;
      }
      if (line.matches("[\\p{Lu}\\u00C5\\u00C4\\u00D6][\\p{Lu}\\u00C5\\u00C4\\u00D6'\\- ]{4,}")) {
        return textMatcher.toNameCase(line);
      }
    }
    return "";
  }

  record ContactInfo(String contactEmail, String phoneNumber, String firstName, String lastName) {
  }
}
