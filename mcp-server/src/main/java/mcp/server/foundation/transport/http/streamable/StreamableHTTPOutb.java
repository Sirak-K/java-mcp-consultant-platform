package mcp.server.foundation.transport.http.streamable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.transport.TranspOutbTelemSupport;
import mcp.server.foundation.transport.TranspSess;
import mcp.server.foundation.transport.http.shared.HTTPTranspCfg;

import java.util.Objects;

final class StreamableHTTPOutb {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String RPC_NOT_APPLICABLE = "N/A";

  private final StreamableHTTPRTOrch runtime;
  private final ServerLogger logger;
  private final ObservCtxFactory obsCtxFactory;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;

  StreamableHTTPOutb(
      StreamableHTTPRTOrch runtime,
      ServerLogger logger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    this.runtime = Objects.requireNonNull(runtime, "runtime");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
  }

  void StrHttpOutSendTo(
      TranspSess session,
      String message) {

    if (session == null || message == null || message.isBlank()) {
      return;
    }

    try {
      JsonNode root = MAPPER.readTree(message);
      JsonNode idNode = root.get("id");

      if (idNode != null && !idNode.isNull()) {
        String correlationId = idNode.isTextual() || idNode.isNumber()
            ? idNode.asText()
            : idNode.toString();

        boolean completed = runtime.StreamableHTTPRTCompletePendingResponse(session, message, correlationId);
        if (completed) {
          TranspOutbTelemSupport.TranspOutbTelemRecordSent(
              runtimeMetrics,
              HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP);
          logger.ServerLogInfoObserved(
              ServerLogger.Component.RUNTIME,
              obsCtxFactory.ObservCtxFactoryFromTranspSess(session),
              "SEND",
              "HTTP_RESPONSE_SENT",
              "StreamableHTTPOutb: HTTP response sent");
        }
        return;
      }

      runtime.StreamableHTTPRTPublishNotification(session, message);
    } catch (Exception ex) {
      TranspOutbTelemSupport.TranspOutbTelemRecordErr(
          runtimeMetrics,
          telemetryMetrics,
          obsCtxFactory.ObservCtxFactoryFromTranspSess(session),
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP);
      logger.ServerLogErrorStructured(
          ServerLogger.Component.RUNTIME,
          session.TranspSessGetMcpSessId(),
          session.TranspSessGetTranspConnId(),
          RPC_NOT_APPLICABLE,
          "StreamableHTTPOutb: send failed: " + ex.getMessage(),
          ex);
    }
  }
}
