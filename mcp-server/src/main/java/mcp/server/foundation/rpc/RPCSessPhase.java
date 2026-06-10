package mcp.server.foundation.rpc;

/**
 * RPCSessPhase
 *
 * Transp-agnostisk, protokoll-nivå fasmodell.
 *
 * Viktigt:
 * - Får ej bero på domain.
 * - Får ej bero på transport.
 * - Representerar RPC-gating-nivå.
 *
 * Coverage:
 * - Logical MCP Session + Routing Policy Layer
 *
 * PRE_INIT:
 * Session existerar men handshake ej färdig.
 *
 * POST_INIT:
 * Handshake klar. Full tool access tillåten.
 *
 * CLOSED:
 * Session ej längre giltig för RPC.
 */
public enum RPCSessPhase {

  PRE_INIT,
  POST_INIT,
  CLOSED
}
