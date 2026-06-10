package mcp.server.foundation.security.request_binding;

import mcp.server.foundation.server_process.orchestration.OperatingSurface;

import java.util.Objects;

/**
 * Fail-closed guard for operating-surface binding compliance.
 */
public final class ReqsBindingComplianceGuard {

  private final ReqsLifecyReg requestLifecycleRegistry;

  public ReqsBindingComplianceGuard(ReqsLifecyReg requestLifecycleRegistry) {
    this.requestLifecycleRegistry = Objects.requireNonNull(requestLifecycleRegistry, "requestLifecycleRegistry");
  }

  public ReqsLifecyReg ReqsBindingComplianceGuardGetRegistry() {
    return requestLifecycleRegistry;
  }

  public ReqsBindingComplianceDecision ReqsBindingComplianceGuardEvaluate(
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding) {

    Objects.requireNonNull(operatingSurface, "operatingSurface");

    ReqsLifecyContract contract = requestLifecycleRegistry.ReqsLifecyRegGet(operatingSurface);
    String expectedRule = ReqsBindingComplianceGuardExpectedRule(operatingSurface, contract);

    if (requestAuthBinding == null) {
      return ReqsBindingComplianceGuardDeny(
          operatingSurface,
          contract,
          null,
          expectedRule,
          "MISSING_REQUEST_AUTH_BINDING",
          "ReqsAuthBinding is required for " + operatingSurface.OperatingSurfaceGetId());
    }

    ReqsPrincipal principal = requestAuthBinding.principal();
    ReqsBindingStage actualStage = requestAuthBinding.bindingStage();
    TenantBindingSource actualSource = requestAuthBinding.bindingSource();
    ActiveTenantCtx activeTenant = requestAuthBinding.activeTenant();
    String principalTypeId = principal == null ? null : principal.principalType().ReqsPrincipalTypeGetId();
    String principalSurfaceId = principal == null ? null : principal.operatingSurface().OperatingSurfaceGetId();
    String tenantId = activeTenant == null ? null : activeTenant.tenantId();

    if (principal == null) {
      return ReqsBindingComplianceGuardDeny(
          operatingSurface,
          contract,
          requestAuthBinding,
          expectedRule,
          "MISSING_PRINCIPAL",
          "ReqsAuthBinding principal is required");
    }

    if (principal.operatingSurface() != operatingSurface) {
      return ReqsBindingComplianceGuardDeny(
          operatingSurface,
          contract,
          requestAuthBinding,
          expectedRule,
          "PRINCIPAL_SURFACE_MISMATCH",
          "Principal surface "
              + principal.operatingSurface().OperatingSurfaceGetId()
              + " does not match request surface "
              + operatingSurface.OperatingSurfaceGetId());
    }

    if (actualStage == ReqsBindingStage.PRE_SESSION) {
      if (!contract.ReqsLifecyContractIsPreSessionAllowed()) {
        return ReqsBindingComplianceGuardDeny(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            "PRE_SESSION_NOT_ALLOWED",
            "Pre-session bindings are not allowed on " + operatingSurface.OperatingSurfaceGetId());
      }

      if (actualSource != TenantBindingSource.PRE_SESSION) {
        return ReqsBindingComplianceGuardDeny(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            "PRE_SESSION_SOURCE_MISMATCH",
            "Pre-session bindings must use PRE_SESSION source");
      }

      if (operatingSurface != OperatingSurface.APP_ADAPTER) {
        return ReqsBindingComplianceGuardDeny(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            "PRE_SESSION_SURFACE_MISMATCH",
            "Pre-session bindings are only allowed on app adapter surface");
      }

      return ReqsBindingComplianceGuardAllow(
          operatingSurface,
          contract,
          requestAuthBinding,
          expectedRule,
          actualStage,
          actualSource,
          principalTypeId,
          principalSurfaceId,
          tenantId,
          false,
          false,
          true);
    }

    if (actualStage == ReqsBindingStage.PLATFORM_BOUND) {
      if (actualSource != TenantBindingSource.PLATFORM_SYSTEM) {
        return ReqsBindingComplianceGuardDeny(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            "PLATFORM_BOUND_SOURCE_MISMATCH",
            "Platform-bound bindings must use PLATFORM_SYSTEM source");
      }

      if (activeTenant == null || !activeTenant.platformSystem()) {
        return ReqsBindingComplianceGuardDeny(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            "PLATFORM_BOUND_TENANT_MISMATCH",
            "Platform-bound bindings require platform_system active tenant");
      }

      if (operatingSurface == OperatingSurface.APP_ADAPTER) {
        return ReqsBindingComplianceGuardDeny(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            "PLATFORM_BOUND_SURFACE_MISMATCH",
            "App adapter surface must not use platform-bound request auth");
      }

      if (operatingSurface == OperatingSurface.PLATFORM_OPS
          && !principal.ReqsPrincipalIsPlatformOps()) {
        return ReqsBindingComplianceGuardDeny(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            "PLATFORM_OPS_PRINCIPAL_REQUIRED",
            "Platform ops surface requires a platform ops principal");
      }

      return ReqsBindingComplianceGuardAllow(
          operatingSurface,
          contract,
          requestAuthBinding,
          expectedRule,
          actualStage,
          actualSource,
          principalTypeId,
          principalSurfaceId,
          tenantId,
          operatingSurface == OperatingSurface.PLATFORM_OPS,
          false,
          false);
    }

    if (actualStage == ReqsBindingStage.TENANT_BOUND) {
      if (activeTenant == null || activeTenant.platformSystem()) {
        return ReqsBindingComplianceGuardDeny(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            "TENANT_BOUND_TENANT_MISMATCH",
            "Tenant-bound bindings require a non-platform tenant");
      }

      if (actualSource == TenantBindingSource.AUTH_IDENTITY) {
        if (operatingSurface == OperatingSurface.PLATFORM_OPS) {
          return ReqsBindingComplianceGuardDeny(
              operatingSurface,
              contract,
              requestAuthBinding,
              expectedRule,
              "OPS_ASSUME_TENANT_REQUIRED",
              "Platform ops tenant binding must use ASSUME_TENANT source");
        }

        return ReqsBindingComplianceGuardAllow(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            actualStage,
            actualSource,
            principalTypeId,
            principalSurfaceId,
            tenantId,
            false,
            operatingSurface == OperatingSurface.PLATFORM_OPS,
            false);
      }

      if (actualSource == TenantBindingSource.ASSUME_TENANT) {
        if (!contract.ReqsLifecyContractIsAssumeTenantAllowed()) {
          return ReqsBindingComplianceGuardDeny(
              operatingSurface,
              contract,
              requestAuthBinding,
              expectedRule,
              "ASSUME_TENANT_NOT_ALLOWED",
              "Assume-tenant bindings are not allowed on " + operatingSurface.OperatingSurfaceGetId());
        }

        if (operatingSurface != OperatingSurface.PLATFORM_OPS) {
          return ReqsBindingComplianceGuardDeny(
              operatingSurface,
              contract,
              requestAuthBinding,
              expectedRule,
              "ASSUME_TENANT_SURFACE_MISMATCH",
              "Assume-tenant bindings are reserved for platform ops surface");
        }

        if (!principal.ReqsPrincipalIsPlatformOps()) {
          return ReqsBindingComplianceGuardDeny(
              operatingSurface,
              contract,
              requestAuthBinding,
              expectedRule,
              "PLATFORM_OPS_PRINCIPAL_REQUIRED",
              "Assume-tenant requires a platform ops principal");
        }

        return ReqsBindingComplianceGuardAllow(
            operatingSurface,
            contract,
            requestAuthBinding,
            expectedRule,
            actualStage,
            actualSource,
            principalTypeId,
            principalSurfaceId,
            tenantId,
            true,
            true,
            false);
      }

      return ReqsBindingComplianceGuardDeny(
          operatingSurface,
          contract,
          requestAuthBinding,
          expectedRule,
          "TENANT_BOUND_SOURCE_MISMATCH",
          "Tenant-bound bindings must use AUTH_IDENTITY or ASSUME_TENANT source");
    }

    return ReqsBindingComplianceGuardDeny(
        operatingSurface,
        contract,
        requestAuthBinding,
        expectedRule,
        "UNKNOWN_BINDING_STAGE",
        "Unknown request binding stage: " + actualStage);
  }

