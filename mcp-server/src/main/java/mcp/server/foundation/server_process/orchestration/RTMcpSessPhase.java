package mcp.server.foundation.server_process.orchestration;

/**
 * Canonical session phases across runtime surfaces.
 */
public enum RTMcpSessPhase {

  CREATED("created"),
  PRE_SESSION("pre_session"),
  PRE_INIT("pre_init"),
  ACTIVE("active"),
  RESUMED("resumed"),
  EXPIRED("expired"),
  CLOSED("closed");

  private final String id;

  RTMcpSessPhase(String id) {
    this.id = id;
  }

  public String RTMcpSessPhaseGetId() {
    return id;
  }
}
