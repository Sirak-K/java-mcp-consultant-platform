package mcp.server.foundation.observability.runtime;

import java.util.List;
import java.util.Objects;

/**
 * Operational checklist view for pre-start and post-start checks.
 */
public record OperChecksView(
    List<OperCheckView> preStart,
    List<OperCheckView> postStart) {

  public OperChecksView {
    preStart = List.copyOf(Objects.requireNonNull(preStart, "preStart"));
    postStart = List.copyOf(Objects.requireNonNull(postStart, "postStart"));
  }
}
