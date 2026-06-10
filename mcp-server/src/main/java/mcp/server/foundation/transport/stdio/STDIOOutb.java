package mcp.server.foundation.transport.stdio;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.transport.TranspOutbTelemSupport;
import mcp.server.foundation.transport.TranspSess;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

final class STDIOOutb {

  private static final String RPC_NOT_APPLICABLE = "N/A";

  private final PrintStream output;
  private final ServerLogger logger;
  private final ObservCtxFactory obsCtxFactory;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;
  private final Function<String, Boolean> closeSessionById;

  STDIOOutb(
      PrintStream output,
      ServerLogger logger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      Function<String, Boolean> closeSessionById) {

    this.output = Objects.requireNonNull(output, "output");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
    this.closeSessionById = Objects.requireNonNull(closeSessionById, "closeSessionById");
  }

  void STDIOOutbSendTo(
      TranspSess session,
      String message) {

    if (session == null || message == null) {
      return;
    }

    try {
      long sendStartedAt = System.nanoTime();
      ObservCtx context = obsCtxFactory.ObservCtxFactoryFromTranspSess(session);
      output.print(message);
      output.print('\n');
      output.flush();

      if (output.checkError()) {
        throw new IllegalStateException("STDIO output stream reported a write failure");
      }

      TranspOutbTelemSupport.TranspOutbTelemRecordSent(
          runtimeMetrics,
          "stdio",
          "stdio.transport.outbound.duration",
          sendStartedAt,
          session,
          message);

      logger.ServerLogInfoObserved(
          ServerLogger.Component.RUNTIME,
          context,
          "SEND",
          "STDIO_MESSAGE_SENT",
          "STDIOOutb: message sent bytes=" + message.getBytes(StandardCharsets.UTF_8).length);
    } catch (RuntimeException ex) {
      TranspOutbTelemSupport.TranspOutbTelemRecordErr(
          runtimeMetrics,
          telemetryMetrics,
          obsCtxFactory.ObservCtxFactoryWithErrType(
              obsCtxFactory.ObservCtxFactoryFromTranspSess(session),
              "TRANSPORT_ERROR"),
          "stdio");
      closeSessionById.apply(session.TranspSessGetMcpSessId());
      logger.ServerLogErrorStructured(
          ServerLogger.Component.RUNTIME,
          session.TranspSessGetMcpSessId(),
          session.TranspSessGetTranspConnId(),
          RPC_NOT_APPLICABLE,
          "STDIOOutb: send failed: " + ex.getMessage(),
          ex);
    }
  }
}
