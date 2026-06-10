package mcp.server.foundation.security.request_binding;

import mcp.server.foundation.server_process.orchestration.OperatingSurface;

import java.util.Objects;

/**
 * Compact verdict for request/auth/tenant compliance on one operating surface.
 */
public record ReqsBindingComplianceDecision(
    OperatingSurface operatingSurface,
    ReqsLifecyContract requestLifecycleContract,
    ReqsAuthBinding requestAuthBinding,
    boolean allowed,
    String decisionCode,
    String decisionReason,
    String expectedBindingRule,
    ReqsBindingStage actualBindingStage,
    TenantBindingSource actualBindingSource,
    String actualPrincipalTypeId,
    String actualPrincipalSurfaceId,
    String actualTenantId,
    boolean platformOpsSurface,
    boolean assumeTenantPath,
    boolean preSessionPath) {

  public ReqsBindingComplianceDecision {
    operatingSurface = Objects.requireNonNull(operatingSurface, "operatingSurface");
    requestLifecycleContract = Objects.requireNonNull(requestLifecycleContract, "requestLifecycleContract");
    decisionCode = requireText(decisionCode, "decisionCode");
    decisionReason = requireText(decisionReason, "decisionReason");
    expectedBindingRule = requireText(expectedBindingRule, "expectedBindingRule");
    actualPrincipalTypeId = normalize(actualPrincipalTypeId);
    actualPrincipalSurfaceId = normalize(actualPrincipalSurfaceId);
    actualTenantId = normalize(actualTenantId);
  }

  public boolean ReqsBindingComplianceDecisionIsAllowed() {
    return allowed;
  }

  public boolean ReqsBindingComplianceDecisionIsDenied() {
    return !allowed;
  }

  public String ReqsBindingComplianceDecisionDescribe() {
    return operatingSurface.OperatingSurfaceGetId()
        + "[allowed=" + allowed
        + ", code=" + decisionCode
        + ", actualStage=" + (actualBindingStage == null ? "null" : actualBindingStage.ReqsBindingStageGetId())
        + ", actualSource=" + (actualBindingSource == null ? "null" : actualBindingSource.TenantBindingSourceGetId())
        + ", actualPrincipalType=" + actualPrincipalTypeId
        + ", actualPrincipalSurface=" + actualPrincipalSurfaceId
        + ", actualTenantId=" + actualTenantId
        + ", expectedRule=" + expectedBindingRule
        + "]";
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName);
    String normalized = value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
