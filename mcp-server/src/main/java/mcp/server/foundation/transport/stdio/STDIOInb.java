package mcp.server.foundation.transport.stdio;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspSignalModel;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.client_context.session.id.McpSessIdGen;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import mcp.server.foundation.transport.TranspSess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class STDIOInb {

  private static final String TRANSPORT_NAME = "stdio";
  private static final String TRANSPORT_CONNECTION_ID = "stdio:process";
  private static final String RPC_NOT_APPLICABLE = "N/A";

  private final InputStream inputStream;
  private final ServerLogger logger;
  private final McpSessIdGen sessionIdGenerator;
  private final ObservCtxFactory obsCtxFactory;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;
  private final ReqsAuthBindingPolicy requestAuthBindingPolicy;
  private final McpSessRTMetaFactory runtimeMetaFactory;

  private volatile Consumer<TranspSess> openHandler;
  private volatile Consumer<TranspSess> closeHandler;
  private volatile BiConsumer<TranspSess, String> messageHandler;
  private volatile TranspSess currentSession;

  STDIOInb(
      InputStream inputStream,
      ServerLogger logger,
      McpSessIdGen sessionIdGenerator,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      McpSessRTMetaFactory runtimeMetaFactory) {

    this.inputStream = Objects.requireNonNull(inputStream, "inputStream");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.sessionIdGenerator = Objects.requireNonNull(sessionIdGenerator, "sessionIdGenerator");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
    this.requestAuthBindingPolicy = Objects.requireNonNull(requestAuthBindingPolicy, "requestAuthBindingPolicy");
    this.runtimeMetaFactory = Objects.requireNonNull(runtimeMetaFactory, "runtimeMetaFactory");
  }

  void STDIOInbSetOpenHandler(Consumer<TranspSess> handler) {
    this.openHandler = Objects.requireNonNull(handler, "handler");
  }

  void STDIOInbSetCloseHandler(Consumer<TranspSess> handler) {
    this.closeHandler = Objects.requireNonNull(handler, "handler");
  }

  void STDIOInbSetMessageHandler(BiConsumer<TranspSess, String> handler) {
    this.messageHandler = Objects.requireNonNull(handler, "handler");
  }

  TranspSess STDIOInbGetCurrentSess() {
    return currentSession;
  }

  boolean STDIOInbCloseSess(String sessionId) {

    TranspSess session = currentSession;
    if (session == null || sessionId == null || sessionId.isBlank()) {
      return false;
    }

    if (!sessionId.equals(session.TranspSessGetMcpSessId())) {
      return false;
    }

    return STDIOInbFinalizeClose(session);
  }

  void STDIOInbRunLoop() {

    TranspSess session = new TranspSess(
        TRANSPORT_NAME,
        TRANSPORT_CONNECTION_ID,
        sessionIdGenerator.generate(),
        OperatingSurface.MCP_DIRECT,
        requestAuthBindingPolicy.ReqsAuthBindingPolicyResolveDirectMcpDefault("stdio_direct_mcp"),
        runtimeMetaFactory);

    currentSession = session;

    runtimeMetrics.RTMetricsIncrementCounter("stdio.connections.opened.total");
    runtimeMetrics.RTMetricsSetGauge("stdio.connections.active", 1L);

    ObservCtx openContext = obsCtxFactory.ObservCtxFactoryFromTranspSess(session);

    logger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        openContext,
        "OPEN",
        "STDIO_TRANSPORT_OPENED",
        "STDIO INBOUND: OPEN");

    Consumer<TranspSess> openConsumer = openHandler;
    if (openConsumer != null) {
      openConsumer.accept(session);
    }

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      String line;
      while ((line = reader.readLine()) != null) {
        if (!session.TranspSessIsActive()) {
          break;
        }

        if (line.isBlank()) {
          continue;
        }

        runtimeMetrics.RTMetricsIncrementCounter("stdio.messages.in.total");

        logger.ServerLogInfoObserved(
            ServerLogger.Component.RUNTIME,
            openContext,
            "RECEIVE",
            "STDIO_MESSAGE_RECEIVED",
            "STDIOInb: message received bytes=" + line.length());

        BiConsumer<TranspSess, String> handler = messageHandler;
        if (handler != null) {
          try {
            handler.accept(session, line);
          } catch (RuntimeException ex) {
            runtimeMetrics.RTMetricsIncrementCounter(
                TranspSignalModel.TransSigTranspErrorsMetricName(TRANSPORT_NAME));
            telemetryMetrics.McpTelemIncrementTranspError(openContext, "inbound", "TRANSPORT_ERROR");
            logger.ServerLogErrorStructured(
                ServerLogger.Component.RUNTIME,
                session.TranspSessGetMcpSessId(),
                session.TranspSessGetTranspConnId(),
                RPC_NOT_APPLICABLE,
                "STDIOInb: message handler failed: " + ex.getMessage(),
                ex);
          }
        }
      }

    } catch (IOException ex) {

      runtimeMetrics.RTMetricsIncrementCounter(
          TranspSignalModel.TransSigTranspErrorsMetricName(TRANSPORT_NAME));
      telemetryMetrics.McpTelemIncrementTranspError(
          obsCtxFactory.ObservCtxFactoryWithErrType(
              obsCtxFactory.ObservCtxFactoryFromTranspSess(session),
              "TRANSPORT_ERROR"),
          "inbound",
          "TRANSPORT_ERROR");

      logger.ServerLogErrorStructured(
          ServerLogger.Component.RUNTIME,
          session.TranspSessGetMcpSessId(),
          session.TranspSessGetTranspConnId(),
          RPC_NOT_APPLICABLE,
          "STDIOInb: read loop failed: " + ex.getMessage(),
          ex);
    } finally {
      STDIOInbFinalizeClose(session);
    }
  }

  private boolean STDIOInbFinalizeClose(TranspSess session) {

    if (session == null || !session.TranspSessClose()) {
      return false;
    }

    currentSession = null;
    runtimeMetrics.RTMetricsIncrementCounter(
        TranspSignalModel.TransSigSessClosedMetricName(TRANSPORT_NAME));
    runtimeMetrics.RTMetricsSetGauge("stdio.connections.active", 0L);

    logger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        obsCtxFactory.ObservCtxFactoryFromTranspSess(session),
        "CLOSE",
        "STDIO_TRANSPORT_CLOSED",
        "STDIO INBOUND: CLOSE");

    Consumer<TranspSess> closeConsumer = closeHandler;
    if (closeConsumer != null) {
      closeConsumer.accept(session);
    }

    return true;
  }
}
