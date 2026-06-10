package mcp.server.domain.candidate_profiles.application.cv;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class CandidateCvDateRangeParser {

  private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile(
      "(?iu)^(\\d{4})\\s*[\\u2013-]\\s*(p\\u00E5g\\u00E5ende|ongoing|present|nuvarande|\\d{4})(?:\\s*\\([^)]*\\))?$");

  ParsedDateRange parse(String line) {
    Matcher matcher = YEAR_RANGE_PATTERN.matcher(safeText(line).trim());
    if (!matcher.matches()) {
      return ParsedDateRange.empty();
    }
    String startYear = matcher.group(1);
    String endToken = matcher.group(2);
    boolean current = containsCurrentToken(line) || containsCurrentToken(endToken);
    return new ParsedDateRange(
        startYear + "-01-01",
        current ? "" : endToken + "-12-31",
        current);
  }

  private boolean containsCurrentToken(String value) {
    String safeValue = safeText(value).toLowerCase(java.util.Locale.ROOT);
    return safeValue.contains("p\u00E5g\u00E5ende")
        || safeValue.contains("ongoing")
        || safeValue.contains("present")
        || safeValue.contains("nuvarande");
  }

  record ParsedDateRange(String startDate, String endDate, boolean current) {
    static ParsedDateRange empty() {
      return new ParsedDateRange("", "", false);
    }

    boolean isEmpty() {
      return startDate.isBlank() && endDate.isBlank() && !current;
    }
  }
}
