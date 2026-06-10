package mcp.server.foundation.server_process.orchestration;

import mcp.server.foundation.security.request_binding.ReqsAuthBinding;

import java.util.Objects;

/**
 * Minimal restore core contract for durable runtime-session state.
 */
public record RTMcpSessRestoreCore(
    String sessionId,
    ReqsAuthBinding requestAuthBinding,
    RTMcpSessType sessionType,
    RTMcpSessPhase sessionPhase,
    long inactivityTtlSeconds,
    long sessionVersion) {

  public RTMcpSessRestoreCore {
    sessionId = requireText(sessionId, "sessionId");
    requestAuthBinding = Objects.requireNonNull(requestAuthBinding, "requestAuthBinding");
    sessionType = Objects.requireNonNull(sessionType, "sessionType");
    sessionPhase = Objects.requireNonNull(sessionPhase, "sessionPhase");
    if (inactivityTtlSeconds <= 0L) {
      throw new IllegalArgumentException("inactivityTtlSeconds must be > 0");
    }
    if (sessionVersion <= 0L) {
      throw new IllegalArgumentException("sessionVersion must be > 0");
    }
  }

  public RTMcpSessRestoreCore RTMcpSessRestoreCoreWithPhase(
      RTMcpSessPhase nextSessionPhase) {

    return new RTMcpSessRestoreCore(
        sessionId,
        requestAuthBinding,
        sessionType,
        Objects.requireNonNull(nextSessionPhase, "nextSessionPhase"),
        inactivityTtlSeconds,
        sessionVersion);
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
