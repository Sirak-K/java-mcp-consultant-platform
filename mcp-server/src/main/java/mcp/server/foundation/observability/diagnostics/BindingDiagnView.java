package mcp.server.foundation.observability.diagnostics;

/**
 * Logical-to-transport binding snapshot.
 */
public record BindingDiagnView(
    String mcpSessId,
    String transportName,
    String transportConnectionId) {
}
