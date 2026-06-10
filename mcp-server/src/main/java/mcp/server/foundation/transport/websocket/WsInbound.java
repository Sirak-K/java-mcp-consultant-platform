package mcp.server.foundation.transport.websocket;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspSignalModel;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.client_context.session.id.McpSessIdGen;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import mcp.server.foundation.transport.TranspSess;

import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * WsInbound
 *
 * WebSocket boundary adapter.
 *
 * Ansvar:
 * - Skapa TranspSess (dual identity)
 * - Koppla open / message / close till TranspAdap hooks
 *
 * Dual identity:
 * - Physical: WsConnId
 * - Logical: McpSessId
 */
final class WsInbound {

  static final String ATTR_WS_CONNECTION_ID = "wsConnId";
  static final String ATTR_MCP_SESSION_ID = "mcpSessId";

  private static final String MCP_NOT_APPLICABLE = "N/A";
  private static final String RPC_NOT_APPLICABLE = "N/A";

  private final ServerLogger logger;
  private final WsConnReg registry;
  private final McpSessIdGen generator;
  private final ObservCtxFactory obsCtxFactory;
  private final RTMetrics runtimeMetrics;
  private final ReqsAuthBindingPolicy requestAuthBindingPolicy;
  private final McpSessRTMetaFactory runtimeMetaFactory;
  private final WsTranspSettings settings;

  private Consumer<TranspSess> openHandler;
  private Consumer<TranspSess> closeHandler;
  private BiConsumer<TranspSess, String> messageHandler;

