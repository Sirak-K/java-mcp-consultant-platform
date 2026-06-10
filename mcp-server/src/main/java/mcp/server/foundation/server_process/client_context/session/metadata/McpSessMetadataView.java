package mcp.server.foundation.server_process.client_context.session.metadata;

import java.time.Instant;

/**
 * McpSessMetadataView
 *
 * REST-safe view model (primitive fields only).
 *
 * Syfte:
 * - Undvik att exponera value objects (McpSessId/WsConnId) direkt till
 * Jackson.
 * - Stabil JSON-kontrakt för observability endpoint.
 */
public record McpSessMetadataView(
        String sessionId,
        String wsConnId,
        String state,
        Instant createdAt,
        String runtimeSessionTypeId,
        String runtimeSessionPhaseId,
        Long runtimeSessionVersion,
        Long runtimeInactivityTtlSeconds,
        String runtimeResumeCapabilityId,
        String runtimeActiveTenantId,
        Boolean runtimeDurableTarget,
        Boolean runtimeResumeSupported,
        Boolean runtimeRequiresActiveTenant) {
}
