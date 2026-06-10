package mcp.server.foundation.security.request_binding;

import java.util.Objects;

/**
 * Canonical active tenant context for one request or bound business session.
 */
public record ActiveTenantCtx(
    String tenantId,
    boolean platformSystem) {

  public static final String PLATFORM_SYSTEM_TENANT_ID = "platform_system";

  public ActiveTenantCtx {
    tenantId = requireText(tenantId, "tenantId");
    if (platformSystem && !PLATFORM_SYSTEM_TENANT_ID.equals(tenantId)) {
      throw new IllegalArgumentException(
          "platformSystem tenant must use tenantId=" + PLATFORM_SYSTEM_TENANT_ID);
    }
    if (!platformSystem && PLATFORM_SYSTEM_TENANT_ID.equals(tenantId)) {
      throw new IllegalArgumentException("platform_system tenantId requires platformSystem=true");
    }
  }

  public static ActiveTenantCtx ActiveTenantCtxForTenant(String tenantId) {
    return new ActiveTenantCtx(tenantId, false);
  }

  public static ActiveTenantCtx ActiveTenantCtxPlatformSystem() {
    return new ActiveTenantCtx(PLATFORM_SYSTEM_TENANT_ID, true);
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