  public ReqsAuthBinding ReqsBindingComplianceGuardRequireCompliant(
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding) {

    ReqsBindingComplianceDecision decision = ReqsBindingComplianceGuardEvaluate(
        operatingSurface,
        requestAuthBinding);

    if (decision.ReqsBindingComplianceDecisionIsDenied()) {
      throw new IllegalArgumentException("Request binding not compliant: " + decision.ReqsBindingComplianceDecisionDescribe());
    }

    return requestAuthBinding;
  }

  private static ReqsBindingComplianceDecision ReqsBindingComplianceGuardAllow(
      OperatingSurface operatingSurface,
      ReqsLifecyContract contract,
      ReqsAuthBinding requestAuthBinding,
      String expectedRule,
      ReqsBindingStage actualStage,
      TenantBindingSource actualSource,
      String principalTypeId,
      String principalSurfaceId,
      String tenantId,
      boolean platformOpsSurface,
      boolean assumeTenantPath,
      boolean preSessionPath) {

    return new ReqsBindingComplianceDecision(
        operatingSurface,
        contract,
        requestAuthBinding,
        true,
        "ALLOW",
        "Request binding is compliant",
        expectedRule,
        actualStage,
        actualSource,
        principalTypeId,
        principalSurfaceId,
        tenantId,
        platformOpsSurface,
        assumeTenantPath,
        preSessionPath);
  }

