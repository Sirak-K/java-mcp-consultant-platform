package mcp.server.foundation.security.request_binding;

import mcp.server.foundation.server_process.orchestration.OperatingSurface;

import java.util.Objects;

/**
 * Compact runtime contract for one first-class request surface.
 */
public final class ReqsLifecyContract {

  private final OperatingSurface operatingSurface;
  private final boolean authRequired;
  private final boolean preSessionAllowed;
  private final boolean assumeTenantAllowed;
  private final ReqsBindingStage defaultStage;
  private final String summary;

  public ReqsLifecyContract(
      OperatingSurface operatingSurface,
      boolean authRequired,
      boolean preSessionAllowed,
      boolean assumeTenantAllowed,
      ReqsBindingStage defaultStage,
      String summary) {

    this.operatingSurface = Objects.requireNonNull(operatingSurface, "operatingSurface");
    this.authRequired = authRequired;
    this.preSessionAllowed = preSessionAllowed;
    this.assumeTenantAllowed = assumeTenantAllowed;
    this.defaultStage = Objects.requireNonNull(defaultStage, "defaultStage");
    this.summary = requireText(summary, "summary");
  }

  public OperatingSurface ReqsLifecyContractGetSurface() {
    return operatingSurface;
  }

  public boolean ReqsLifecyContractIsAuthRequired() {
    return authRequired;
  }

  public boolean ReqsLifecyContractIsPreSessionAllowed() {
    return preSessionAllowed;
  }

  public boolean ReqsLifecyContractIsAssumeTenantAllowed() {
    return assumeTenantAllowed;
  }

  public ReqsBindingStage ReqsLifecyContractGetDefaultStage() {
    return defaultStage;
  }

  public String ReqsLifecyContractGetSummary() {
    return summary;
  }

  public String ReqsLifecyContractDescribe() {
    return operatingSurface.OperatingSurfaceGetId()
        + "[authRequired=" + authRequired
        + ", preSessionAllowed=" + preSessionAllowed
        + ", assumeTenantAllowed=" + assumeTenantAllowed
        + ", defaultStage=" + defaultStage.ReqsBindingStageGetId()
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
}
