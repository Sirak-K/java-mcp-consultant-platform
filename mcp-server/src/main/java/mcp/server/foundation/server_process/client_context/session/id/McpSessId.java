package mcp.server.foundation.server_process.client_context.session.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class McpSessId implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final UUID value;

  public McpSessId(UUID value) {
    this.value = Objects.requireNonNull(value, "value");
  }

  public static McpSessId of(UUID value) {
    return new McpSessId(value);
  }

  public static McpSessId random() {
    return new McpSessId(UUID.randomUUID());
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static McpSessId fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("McpSessId cannot be null or blank");
    }
    try {
      return new McpSessId(UUID.fromString(raw));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid McpSessId format");
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
    if (this == o)
      return true;
    if (!(o instanceof McpSessId that))
      return false;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}