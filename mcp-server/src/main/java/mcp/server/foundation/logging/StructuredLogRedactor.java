package mcp.server.foundation.logging;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Central redaction pass for serialized structured logs.
 */
public final class StructuredLogRedactor {

  private final List<Rule> rules = List.of(
      new Rule(Pattern.compile("(?i)(authorization\\s*[=:]\\s*[A-Za-z]+\\s+)([A-Za-z0-9._\\-+/=]+)"), "$1[REDACTED]"),
      new Rule(Pattern.compile("(?i)(Bearer\\s+)([A-Za-z0-9._\\-+/=]+)"), "$1[REDACTED]"),
      new Rule(Pattern.compile("(?i)(bearer-token\\s*[=:]\\s*)([^,\\]\\}\"|\\s]+)"), "$1[REDACTED]"),
      new Rule(Pattern.compile("(?i)(cookie\\s*[=:]\\s*)([^,\\]\\}\"|\\s]+)"), "$1[REDACTED]"),
      new Rule(Pattern.compile("(?i)(set-cookie\\s*[=:]\\s*)([^,\\]\\}\"|\\s]+)"), "$1[REDACTED]"),
      new Rule(Pattern.compile("(?i)(api[-_ ]?key\\s*[=:]\\s*)([^,\\]\\}\"|\\s]+)"), "$1[REDACTED]"),
      new Rule(Pattern.compile("(?i)(secret\\s*[=:]\\s*)([^,\\]\\}\"|\\s]+)"), "$1[REDACTED]"),
      new Rule(Pattern.compile("(?i)(password\\s*[=:]\\s*)([^,\\]\\}\"|\\s]+)"), "$1[REDACTED]"),
      new Rule(Pattern.compile("(?i)(token\\s*[=:]\\s*)([^,\\]\\}\"|\\s]+)"), "$1[REDACTED]"),
      new Rule(Pattern.compile("(?i)((?:jdbc:[A-Za-z0-9+\\-.]+://|[A-Za-z][A-Za-z0-9+\\-.]*://)[^:\\s]+:)([^@\\s]+)(@)"), "$1[REDACTED]$3"),
      new Rule(Pattern.compile("(?s)-----BEGIN [A-Z ]*PRIVATE KEY-----.*?-----END [A-Z ]*PRIVATE KEY-----"), "[REDACTED_PRIVATE_KEY]"),
      new Rule(Pattern.compile("(?i)(\"MCP_SECURITY_BEARER_TOKEN\"\\s*:\\s*\")([^\"]+)(\")"), "$1[REDACTED]$3"),
      new Rule(Pattern.compile("(?i)(\"MCP_DB_PASSWORD\"\\s*:\\s*\")([^\"]+)(\")"), "$1[REDACTED]$3"),
      new Rule(Pattern.compile("(?i)(\"cookie\"\\s*:\\s*\")([^\"]+)(\")"), "$1[REDACTED]$3"),
      new Rule(Pattern.compile("(?i)(\"setCookie\"\\s*:\\s*\")([^\"]+)(\")"), "$1[REDACTED]$3"),
      new Rule(Pattern.compile("(?i)(\"apiKey\"\\s*:\\s*\")([^\"]+)(\")"), "$1[REDACTED]$3"),
      new Rule(Pattern.compile("(?i)(\"secret\"\\s*:\\s*\")([^\"]+)(\")"), "$1[REDACTED]$3"),
      new Rule(Pattern.compile("(?i)(\"password\"\\s*:\\s*\")([^\"]+)(\")"), "$1[REDACTED]$3"),
      new Rule(Pattern.compile("(?i)(\"authorization\"\\s*:\\s*\")([^\"]+)(\")"), "$1[REDACTED]$3"),
      new Rule(Pattern.compile("(?i)(\"bearerToken\"\\s*:\\s*\")([^\"]+)(\")"), "$1[REDACTED]$3"));

  public String StructuredLogRedact(String jsonLine) {
    Objects.requireNonNull(jsonLine, "jsonLine");

    String redacted = jsonLine;
    for (Rule rule : rules) {
      redacted = rule.pattern().matcher(redacted).replaceAll(rule.replacement());
    }
    return redacted;
  }

  private record Rule(Pattern pattern, String replacement) {
  }
}
