package mcp.server.foundation.tool_interface;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;

import java.util.Objects;

final class ToolInvocLogger {

  private final ServerLogger logger;

  ToolInvocLogger(ServerLogger logger) {
    this.logger = logger;
  }

  void info(
      ObservCtx context,
      String action,
      String eventName,
      String message) {

    info(context, action, eventName, message, null);
  }

  void info(
      ObservCtx context,
      String action,
      String eventName,
      String message,
      Long durationMs) {

    if (logger == null) {
      return;
    }

    if (durationMs == null) {
      logger.ServerLogInfoObserved(ServerLogger.Component.RPC, context, action, eventName, message);
      return;
    }

    logger.ServerLogInfoObserved(ServerLogger.Component.RPC, context, action, eventName, message, durationMs);
  }

  void warn(
      ObservCtx context,
      String action,
      String eventName,
      String message,
      String errorType) {

    if (logger == null) {
      return;
    }

    logger.ServerLogWarnObserved(ServerLogger.Component.RPC, context, action, eventName, message, errorType);
  }

  void error(
      ObservCtx context,
      String action,
      String eventName,
      String message,
      Long durationMs,
      String errorType,
      Throwable throwable) {

    if (logger == null) {
      return;
    }

    logger.ServerLogErrorObserved(
        ServerLogger.Component.RPC,
        context,
        action,
        eventName,
        message,
        durationMs,
        errorType,
        Objects.requireNonNull(throwable, "throwable"));
  }
}
