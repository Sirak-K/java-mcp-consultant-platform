package mcp.server.foundation.server_process.orchestration;

/**
 * Canonical runtime-session families.
 */
public enum RTMcpSessType {

  MCP_SESSION("mcp_session"),
  APP_PRE_SESSION("app_pre_session"),
  BOUND_BUSINESS_SESSION("bound_business_session"),
  PLATFORM_OPS_SESSION("platform_ops_session");

  private final String id;

  RTMcpSessType(String id) {
    this.id = id;
  }

  public String RTMcpSessTypeGetId() {
    return id;
  }
}
