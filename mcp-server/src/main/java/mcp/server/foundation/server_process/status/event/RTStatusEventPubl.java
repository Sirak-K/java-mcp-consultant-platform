package mcp.server.foundation.server_process.status.event;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.server_process.client_context.session.event.SentinelNotificationSupport;

import java.util.Objects;

/**
 * RTStatusEventPubl
 *
 * Runtime status publication:
 * - correlationId MUST come from RPCRouter.
 */
public final class RTStatusEventPubl {

  private static final String RPC_METHOD = "runtime/status";

  private final SentinelNotificationSupport notificationSupport;

  public RTStatusEventPubl(
      SentinelNotificationSupport notificationSupport) {

    this.notificationSupport = Objects.requireNonNull(notificationSupport, "notificationSupport");
  }

  public void publish(RTStatusEventPayl payload) {

    Objects.requireNonNull(payload, "payload");

    notificationSupport.SentinelNotificationSupportPublishToAll(
        RPC_METHOD,
        payload.toParams(),
        ServerLogger.Component.RUNTIME,
        "RUNTIME_STATUS_NOTIFICATION_PUBLISHED",
        "RPC OUT(NOTIFICATION) method=" + RPC_METHOD,
        "Failed to send runtime/status notification: ");
  }
}
