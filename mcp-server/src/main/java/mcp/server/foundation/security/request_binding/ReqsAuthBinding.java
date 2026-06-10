package mcp.server.foundation.security.request_binding;

import java.util.Objects;

/**
 * Canonical request binding object: principal + binding source + active tenant phase.
 */
public record ReqsAuthBinding(
    ReqsPrincipal principal,
    ActiveTenantCtx activeTenant,
    TenantBindingSource bindingSource,
    ReqsBindingStage bindingStage) {

  public ReqsAuthBinding {
    principal = Objects.requireNonNull(principal, "principal");
    bindingSource = Objects.requireNonNull(bindingSource, "bindingSource");
    bindingStage = Objects.requireNonNull(bindingStage, "bindingStage");

    if (bindingStage == ReqsBindingStage.PRE_SESSION) {
      if (activeTenant != null) {
        throw new IllegalArgumentException("PRE_SESSION binding must not carry activeTenant");
      }
      if (bindingSource != TenantBindingSource.PRE_SESSION) {
        throw new IllegalArgumentException("PRE_SESSION binding must use PRE_SESSION source");
      }
    }

    if (bindingStage == ReqsBindingStage.TENANT_BOUND) {
      if (activeTenant == null || activeTenant.platformSystem()) {
        throw new IllegalArgumentException("TENANT_BOUND binding requires a non-platform tenant");
      }
    }

    if (bindingStage == ReqsBindingStage.PLATFORM_BOUND) {
      if (activeTenant == null || !activeTenant.platformSystem()) {
        throw new IllegalArgumentException("PLATFORM_BOUND binding requires platform_system tenant");
      }
      if (bindingSource != TenantBindingSource.PLATFORM_SYSTEM
          && bindingSource != TenantBindingSource.ASSUME_TENANT) {
        throw new IllegalArgumentException(
            "PLATFORM_BOUND binding must use PLATFORM_SYSTEM or ASSUME_TENANT source");
      }
    }
  }

  public boolean ReqsAuthBindingHasActiveTenant() {
    return activeTenant != null;
  }

  public boolean ReqsAuthBindingIsPreSession() {
    return bindingStage == ReqsBindingStage.PRE_SESSION;
  }

  public boolean ReqsAuthBindingIsPlatformBound() {
    return bindingStage == ReqsBindingStage.PLATFORM_BOUND;
  }

  public String principalId() {
    return principal.principalId();
  }

  public String tenantExternalId() {
    if (activeTenant == null || activeTenant.platformSystem()) {
      return null;
    }
    return activeTenant.tenantId();
  }

  public String tenantType() {
    if (platformSystem()) {
      return "PLATFORM_SYSTEM";
    }
    return "ASSUMED_CONTEXT";
  }

  public boolean platformSystem() {
    return activeTenant != null && activeTenant.platformSystem();
  }

  public String roleName() {
    if (platformSystem()) {
      return "OPS";
    }
    return "CONTEXT";
  }

  public String authority() {
    return "ROLE_" + roleName();
  }
}
