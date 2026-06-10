package mcp.server.foundation.observability.runtime;

import java.util.Objects;

/**
 * Single operational drift check for pre-start or post-start use.
 */
public record OperCheckView(
    String name,
    boolean passed,
    String detail) {

  public OperCheckView {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(detail, "detail");
  }
}
