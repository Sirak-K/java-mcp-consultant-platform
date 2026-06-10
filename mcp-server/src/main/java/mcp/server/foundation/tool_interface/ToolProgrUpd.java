package mcp.server.foundation.tool_interface;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ToolProgrUpd(
    String requestId,
    String toolName,
    State state,
    Integer progressPercent,
    String message,
    boolean cancellable,
    Instant timestamp) {

  public enum State {
    STARTED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    TIMED_OUT
  }

  public ToolProgrUpd {
    requestId = requireText(requestId, "requestId");
    toolName = requireText(toolName, "toolName");
    state = Objects.requireNonNull(state, "state");
    message = message == null ? "" : message.trim();
    timestamp = Objects.requireNonNull(timestamp, "timestamp");

    if (progressPercent != null && (progressPercent < 0 || progressPercent > 100)) {
      throw new IllegalArgumentException("progressPercent must be between 0 and 100");
    }
  }

  public Map<String, Object> ToolProgrUpdToNotificationParams() {

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("requestId", requestId);
    params.put("toolName", toolName);
    params.put("state", state.name());
    params.put("cancellable", cancellable);
    params.put("timestamp", timestamp.toString());

    if (progressPercent != null) {
      params.put("progressPercent", progressPercent);
    }

    if (!message.isBlank()) {
      params.put("message", message);
    }

    return Map.copyOf(params);
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName);
    String normalized = value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
