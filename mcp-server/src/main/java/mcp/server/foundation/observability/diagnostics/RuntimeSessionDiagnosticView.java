package mcp.server.foundation.observability.diagnostics;

import java.time.Instant;

/**
 * Persisted runtime session snapshot for operations diagnostics.
 */
public record RuntimeSessionDiagnosticView(
    String sessionId,
    String tenantType,
    String activeTenantId,
    String bindingStage,
    Instant createdAt,
    Instant lastActivityAt) {
}
