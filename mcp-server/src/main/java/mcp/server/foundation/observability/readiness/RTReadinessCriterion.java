package mcp.server.foundation.observability.readiness;

import java.util.List;
import java.util.Objects;

/**
 * One must-pass or optional readiness criterion.
 */
public record RTReadinessCriterion(
    String key,
    String label,
    boolean required,
    RTReadinessStatus status,
    List<RTReadinessEvidence> evidence) {

  public RTReadinessCriterion {
    key = Objects.requireNonNull(key, "key");
    label = Objects.requireNonNull(label, "label");
    status = Objects.requireNonNull(status, "status");
    evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
  }

  public boolean passed() {
    return status == RTReadinessStatus.PASS;
  }
}
