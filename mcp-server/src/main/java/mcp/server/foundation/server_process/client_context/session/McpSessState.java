package mcp.server.foundation.server_process.client_context.session;

/**
 * McpSessState
 *
 * Logical lifecycle state för en MCP-session.
 *
 * Viktigt:
 * - Representerar endast LOGICAL session lifecycle.
 * - Får inte spegla transportstatus direkt.
 * - Transpstatus hanteras separat i registry (transportLost).
 *
 * Coverage:
 * Logical MCP Session + State
 *
 * State machine:
 *
 * CONNECTED
 * ↓
 * INITIALIZED
 * ↓
 * CLOSED (terminal)
 *
 * Invariants:
 *
 * 1. CONNECTED
 * - Session skapad via transport open
 * - MCP handshake ej klar
 * - motsvarar RPCSessPhase.PRE_INIT
 *
 * 2. INITIALIZED
 * - MCP initialize RPC lyckades
 * - Full RPC access tillåten
 * - motsvarar RPCSessPhase.POST_INIT
 *
 * 3. CLOSED
 * - Terminal state
 * - Session får inte återöppnas
 *
 * Viktig separation:
 *
 * Logical lifecycle
 * ≠
 * Physical transport lifecycle
 *
 * Transp close markeras istället via:
 *
 * registry.transportLostIds
 */
public enum McpSessState {

  /**
   * Session skapad via transport open.
   *
   * Handshake ej färdig.
   *
   * RPC tillåtet enligt RPCMetName pre-init policy.
   */
  CONNECTED,

  /**
   * Handshake färdig.
   *
   * Session fullt aktiv.
   */
  INITIALIZED,

  /**
   * Terminal state.
   *
   * Session permanent stängd.
   */
  CLOSED
}
