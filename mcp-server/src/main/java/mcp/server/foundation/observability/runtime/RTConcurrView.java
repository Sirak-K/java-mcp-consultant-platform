package mcp.server.foundation.observability.runtime;

import java.util.List;
import java.util.Objects;

/**
 * Shared concurrency view for runtime and diagnostics surfaces.
 */
public record RTConcurrView(
    int globalMaxConcurrency,
    int globalAvailablePermits,
    List<ToolConcurrView> tools) {

  public RTConcurrView {
    tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
  }
}
