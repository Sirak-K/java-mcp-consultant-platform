package mcp.server.domain.candidate_profiles.application.cv;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class CandidateCvTextMatcher {

  List<String> normalizedLines(String sourceText) {
    return safeText(sourceText).lines()
        .map(this::cleanLine)
        .filter(line -> !line.isBlank())
        .toList();
  }

  String cleanLine(String line) {
    String cleaned = safeText(line)
        .replace('\u00A0', ' ')
        .replaceAll("\\s+", " ")
        .trim();
    cleaned = cleaned.replaceFirst("^[^\\p{L}\\p{N}]+\\s*", "");
    cleaned = cleaned.replaceFirst("^[_-]{3,}\\s*", "");
    return cleaned;
  }

  String joinLines(List<String> lines) {
    return lines.stream()
        .map(this::cleanLine)
        .filter(line -> !line.isBlank())
        .collect(Collectors.joining(" "))
        .replaceAll("\\s{2,}", " ")
        .trim();
  }

  String firstMatch(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(safeText(text));
    if (!matcher.find()) {
      return "";
    }
    return matcher.group().replaceAll("\\s{2,}", " ").trim();
  }

  boolean containsTerm(String text, String term) {
    String safeTerm = safeText(term).trim();
    if (safeTerm.isBlank()) {
      return false;
    }
    if (safeTerm.length() <= 2 && safeTerm.matches("[A-Za-z]{1,2}")) {
      return false;
    }
    Pattern pattern = Pattern.compile(
        "(?iu)(^|[^\\p{L}\\p{N}])" + Pattern.quote(safeTerm) + "($|[^\\p{L}\\p{N}])");
    return pattern.matcher(safeText(text)).find();
  }

  boolean containsCatalogAlias(String text, String term) {
    String safeTerm = safeText(term).trim();
    if (safeTerm.isBlank()) {
      return false;
    }
    Pattern pattern = Pattern.compile(
        "(?iu)(^|[^\\p{L}\\p{N}])" + Pattern.quote(safeTerm) + "($|[^\\p{L}\\p{N}])");
    return pattern.matcher(safeText(text)).find();
  }

  boolean containsIgnoreCase(String text, String value) {
    return safeText(text).toLowerCase(Locale.ROOT)
        .contains(safeText(value).toLowerCase(Locale.ROOT));
  }

  boolean containsAnyIgnoreCase(String text, List<String> terms) {
    return terms.stream()
        .anyMatch(term -> containsIgnoreCase(text, term));
  }

  int countDigits(String value) {
    int digits = 0;
    for (char current : safeText(value).toCharArray()) {
      if (Character.isDigit(current)) {
        digits++;
      }
    }
    return digits;
  }

  String toNameCase(String rawName) {
    return java.util.Arrays.stream(safeText(rawName).trim().split("\\s+"))
        .map(part -> part.isBlank()
            ? part
            : part.substring(0, 1).toUpperCase(Locale.ROOT)
                + part.substring(1).toLowerCase(Locale.ROOT))
        .collect(Collectors.joining(" "));
  }
}
