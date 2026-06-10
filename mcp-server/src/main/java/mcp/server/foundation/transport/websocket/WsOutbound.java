package mcp.server.foundation.transport.websocket;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.transport.TranspOutbTelemSupport;
import mcp.server.foundation.transport.TranspSess;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;
import java.util.function.Function;

/**
 * WsOutbound
 *
 * Ontologi: Transp Egress Adapter (WS).
 *
 * Ansvar:
 * - Skicka outbound payload via PHYSICAL ws-connection identity
 * (WsConnId).
 */
final class WsOutbound {

  private static final String MCP_NOT_APPLICABLE = "N/A";
  private static final String RPC_NOT_APPLICABLE = "N/A";

  private final ServerLogger logger;
  private final WsConnReg registry;
  private final ObservCtxFactory obsCtxFactory;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;
  private final Function<String, Boolean> closeSessionById;

  WsOutbound(
      ServerLogger logger,
      WsConnReg registry,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      Function<String, Boolean> closeSessionById) {

    this.logger = Objects.requireNonNull(logger, "logger");
    this.registry = Objects.requireNonNull(registry, "registry");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
    this.closeSessionById = Objects.requireNonNull(closeSessionById, "closeSessionById");
  }

  void WsOutbSendTo(
      TranspSess session,
      String message) {

    if (session == null) {
      logger.ServerLogWarnStructured(
          ServerLogger.Component.WS,
          MCP_NOT_APPLICABLE,
          ServerLogger.WS_UNBOUND,
          RPC_NOT_APPLICABLE,
          "WsOutbound: session null");
      return;
    }

    // CRITICAL: WS lookup is PHYSICAL (wsConnId), not mcpSessId
    WebSocketSession ws = registry.WsConnRegGetWsSess(
        session.TranspSessGetWsConnIdObject());

    if (ws == null || !ws.isOpen()) {
      logger.ServerLogWarnStructured(
          ServerLogger.Component.WS,
          session.TranspSessGetMcpSessId(),
          session.TranspSessGetWsConnId(),
          RPC_NOT_APPLICABLE,
          "WsOutbound: WS not open for wsConnId="
              + session.TranspSessGetWsConnId()
              + " mcpSessId=" + session.TranspSessGetMcpSessId());
      closeSessionById.apply(session.TranspSessGetMcpSessId());
      return;
    }

    if (message == null) {
      logger.ServerLogWarnStructured(
          ServerLogger.Component.WS,
          session.TranspSessGetMcpSessId(),
          session.TranspSessGetWsConnId(),
          RPC_NOT_APPLICABLE,
          "WsOutbound: message null");
      return;
    }

    try {
      long sendStartedAt = System.nanoTime();
      ObservCtx context = obsCtxFactory.ObservCtxFactoryFromTranspSess(session);
      ws.sendMessage(new TextMessage(message));
      TranspOutbTelemSupport.TranspOutbTelemRecordSent(
          runtimeMetrics,
          "websocket",
          "ws.transport.outbound.duration",
          sendStartedAt,
          session,
          message);
      logger.ServerLogInfoObserved(
          ServerLogger.Component.WS,
          context,
          "SEND",
          "WS_MESSAGE_SENT",
          "WsOutbound: message sent bytes=" + message.length());
    } catch (Exception ex) {
      TranspOutbTelemSupport.TranspOutbTelemRecordErr(
          runtimeMetrics,
          telemetryMetrics,
          obsCtxFactory.ObservCtxFactoryFromTranspSess(session),
          "websocket");
      closeSessionById.apply(session.TranspSessGetMcpSessId());
      logger.ServerLogErrorStructured(
          ServerLogger.Component.WS,
          session.TranspSessGetMcpSessId(),
          session.TranspSessGetWsConnId(),
          RPC_NOT_APPLICABLE,
          "WsOutbound: send failed: " + ex.getMessage(),
          ex);
    }
  }
}
