package mcp.server.foundation.observability.diagnostics;

import java.time.Instant;

/**
 * Active logical session snapshot for diagnostics.
 */
public record SessDiagnView(
    String mcpSessId,
    String state,
    Instant createdAt) {
}
