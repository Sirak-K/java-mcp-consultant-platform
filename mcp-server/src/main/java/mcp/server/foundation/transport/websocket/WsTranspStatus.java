package mcp.server.foundation.transport.websocket;

/**
 * WsTranspStatus
 *
 * DTO returned by RTStatusCtrl.
 *
 * Invariant:
 * - sessionCount represents active physical WebSocket sessions (server truth).
 */
public record WsTranspStatus(int sessionCount) {
}