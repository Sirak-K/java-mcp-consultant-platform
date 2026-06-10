package mcp.server.foundation.transport.websocket;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.server_process.client_context.session.id.McpSessIdGen;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import mcp.server.foundation.transport.TranspSess;

import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * WsRTOrch
 *
 * Runtime orch for WebSocket transport.
 *
 * Ansvar:
 * - Äga WsConnReg + inbound/outbound
 * - Exponera status + kontrolloperationer för WS-transporten
 * - Exponera deterministiskt API för WsTranspAdap
 */
public final class WsRTOrch {

  private final WsConnReg registry;
  private final WsInbound inbound;
  private final WsOutbound outbound;
  private final WsTranspSettings settings;

  public WsRTOrch(
      McpSessIdGen mcpSessIdGenerator,
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      McpSessRTMetaFactory runtimeMetaFactory,
      WsTranspSettings settings) {

    Objects.requireNonNull(mcpSessIdGenerator, "mcpSessIdGenerator");
    Objects.requireNonNull(serverLogger, "serverLogger");
    Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.settings = Objects.requireNonNull(settings, "settings");

    this.registry = new WsConnReg();

    // Canonical ctor-order: (logger, registry, generator)
    this.inbound = new WsInbound(
        serverLogger,
        registry,
        mcpSessIdGenerator,
        obsCtxFactory,
        runtimeMetrics,
        requestAuthBindingPolicy,
        runtimeMetaFactory,
        settings);

    this.outbound = new WsOutbound(
        serverLogger,
        registry,
        obsCtxFactory,
        runtimeMetrics,
        telemetryMetrics,
        inbound::WsInbCloseSessById);
  }

  public WsInbound WsRTOrchGetInb() {
    return inbound;
  }

  public WsOutbound WsRTOrchGetOutb() {
    return outbound;
  }

  // =========================================================
  // Adapter-facing API (WsTranspAdap expects these)
  // =========================================================

  public void WsRTOrchSetMessageHandler(BiConsumer<TranspSess, String> handler) {
    inbound.WsInbSetMessageHandler(handler);
  }

  public void WsRTOrchSetOpenHandler(Consumer<TranspSess> handler) {
    inbound.WsInbSetOpenHandler(handler);
  }

  public void WsRTOrchSetCloseHandler(Consumer<TranspSess> handler) {
    inbound.WsInbSetCloseHandler(handler);
  }

  public void WsRTOrchSendTo(TranspSess session, String message) {
    outbound.WsOutbSendTo(session, message);
  }

  public TranspSess WsRTOrchGetSessById(String sessionId) {
    return registry.WsConnRegFindTranspSessByMcpSessId(sessionId);
  }

  public boolean WsRTOrchCloseSess(String sessionId) {
    return inbound.WsInbCloseSessById(sessionId);
  }

  public void WsRTOrchOnOpen(WebSocketSession ws) {
    inbound.WsInbOnOpen(ws);
  }

  public void WsRTOrchOnMessage(WebSocketSession ws, String payload) {
    inbound.WsInbOnMessage(ws, payload);
  }

  public void WsRTOrchOnClose(WebSocketSession ws) {
    inbound.WsInbOnClose(ws);
  }

  // =========================================================
  // Status/Control
  // =========================================================

  public int WsRTOrchGetActiveWsSessCount() {
    // “Active connection count” = fysiska WS bindningar
    return registry.WsConnRegGetActiveConnCount();
  }

  public WsTranspStatus WsRTOrchGetStatus() {
    return new WsTranspStatus(registry.WsConnRegGetActiveSessCount());
  }

  public void WsRTOrchCloseAll() {
    registry.WsConnRegCloseAll();
  }

  public WsTranspSettings WsRTOrchGetSettings() {
    return settings;
  }
}
