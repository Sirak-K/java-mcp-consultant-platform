package mcp.server.foundation.tool_interface;

public record ToolExecPolicy(
    long timeoutMillis,
    int maxConcurrency,
    boolean cancellable,
    boolean progressEnabled) {

  public ToolExecPolicy {
    if (timeoutMillis <= 0L) {
      throw new IllegalArgumentException("timeoutMillis must be > 0");
    }

    if (maxConcurrency <= 0) {
      throw new IllegalArgumentException("maxConcurrency must be > 0");
    }
  }
}
