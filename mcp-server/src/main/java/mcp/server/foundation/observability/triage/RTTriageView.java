package mcp.server.foundation.observability.triage;

import java.util.List;
import java.util.Objects;

/**
 * Top-level operational triage payload.
 */
public record RTTriageView(
    List<TriageSymptomView> symptoms) {

  public RTTriageView {
    symptoms = List.copyOf(Objects.requireNonNull(symptoms, "symptoms"));
  }
}
