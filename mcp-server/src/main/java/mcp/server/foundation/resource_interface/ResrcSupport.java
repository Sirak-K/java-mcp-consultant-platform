package mcp.server.foundation.resource_interface;

import java.util.Objects;

final class ResrcSupport {

  static final String RESOURCE_URI_PREFIX = "resource://";

  private ResrcSupport() {
  }

  static String requireNonBlank(String value, String fieldName) {

    Objects.requireNonNull(value, fieldName);

    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return trimmed;
  }

  static String requireResourceUri(String uri) {

    String normalizedUri = requireNonBlank(uri, "uri");
    if (!normalizedUri.startsWith(RESOURCE_URI_PREFIX)) {
      throw new IllegalArgumentException(
          "Resource URI must start with '" + RESOURCE_URI_PREFIX + "' but was: " + normalizedUri);
    }

    return normalizedUri;
  }
}
