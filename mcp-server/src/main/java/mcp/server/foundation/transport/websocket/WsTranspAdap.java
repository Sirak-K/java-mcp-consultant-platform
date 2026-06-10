package mcp.server.foundation.transport.websocket;

import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.TranspCapacityProfile;
import mcp.server.foundation.transport.TranspSess;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * WsTranspAdap
 *
 * Adapter är identity-neutral.
 * Den arbetar endast med TranspSess.
 *
 * HARDENING:
 * - wsConnId normaliseras alltid innan vidareflöde
 * - Ingen null/blank identity får passera transportgränsen
 */
public final class WsTranspAdap implements TranspAdap {

  private static final String TRANSPORT_NAME = "websocket";

  private final WsRTOrch runtime;

  public WsTranspAdap(WsRTOrch runtime) {
    this.runtime = runtime;
  }

  // =========================================================
  // Lifecycle
  // =========================================================

  @Override
  public String TranspAdapGetTranspName() {
    return TRANSPORT_NAME;
  }

  @Override
  public void TranspAdapStart() {
    // no-op
  }

  @Override
  public void TranspAdapStop() {
    runtime.WsRTOrchCloseAll();
  }

  // =========================================================
  // Messaging
  // =========================================================

  @Override
  public void TranspAdapSetMessageHandler(
      BiConsumer<TranspSess, String> handler) {
    runtime.WsRTOrchSetMessageHandler(handler);
  }

  @Override
  public void TranspAdapSetSessionOpenHandler(
      Consumer<TranspSess> handler) {
    runtime.WsRTOrchSetOpenHandler(handler);
  }

  @Override
  public void TranspAdapSetSessionCloseHandler(
      Consumer<TranspSess> handler) {
    runtime.WsRTOrchSetCloseHandler(handler);
  }

  @Override
  public void TranspAdapSendTo(
      TranspSess session,
      String message) {

    if (session == null) {
      throw new IllegalArgumentException("TranspSess must not be null");
    }

    runtime.WsRTOrchSendTo(session, message);
  }

  @Override
  public TranspSess TranspAdapGetSessionById(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return null;
    }
    return runtime.WsRTOrchGetSessById(sessionId);
  }

  @Override
  public boolean TranspAdapCloseSessById(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return false;
    }
    return runtime.WsRTOrchCloseSess(sessionId);
  }

  @Override
  public Map<String, Long> TranspAdapDescribeCapacityProfile() {
    return TranspCapacityProfile.TransCapProfileWs(
        runtime.WsRTOrchGetSettings().maxActiveConnections());
  }

  public int WSTrGetActiveWsSessCount() {
    return runtime.WsRTOrchGetActiveWsSessCount();
  }

  void WsTranspAdapOnOpen(WebSocketSession ws) {
    runtime.WsRTOrchOnOpen(ws);
  }

  void WsTranspAdapOnMessage(WebSocketSession ws, String payload) {
    runtime.WsRTOrchOnMessage(ws, payload);
  }

  void WsTranspAdapOnClose(WebSocketSession ws) {
    runtime.WsRTOrchOnClose(ws);
  }
}
