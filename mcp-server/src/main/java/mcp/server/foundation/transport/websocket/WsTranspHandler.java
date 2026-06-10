package mcp.server.foundation.transport.websocket;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspSignalModel;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;

/**
 * Spring framework entrypoint for WebSocket events.
 *
 * Ansvar:
 * - Ta emot Spring WebSocket callbacks
 * - Delegera direkt till WsTranspAdap
 *
 * Ingen runtime-logik får finnas här.
 */
public final class WsTranspHandler implements WebSocketHandler {

  private final WsTranspAdap adapter;
  private final ServerLogger logger;
  private final ObservCtxFactory obsCtxFactory;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;

  public WsTranspHandler(
      WsTranspAdap adapter,
      ServerLogger logger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    this.adapter = Objects.requireNonNull(adapter);
    this.logger = Objects.requireNonNull(logger, "logger");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    adapter.WsTranspAdapOnOpen(session);
  }

  @Override
  public void handleMessage(WebSocketSession session,
      org.springframework.web.socket.WebSocketMessage<?> message) {

    if (message instanceof TextMessage textMessage) {
      adapter.WsTranspAdapOnMessage(session, textMessage.getPayload());
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    runtimeMetrics.RTMetricsIncrementCounter(
        TranspSignalModel.TransSigTranspErrorsMetricName("websocket"));
    ObservCtx transportContext = session == null
        ? obsCtxFactory.ObservCtxFactoryEmpty()
        : obsCtxFactory.ObservCtxFactoryFromTranspCoordinates(
            "websocket",
            String.valueOf(session.getAttributes().get(WsInbound.ATTR_WS_CONNECTION_ID)),
            String.valueOf(session.getAttributes().get(WsInbound.ATTR_MCP_SESSION_ID)));
    telemetryMetrics.McpTelemIncrementTranspError(
        transportContext,
        "inbound",
        "TRANSPORT_ERROR");
    logger.ServerLogErrorObserved(
        ServerLogger.Component.WS,
        obsCtxFactory.ObservCtxFactoryWithErrType(transportContext, "TRANSPORT_ERROR"),
        "ERROR",
        "WS_TRANSPORT_ERROR",
        "WsTranspHandler: transport error: " + (exception == null ? "unknown" : exception.getMessage()),
        "TRANSPORT_ERROR",
        exception);
    adapter.WsTranspAdapOnClose(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
    adapter.WsTranspAdapOnClose(session);
  }

  @Override
  public boolean supportsPartialMessages() {
    return false;
  }
}
