package mcp.server.foundation.observability.runtime;

/**
 * Per-tool concurrency and execution policy view.
 */
public record ToolConcurrView(
    String toolName,
    String officialToolName,
    String officialToolFamily,
    String officialAction,
    boolean officialToolSurface,
    long timeoutMillis,
    int maxConcurrency,
    boolean cancellable,
    boolean progressEnabled,
    int activeExecutions) {
}
