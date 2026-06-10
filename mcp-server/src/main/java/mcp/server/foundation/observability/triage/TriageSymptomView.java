package mcp.server.foundation.observability.triage;

import java.util.List;
import java.util.Objects;

/**
 * Canonical symptom-to-signal mapping for operational triage.
 */
public record TriageSymptomView(
    String symptom,
    boolean active,
    String operatorHint,
    List<TriageSignalView> signals) {

  public TriageSymptomView {
    Objects.requireNonNull(symptom, "symptom");
    Objects.requireNonNull(operatorHint, "operatorHint");
    signals = List.copyOf(Objects.requireNonNull(signals, "signals"));
  }
}
