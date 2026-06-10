package mcp.server.foundation.server_process.client_context.session.event;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.rpc.RPCMetName;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;

import java.util.Objects;

/**
 * McpSessEventPubl
 *
 * Foundation-level session notification publisher.
 *
 * Runtime contract:
 * - CorrelationId comes from RPCRouter as the single source of truth.
 * - Notifications have no root-level "id".
 *
 * Responsibility:
 * - Publish session lifecycle notifications to sentinel sessions.
 * - Only transport-active sentinel sessions receive notifications.
 */
public final class McpSessEventPubl {

  private static final String RPC_METHOD = RPCMetName.SESSION_EVENT;
  private final SentinelNotificationSupport notificationSupport;

  public McpSessEventPubl(
      SentinelNotificationSupport notificationSupport) {

    this.notificationSupport = Objects.requireNonNull(notificationSupport, "notificationSupport");
  }

  public void SessEventPublPublish(McpSessEventPayl payload) {

    Objects.requireNonNull(payload, "payload");

    notificationSupport.SentinelNotificationSupportPublishToAll(
        RPC_METHOD,
        payload.toParams(),
        ServerLogger.Component.MCP,
        "MCP_SESSION_NOTIFICATION_PUBLISHED",
        "RPC OUT(NOTIFICATION) method=" + RPC_METHOD,
        "Failed to send session/event notification: ");
  }

  /**
   * Sends a session/event notification directly to one specific sentinel.
   *
   * Used for subscribe + initial state replay: when a new sentinel subscribes,
   * push the current snapshot of active sessions only to that sentinel, without
   * re-broadcasting to other already-registered sentinels.
   */
  public void SessEventPublPublishTo(McpSessId targetSentinelId, McpSessEventPayl payload) {

    Objects.requireNonNull(targetSentinelId, "targetSentinelId");
    Objects.requireNonNull(payload, "payload");

    notificationSupport.SentinelNotificationSupportPublishTo(
        targetSentinelId,
        RPC_METHOD,
        payload.toParams(),
        ServerLogger.Component.MCP,
        "MCP_SESSION_NOTIFICATION_PUBLISHED",
        "RPC OUT(NOTIFICATION/SNAPSHOT) method=" + RPC_METHOD
            + " eventType=" + payload.getType(),
        "Failed to send snapshot session/event notification: ");
  }
}
