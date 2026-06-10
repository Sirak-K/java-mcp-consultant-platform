package mcp.server.foundation.observability.readiness;

import java.util.Objects;

/**
 * Short evidence snippet attached to a readiness criterion.
 */
public record RTReadinessEvidence(String source, String detail) {

  public RTReadinessEvidence {
    source = Objects.requireNonNull(source, "source");
    detail = sanitize(Objects.requireNonNull(detail, "detail"));
  }

  public static RTReadinessEvidence of(String source, Object detail) {
    return new RTReadinessEvidence(source, Objects.toString(detail, "<null>"));
  }

  private static String sanitize(String detail) {
    final String trimmed = detail.trim();
    if (trimmed.length() <= 220) {
      return trimmed;
    }
    return trimmed.substring(0, 220) + "...";
  }
}
