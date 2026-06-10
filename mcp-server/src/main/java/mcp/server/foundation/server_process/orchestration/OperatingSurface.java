package mcp.server.foundation.server_process.orchestration;

import java.util.Objects;

/**
 * Canonical operating surfaces for the current operating model.
 *
 * <p>These values represent first-class product/server entry surfaces, not
 * transport names and not tenant identities.
 */
public enum OperatingSurface {

  MCP_DIRECT("mcp_direct"),
  APP_ADAPTER("app_adapter"),
  PLATFORM_OPS("platform_ops");

  private final String id;

  OperatingSurface(String id) {

    this.id = requireText(id, "id");
  }

  public String OperatingSurfaceGetId() {
    return id;
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
