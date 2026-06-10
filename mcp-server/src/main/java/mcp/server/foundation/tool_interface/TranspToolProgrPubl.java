package mcp.server.foundation.tool_interface;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.rpc.RPCJsonSeria;
import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.TranspSess;

import java.util.Map;
import java.util.Objects;

public final class TranspToolProgrPubl implements ToolProgrPubl {

  private static final String METHOD = "tools/progress";

  private final TranspAdap transportAdapter;
  private final RPCJsonSeria serializer;
  private final ServerLogger logger;

  public TranspToolProgrPubl(
      TranspAdap transportAdapter,
      RPCJsonSeria serializer,
      ServerLogger logger) {

    this.transportAdapter = Objects.requireNonNull(transportAdapter, "transportAdapter");
    this.serializer = Objects.requireNonNull(serializer, "serializer");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @Override
  public void ToolProgrPublPublish(
      ToolProgrUpd update,
      ObservCtx context) {

    Objects.requireNonNull(update, "update");
    Objects.requireNonNull(context, "context");

    String sessionId = context.ObservCtxGetMcpSessId();
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }

    TranspSess session = transportAdapter.TranspAdapGetSessionById(sessionId);
    if (session == null || !session.TranspSessIsActive()) {
      return;
    }

    String payload = serializer.JsonRPCSerSerialize(Map.of(
        "jsonrpc", "2.0",
        "method", METHOD,
        "params", update.ToolProgrUpdToNotificationParams()));

    transportAdapter.TranspAdapSendTo(session, payload);

    logger.ServerLogInfoObserved(
        ServerLogger.Component.RPC,
        context,
        "PROGRESS",
        "TOOL_PROGRESS_PUBLISHED",
        "Tool progress published for " + update.toolName() + " state=" + update.state().name());
  }
}
