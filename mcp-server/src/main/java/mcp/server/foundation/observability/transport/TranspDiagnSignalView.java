package mcp.server.foundation.observability.transport;

import java.util.Objects;

/**
 * Canonical transport-level diagnostic signal mirrored across runtime,
 * diagnostics and triage views.
 */
public record TranspDiagnSignalView(
    String name,
    String transportFamily,
    long value,
    boolean observed,
    String detail) {

  public TranspDiagnSignalView {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(transportFamily, "transportFamily");
    Objects.requireNonNull(detail, "detail");
  }
}
