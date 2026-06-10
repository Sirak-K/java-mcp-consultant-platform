package mcp.server.foundation.observability.triage;

import java.util.Objects;

/**
 * Single observed or supporting signal for a triage symptom.
 */
public record TriageSignalView(
    String source,
    String name,
    boolean observed,
    String detail) {

  public TriageSignalView {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(detail, "detail");
  }
}
