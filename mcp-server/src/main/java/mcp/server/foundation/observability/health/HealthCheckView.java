package mcp.server.foundation.observability.health;

import java.util.Objects;

/**
 * Canonical health check view for a single runtime dependency.
 */
public record HealthCheckView(
    String status,
    String detail) {

  public HealthCheckView {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(detail, "detail");
  }
}
