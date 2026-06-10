package mcp.server.foundation.security.request_binding;

/**
 * High-level binding stage for one request or bound session phase.
 */
public enum ReqsBindingStage {

  PRE_SESSION("pre_session"),
  TENANT_BOUND("tenant_bound"),
  PLATFORM_BOUND("platform_bound");

  private final String id;

  ReqsBindingStage(String id) {
    this.id = id;
  }

  public String ReqsBindingStageGetId() {
    return id;
  }
}
