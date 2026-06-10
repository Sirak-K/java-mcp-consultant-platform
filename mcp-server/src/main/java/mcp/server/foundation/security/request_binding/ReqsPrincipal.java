package mcp.server.foundation.security.request_binding;

import mcp.server.foundation.server_process.orchestration.OperatingSurface;

import java.util.Objects;

/**
 * Canonical authenticated identity seen by the server for one request flow.
 */
public record ReqsPrincipal(
    String principalId,
    ReqsPrincipalType principalType,
    String authorityId,
    OperatingSurface operatingSurface) {

  public ReqsPrincipal {
    principalId = requireText(principalId, "principalId");
    principalType = Objects.requireNonNull(principalType, "principalType");
    authorityId = requireText(authorityId, "authorityId");
    operatingSurface = Objects.requireNonNull(operatingSurface, "operatingSurface");
  }

  public boolean ReqsPrincipalIsPlatformOps() {
    return principalType == ReqsPrincipalType.PLATFORM_OPS_PRINCIPAL;
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
