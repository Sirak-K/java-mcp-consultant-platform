package mcp.server.foundation.security.request_binding;

import mcp.server.foundation.server_process.orchestration.OperatingSurface;

import java.util.Objects;

/**
 * Canonical policy helper for constructing allowed request bindings.
 */
public final class ReqsAuthBindingPolicy {

  public static final String AUTHORITY_NETWORK_AUTH = "network_auth";
  public static final String AUTHORITY_APP_AUTH = "app_auth";
  public static final String AUTHORITY_PLATFORM_OPS = "platform_ops";
  public static final String AUTHORITY_JWT_AUTH = "jwt_auth";

  private final ReqsLifecyReg requestLifecycleRegistry;

  public ReqsAuthBindingPolicy(ReqsLifecyReg requestLifecycleRegistry) {
    this.requestLifecycleRegistry = Objects.requireNonNull(requestLifecycleRegistry, "requestLifecycleRegistry");
  }

  public ReqsLifecyReg ReqsAuthBindingPolicyGetRegistry() {
    return requestLifecycleRegistry;
  }

  public ReqsAuthBinding ReqsAuthBindingPolicyResolveDirectMcpDefault(String principalId) {
    return new ReqsAuthBinding(
        new ReqsPrincipal(
            requireText(principalId, "principalId"),
            ReqsPrincipalType.SERVICE_PRINCIPAL,
            AUTHORITY_NETWORK_AUTH,
            OperatingSurface.MCP_DIRECT),
        ActiveTenantCtx.ActiveTenantCtxPlatformSystem(),
        TenantBindingSource.PLATFORM_SYSTEM,
        ReqsBindingStage.PLATFORM_BOUND);
  }

  public ReqsAuthBinding ReqsAuthBindingPolicyResolveJwtMcpDefault(String principalId) {
    return new ReqsAuthBinding(
        new ReqsPrincipal(
            requireText(principalId, "principalId"),
            ReqsPrincipalType.SERVICE_PRINCIPAL,
            AUTHORITY_JWT_AUTH,
            OperatingSurface.MCP_DIRECT),
        ActiveTenantCtx.ActiveTenantCtxPlatformSystem(),
        TenantBindingSource.PLATFORM_SYSTEM,
        ReqsBindingStage.PLATFORM_BOUND);
  }

  public ReqsAuthBinding ReqsAuthBindingPolicyResolveAppPreSession(String principalId) {
    return new ReqsAuthBinding(
        new ReqsPrincipal(
            requireText(principalId, "principalId"),
            ReqsPrincipalType.HUMAN_PRINCIPAL,
            AUTHORITY_APP_AUTH,
            OperatingSurface.APP_ADAPTER),
        null,
        TenantBindingSource.PRE_SESSION,
        ReqsBindingStage.PRE_SESSION);
  }

  public ReqsAuthBinding ReqsAuthBindingPolicyResolvePlatformOpsDefault(String principalId) {
    return new ReqsAuthBinding(
        new ReqsPrincipal(
            requireText(principalId, "principalId"),
            ReqsPrincipalType.PLATFORM_OPS_PRINCIPAL,
            AUTHORITY_PLATFORM_OPS,
            OperatingSurface.PLATFORM_OPS),
        ActiveTenantCtx.ActiveTenantCtxPlatformSystem(),
        TenantBindingSource.PLATFORM_SYSTEM,
        ReqsBindingStage.PLATFORM_BOUND);
  }

  public ReqsAuthBinding ReqsAuthBindingPolicyResolveOpsAssumeTenant(
      ReqsPrincipal platformOpsPrincipal,
      String tenantId) {

    Objects.requireNonNull(platformOpsPrincipal, "platformOpsPrincipal");
    if (!platformOpsPrincipal.ReqsPrincipalIsPlatformOps()) {
      throw new IllegalArgumentException("assume-tenant requires platform ops principal");
    }

    return new ReqsAuthBinding(
        platformOpsPrincipal,
        ActiveTenantCtx.ActiveTenantCtxForTenant(tenantId),
        TenantBindingSource.ASSUME_TENANT,
        ReqsBindingStage.TENANT_BOUND);
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