  private static ReqsBindingComplianceDecision ReqsBindingComplianceGuardDeny(
      OperatingSurface operatingSurface,
      ReqsLifecyContract contract,
      ReqsAuthBinding requestAuthBinding,
      String expectedRule,
      String decisionCode,
      String decisionReason) {

    String principalTypeId = null;
    String principalSurfaceId = null;
    String tenantId = null;
    ReqsBindingStage actualStage = null;
    TenantBindingSource actualSource = null;

    if (requestAuthBinding != null) {
      actualStage = requestAuthBinding.bindingStage();
      actualSource = requestAuthBinding.bindingSource();
      if (requestAuthBinding.principal() != null) {
        principalTypeId = requestAuthBinding.principal().principalType().ReqsPrincipalTypeGetId();
        principalSurfaceId = requestAuthBinding.principal().operatingSurface().OperatingSurfaceGetId();
      }
      if (requestAuthBinding.activeTenant() != null) {
        tenantId = requestAuthBinding.activeTenant().tenantId();
      }
    }

    return new ReqsBindingComplianceDecision(
        operatingSurface,
        contract,
        requestAuthBinding,
        false,
        decisionCode,
        decisionReason,
        expectedRule,
        actualStage,
        actualSource,
        principalTypeId,
        principalSurfaceId,
        tenantId,
        operatingSurface == OperatingSurface.PLATFORM_OPS,
        actualSource == TenantBindingSource.ASSUME_TENANT,
        actualStage == ReqsBindingStage.PRE_SESSION);
  }

  private static String ReqsBindingComplianceGuardExpectedRule(
      OperatingSurface operatingSurface,
      ReqsLifecyContract contract) {

    return operatingSurface.OperatingSurfaceGetId()
        + "[defaultStage=" + contract.ReqsLifecyContractGetDefaultStage().ReqsBindingStageGetId()
        + ", authRequired=" + contract.ReqsLifecyContractIsAuthRequired()
        + ", preSessionAllowed=" + contract.ReqsLifecyContractIsPreSessionAllowed()
        + ", assumeTenantAllowed=" + contract.ReqsLifecyContractIsAssumeTenantAllowed()
        + "]";
  }
}
