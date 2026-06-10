package mcp.server.foundation.transport.websocket;

import mcp.server.foundation.transport.TranspSess;
import mcp.server.foundation.transport.TranspSessBindRegSupport;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WsConnReg
 *
 * Ansvar:
 * - Hålla mapping: WsConnId -> WebSocketSession (Spring) +
 * TranspSess
 * - Exponera lookup + diagnostik
 *
 * NOTE:
 * - “Connection” = fysisk WS-identitet
 * - “Session” = MCP-logisk identitet (TranspSess innehåller båda)
 */
public final class WsConnReg {

  private final TranspSessBindRegSupport<WsConnId, TranspSess> transportBindings =
      new TranspSessBindRegSupport<>(
          TranspSess::TranspSessGetWsConnIdObject,
          TranspSess::TranspSessGetMcpSessId);
  private final Map<WsConnId, WebSocketSession> webSocketSessionsByWsConnId = new ConcurrentHashMap<>();

  // =========================================================
  // Register / Unregister
  // =========================================================

  public void WsConnRegRegister(
      WsConnId connectionId,
      TranspSess transportSession,
      WebSocketSession webSocketSession) {

    Objects.requireNonNull(connectionId, "connectionId");
    Objects.requireNonNull(webSocketSession, "webSocketSession");
    Objects.requireNonNull(transportSession, "transportSession");

    String mcpSessId = transportSession.TranspSessGetMcpSessId();
    if (mcpSessId == null || mcpSessId.isBlank()) {
      throw new IllegalArgumentException("transportSession.mcpSessId must not be null/blank");
    }

    // Atomic konsistens mellan index:
    // - wsConnId -> sessions (2 mappar)
    // - mcpSessId -> transportSession (1 map)
    synchronized (this) {
      transportBindings.register(transportSession);
      webSocketSessionsByWsConnId.put(connectionId, webSocketSession);
    }
  }

  public TranspSess WsConnRegUnregister(WsConnId connectionId) {

    if (connectionId == null) {
      return null;
    }

    synchronized (this) {

      webSocketSessionsByWsConnId.remove(connectionId);
      return transportBindings.unregisterByConnId(connectionId);
    }
  }

  public TranspSess WsConnRegUnregisterByMcpSessId(String mcpSessId) {

    if (mcpSessId == null || mcpSessId.isBlank()) {
      return null;
    }

    synchronized (this) {
      TranspSess session = transportBindings.getByLogicalSessId(mcpSessId);
      if (session == null) {
        return null;
      }

      return WsConnRegUnregister(session.TranspSessGetWsConnIdObject());
    }
  }

  // =========================================================
  // Lookup
  // =========================================================

  public WebSocketSession WsConnRegGetWsSess(WsConnId connectionId) {

    if (connectionId == null) {
      return null;
    }

    return webSocketSessionsByWsConnId.get(connectionId);
  }

  public TranspSess WsConnRegGetTranspSess(WsConnId connectionId) {

    if (connectionId == null) {
      return null;
    }

    return transportBindings.getByConnId(connectionId);
  }

  /**
   * Lookup TranspSess via MCP sessionId (String) — O(1).
   */
  public TranspSess WsConnRegFindTranspSessByMcpSessId(String mcpSessId) {

    if (mcpSessId == null || mcpSessId.isBlank()) {
      return null;
    }

    return transportBindings.getByLogicalSessId(mcpSessId);
  }

  // =========================================================
  // Status / Diagnostik
  // =========================================================

  public int WsConnRegGetActiveConnCount() {
    return webSocketSessionsByWsConnId.size();
  }

  /**
   * “Active session count” = antalet TranspSess-entries (ws-index).
   */
  public int WsConnRegGetActiveSessCount() {
    return transportBindings.getActiveBindingCount();
  }

  // =========================================================
  // Clear
  // =========================================================

  public void WsConnRegCloseAll() {
    synchronized (this) {
      transportBindings.clearAll();
      webSocketSessionsByWsConnId.clear();
    }
  }
}
