package mcp.server.foundation.transport.stdio;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.server_process.client_context.session.id.McpSessIdGen;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import mcp.server.foundation.transport.TranspSess;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class STDIORTOrch {

  private final STDIOInb inbound;
  private final STDIOOutb outbound;
  private final AtomicBoolean started = new AtomicBoolean(false);

  private volatile Thread readLoopThread;

  public STDIORTOrch(
      InputStream inputStream,
      PrintStream output,
      McpSessIdGen sessionIdGenerator,
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      McpSessRTMetaFactory runtimeMetaFactory) {

    Objects.requireNonNull(inputStream, "inputStream");
    Objects.requireNonNull(output, "output");
    Objects.requireNonNull(sessionIdGenerator, "sessionIdGenerator");
    Objects.requireNonNull(serverLogger, "serverLogger");
    Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");

    this.inbound = new STDIOInb(
        inputStream,
        serverLogger,
        sessionIdGenerator,
        obsCtxFactory,
        runtimeMetrics,
        telemetryMetrics,
        requestAuthBindingPolicy,
        runtimeMetaFactory);

    this.outbound = new STDIOOutb(
        output,
        serverLogger,
        obsCtxFactory,
        runtimeMetrics,
        telemetryMetrics,
        inbound::STDIOInbCloseSess);
  }

  public void STDIORTOrchSetMessageHandler(BiConsumer<TranspSess, String> handler) {
    inbound.STDIOInbSetMessageHandler(handler);
  }

  public void STDIORTOrchSetOpenHandler(Consumer<TranspSess> handler) {
    inbound.STDIOInbSetOpenHandler(handler);
  }

  public void STDIORTOrchSetCloseHandler(Consumer<TranspSess> handler) {
    inbound.STDIOInbSetCloseHandler(handler);
  }

  public void STDIORTOrchSendTo(TranspSess session, String message) {
    outbound.STDIOOutbSendTo(session, message);
  }

  public TranspSess STDIORTOrchGetSessById(String sessionId) {

    TranspSess current = inbound.STDIOInbGetCurrentSess();

    if (current == null || sessionId == null || sessionId.isBlank()) {
      return null;
    }

    return sessionId.equals(current.TranspSessGetMcpSessId()) ? current : null;
  }

  public boolean STDIORTOrchCloseSess(String sessionId) {
    return inbound.STDIOInbCloseSess(sessionId);
  }

  public void STDIORTOrchStart() {

    if (!started.compareAndSet(false, true)) {
      return;
    }

    Thread thread = new Thread(inbound::STDIOInbRunLoop, "mcp-stdio-read-loop");
    thread.setDaemon(true);
    readLoopThread = thread;
    thread.start();
  }

  public void STDIORTOrchStop() {

    started.set(false);

    Thread thread = readLoopThread;
    if (thread != null) {
      thread.interrupt();
    }

    TranspSess session = inbound.STDIOInbGetCurrentSess();
    if (session != null) {
      session.TranspSessClose();
    }
  }
}
