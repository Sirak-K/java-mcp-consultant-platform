package mcp.server.foundation.transport.websocket;

import java.util.Arrays;
import java.util.Objects;

public record WsTranspSettings(
    String path,
    String[] allowedOrigins,
    int maxActiveConnections) {

  public WsTranspSettings {
    path = normalizePath(path);
    Objects.requireNonNull(allowedOrigins, "allowedOrigins");
    allowedOrigins = Arrays.stream(allowedOrigins)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toArray(String[]::new);

    if (allowedOrigins.length == 0) {
      throw new IllegalArgumentException("allowedOrigins must contain at least one value");
    }

    if (Arrays.stream(allowedOrigins).anyMatch("*"::equals)) {
      throw new IllegalArgumentException("allowedOrigins must not contain wildcard '*'");
    }

    if (maxActiveConnections <= 0) {
      throw new IllegalArgumentException("maxActiveConnections must be > 0");
    }
  }

  private static String normalizePath(String rawPath) {
    Objects.requireNonNull(rawPath, "rawPath");

    String normalized = rawPath.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("WebSocket path must not be blank");
    }

    if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }

    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }

    return normalized;
  }
}
