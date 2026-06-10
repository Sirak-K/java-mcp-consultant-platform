package mcp.server.foundation.observability.diagnostics;

/**
 * Active tool invocation snapshot for diagnostics.
 */
public record ActiveToolInvocView(
    String requestId,
    String toolName,
    long timeoutMillis,
    boolean cancellable,
    boolean progressEnabled) {
}
