package mcp.server.foundation.prompt_interface;

import java.util.Map;
import java.util.Objects;

final class PromptSupport {

  private PromptSupport() {
  }

  static String requireNonBlank(String value, String fieldName) {

    Objects.requireNonNull(value, fieldName);

    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return trimmed;
  }

  static Map<String, Object> safeArguments(Map<String, Object> arguments) {
    return arguments == null ? Map.of() : Map.copyOf(arguments);
  }

  static String normalizeResourcePath(String resourcePath) {

    String normalized = requireNonBlank(resourcePath, "resourcePath");

    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("resourcePath must not resolve to empty path");
    }

    return normalized;
  }
}
