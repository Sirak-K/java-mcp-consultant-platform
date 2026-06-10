package mcp.server.foundation.observability.diagnostics;

/**
 * Persisted runtime binding snapshot for operations diagnostics.
 */
public record RuntimeBindingDiagnosticView(
    String tenantType,
    String activeTenantId,
    String bindingStage,
    boolean platformSystem) {
}