  WsInbound(
      ServerLogger logger,
      WsConnReg registry,
      McpSessIdGen generator,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      McpSessRTMetaFactory runtimeMetaFactory,
      WsTranspSettings settings) {

    this.logger = Objects.requireNonNull(logger, "logger");
    this.registry = Objects.requireNonNull(registry, "registry");
    this.generator = Objects.requireNonNull(generator, "generator");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.requestAuthBindingPolicy = Objects.requireNonNull(requestAuthBindingPolicy, "requestAuthBindingPolicy");
    this.runtimeMetaFactory = Objects.requireNonNull(runtimeMetaFactory, "runtimeMetaFactory");
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  // =========================================================
  // Handler wiring
  // =========================================================

  void WsInbSetOpenHandler(Consumer<TranspSess> handler) {
    this.openHandler = Objects.requireNonNull(handler, "handler");
  }

  void WsInbSetMessageHandler(BiConsumer<TranspSess, String> handler) {
    this.messageHandler = Objects.requireNonNull(handler, "handler");
  }

  void WsInbSetCloseHandler(Consumer<TranspSess> handler) {
    this.closeHandler = Objects.requireNonNull(handler, "handler");
  }

  // =========================================================
  // WS Hooks
  // =========================================================

  void WsInbOnOpen(WebSocketSession ws) {

    if (ws == null) {
      return;
    }

    if (registry.WsConnRegGetActiveConnCount() >= settings.maxActiveConnections()) {
      runtimeMetrics.RTMetricsIncrementCounter("ws.capacity.rejected.total");
      try {
        ws.close();
      } catch (IOException ignored) {
        // Best-effort close for overflowing websocket connections.
      }
      return;
    }

    WsConnId connectionId = WsConnId.random();
    McpSessId sessionId = generator.generate();

    Map<String, Object> attrs = ws.getAttributes();
    attrs.put(ATTR_WS_CONNECTION_ID, connectionId);
    attrs.put(ATTR_MCP_SESSION_ID, sessionId);

    TranspSess transportSession = new TranspSess(
        connectionId,
        sessionId,
        OperatingSurface.MCP_DIRECT,
        requestAuthBindingPolicy.ReqsAuthBindingPolicyResolveDirectMcpDefault("websocket_direct_mcp"),
        runtimeMetaFactory);

    registry.WsConnRegRegister(connectionId, transportSession, ws);
    runtimeMetrics.RTMetricsIncrementCounter("ws.connections.opened.total");
    runtimeMetrics.RTMetricsSetGauge("ws.connections.active", registry.WsConnRegGetActiveConnCount());

    ObservCtx context = obsCtxFactory.ObservCtxFactoryFromTranspSess(transportSession);

    logger.ServerLogInfoObserved(
        ServerLogger.Component.WS,
        context,
        "OPEN",
        "WS_CONNECTION_OPENED",
        "WS INBOUND: OPEN");

    logger.ServerLogInfoObserved(
        ServerLogger.Component.WS,
        context,
        "REGISTER",
        "WS_CONNECTION_REGISTERED",
        "WsInbound: transport session registered");

    Consumer<TranspSess> handler = openHandler;

    if (handler != null) {
      try {
        handler.accept(transportSession);
      } catch (Exception ex) {
        logger.ServerLogErrorStructured(
            ServerLogger.Component.WS,
            sessionId.toString(),
            connectionId.toString(),
            RPC_NOT_APPLICABLE,
            "WsInbOnOpen handler failed: " + ex.getMessage(),
            ex);
      }
    }
  }

  void WsInbOnMessage(WebSocketSession ws, String payload) {

    if (ws == null || payload == null) {
      return;
    }

    BiConsumer<TranspSess, String> handler = messageHandler;
    if (handler == null) {
      return;
    }

    WsConnId connectionId = WsInbGetConnId(ws);

    if (connectionId == null) {

      logger.ServerLogWarnStructured(
          ServerLogger.Component.WS,
          MCP_NOT_APPLICABLE,
          ServerLogger.WS_UNBOUND,
          RPC_NOT_APPLICABLE,
          "WsInbOnMessage: missing wsConnId attribute");

      return;
    }

    TranspSess session = registry.WsConnRegGetTranspSess(connectionId);

    if (session == null || !session.TranspSessIsActive()) {

      logger.ServerLogWarnStructured(
          ServerLogger.Component.WS,
          MCP_NOT_APPLICABLE,
          connectionId.toString(),
          RPC_NOT_APPLICABLE,
          "WsInbOnMessage: missing or inactive TranspSess");

      return;
    }

    try {
      runtimeMetrics.RTMetricsIncrementCounter("ws.messages.in.total");
      runtimeMetrics.RTMetricsSetGauge("ws.connections.active", registry.WsConnRegGetActiveConnCount());

      ObservCtx context = obsCtxFactory.ObservCtxFactoryFromTranspSess(session);

      logger.ServerLogInfoObserved(
          ServerLogger.Component.WS,
          context,
          "RECEIVE",
          "WS_MESSAGE_RECEIVED",
          "WsInbound: message received bytes=" + payload.length());

      handler.accept(session, payload);

    } catch (Exception ex) {

      logger.ServerLogErrorStructured(
          ServerLogger.Component.WS,
          session.TranspSessGetMcpSessId(),
          session.TranspSessGetWsConnId(),
          RPC_NOT_APPLICABLE,
          "WsInbOnMessage handler failed: " + ex.getMessage(),
          ex);
    }
  }

  void WsInbOnClose(WebSocketSession ws) {

    if (ws == null) {
      return;
    }

    WsConnId connectionId = WsInbGetConnId(ws);

    TranspSess session = connectionId == null
        ? null
        : registry.WsConnRegUnregister(connectionId);

    WsInbFinalizeClose(session);
  }

  boolean WsInbCloseSessById(String sessionId) {

    if (sessionId == null || sessionId.isBlank()) {
      return false;
    }

    TranspSess session = registry.WsConnRegFindTranspSessByMcpSessId(sessionId);
    if (session == null) {
      return false;
    }

    WebSocketSession webSocketSession = registry.WsConnRegGetWsSess(session.TranspSessGetWsConnIdObject());
    TranspSess removed = registry.WsConnRegUnregisterByMcpSessId(sessionId);

    WsInbFinalizeClose(removed);

    if (webSocketSession != null && webSocketSession.isOpen()) {
      try {
        webSocketSession.close();
      } catch (IOException ex) {
        logger.ServerLogErrorStructured(
            ServerLogger.Component.WS,
            sessionId,
            session.TranspSessGetWsConnId(),
            RPC_NOT_APPLICABLE,
            "WsInbound: forced close failed: " + ex.getMessage(),
            ex);
      }
    }

    return removed != null;
  }

  private void WsInbFinalizeClose(TranspSess session) {

    if (session == null) {
      return;
    }

    session.TranspSessClose();
    runtimeMetrics.RTMetricsIncrementCounter(
        TranspSignalModel.TransSigSessClosedMetricName("websocket"));
    runtimeMetrics.RTMetricsSetGauge("ws.connections.active", registry.WsConnRegGetActiveConnCount());

    ObservCtx context = obsCtxFactory.ObservCtxFactoryFromTranspSess(session);

    logger.ServerLogInfoObserved(
        ServerLogger.Component.WS,
        context,
        "CLOSE",
        "WS_CONNECTION_CLOSED",
        "WS INBOUND: CLOSE");

    logger.ServerLogInfoObserved(
        ServerLogger.Component.WS,
        context,
        "UNREGISTER",
        "WS_CONNECTION_UNREGISTERED",
        "WsInbound: transport session unregistered");

    Consumer<TranspSess> handler = closeHandler;

    if (handler != null) {
      try {
        handler.accept(session);
      } catch (Exception ex) {
        logger.ServerLogErrorStructured(
            ServerLogger.Component.WS,
            session.TranspSessGetMcpSessId(),
            session.TranspSessGetWsConnId(),
            RPC_NOT_APPLICABLE,
            "WsInbOnClose handler failed: " + ex.getMessage(),
            ex);
      }
    }
  }

  // =========================================================
  // Helpers
  // =========================================================

  private WsConnId WsInbGetConnId(WebSocketSession ws) {

    Object raw = ws.getAttributes().get(ATTR_WS_CONNECTION_ID);

    if (raw instanceof WsConnId id) {
      return id;
    }

    if (raw instanceof String s) {
      try {
        return WsConnId.fromString(s);
      } catch (Exception ignored) {
        return null;
      }
    }

    return null;
  }
}
