package mcp.server.foundation.transport.websocket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * WsConnId
 *
 * Representerar den fysiska WebSocket-anslutningens identitet.
 *
 * KONTRAKT:
 * - Är strikt transport-scope (physical connection identity).
 * - Har ingen koppling till MCP McpSessId.
 * - Är immutable value object.
 */
public final class WsConnId implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final UUID value;

  private WsConnId(UUID value) {
    this.value = Objects.requireNonNull(value, "value");
  }

  public static WsConnId random() {
    return new WsConnId(UUID.randomUUID());
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static WsConnId fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("WsConnId cannot be null or blank");
    }
    try {
      return new WsConnId(UUID.fromString(raw));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid WsConnId format");
    }
  }

  @JsonValue
  public String asString() {
    return value.toString();
  }

  public UUID asUuid() {
    return value;
  }

  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WsConnId that)) {
      return false;
    }
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}