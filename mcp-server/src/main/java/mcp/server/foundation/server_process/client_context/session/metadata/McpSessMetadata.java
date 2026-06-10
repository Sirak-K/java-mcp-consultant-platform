package mcp.server.foundation.server_process.client_context.session.metadata;

import java.time.Instant;

import mcp.server.foundation.server_process.client_context.session.McpSessState;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.transport.websocket.WsConnId;

/**
 * McpSessMetadata
 *
 * Observability DTO.
 *
 * Invariant:
 * - sessionId = logical MCP identity
 * - connectionId = physical WS identity (nullable if unbound)
 */
public record McpSessMetadata(
        McpSessId sessionId,
        WsConnId connectionId,
        McpSessState state,
        Instant createdAt,
        McpSessRTMeta runtimeMeta) {
}
