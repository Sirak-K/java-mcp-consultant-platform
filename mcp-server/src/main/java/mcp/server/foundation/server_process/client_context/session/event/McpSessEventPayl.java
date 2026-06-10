package mcp.server.foundation.server_process.client_context.session.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class McpSessEventPayl {

  public enum EventType {
    CONNECTED,
    INITIALIZED,
    CLOSED
  }

  private final EventType type;
  private final String sessionId;
  private final Instant timestamp;

  /**
   * rpcFallbackId
   *
   * Purpose:
   * - Correlation for server-emitted JSON-RPC notifications without root "id".
   *
   * Contract:
   * - Always UUID format.
   * - Exists only in params; notifications never have root-level "id".
   */
  private final String rpcFallbackId;

  public McpSessEventPayl(EventType type, String sessionId) {
    this.type = Objects.requireNonNull(type, "type");
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    this.timestamp = Instant.now();
    this.rpcFallbackId = UUID.randomUUID().toString();
  }

  public EventType getType() {
    return type;
  }

  public String getMcpSessId() {
    return sessionId;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Backward compatible accessor.
   */
  public String getRPCFallbackId() {
    return rpcFallbackId;
  }

  /**
   * Canonical correlation accessor for notification payloads.
   */
  public String getRPCCorrelaId() {
    return rpcFallbackId;
  }

  /**
   * XGUIDE compatibility:
   * - eventName gives stable domain-like names without breaking eventType.
   * - phase is emitted alongside sessionId for session-state consumers.
   */
  private String McpSessEventPlEventName() {
    return switch (type) {
      case CONNECTED -> "session_created";
      case CLOSED -> "session_removed";
      case INITIALIZED -> "session_initialized";
    };
  }

  private String McpSessEventPlPhase() {
    return type.name();
  }

  public Map<String, Object> toParams() {
    return Map.of(
        "eventType", type.name(),
        "phase", McpSessEventPlPhase(),
        "eventName", McpSessEventPlEventName(),
        "sessionId", sessionId,
        "timestamp", timestamp.toString(),
        "rpcFallbackId", rpcFallbackId,
        "rpcCorrelaId", rpcFallbackId);
  }
}
