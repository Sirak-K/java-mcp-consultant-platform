package mcp.server.foundation.observability.runtime;

/**
 * Aggregated failure profile for a tool.
 */
public record ToolFailureView(
    String toolName,
    String officialToolName,
    String officialToolFamily,
    String officialAction,
    boolean officialToolSurface,
    long totalFailures,
    long errorCount,
    long timeoutCount,
    long rejectionCount,
    long cancellationCount) {
}
