package mcp.server.foundation.observability.tracing;

public final class McpObservationCatal {

  public static final String MCP_RPC_ROUTE = "mcp.rpc.route";
  public static final String MCP_TOOL_INVOKE = "mcp.tool.invoke";
  public static final String MCP_PERSISTENCE_CALL = "mcp.persistence.call";

  public static final String FIELD_TRANSPORT = "transport";
  public static final String FIELD_SESSION_PHASE = "session.phase";
  public static final String FIELD_ERROR_TYPE = "error.type";
  public static final String FIELD_RPC_METHOD = "rpc.method";
  public static final String FIELD_TOOL_NAME = "tool.name";
  public static final String FIELD_PERSISTENCE_REPOSITORY = "persistence.repository";
  public static final String FIELD_PERSISTENCE_OPERATION = "persistence.operation";

  private McpObservationCatal() {
  }
}
