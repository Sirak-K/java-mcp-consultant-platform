package mcp.server.foundation.observability.health;

import java.util.Map;
import java.util.Objects;

/**
 * Canonical health response for operational runtime checks.
 */
public record RTHealthView(
    String status,
    boolean live,
    boolean ready,
    String runtimeState,
    String transport,
    Map<String, HealthCheckView> checks) {

  public RTHealthView {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(runtimeState, "runtimeState");
    Objects.requireNonNull(transport, "transport");
    checks = Map.copyOf(Objects.requireNonNull(checks, "checks"));
  }
}
