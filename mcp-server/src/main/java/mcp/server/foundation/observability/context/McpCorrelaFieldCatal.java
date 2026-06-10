package mcp.server.foundation.observability.context;

public final class McpCorrelaFieldCatal {

  public static final String MCP_SESSION_ID = "mcp.session.id";
  public static final String MCP_WS_CONNECTION_ID = "mcp.ws.connection.id";
  public static final String MCP_RPC_CORRELATION_ID = "mcp.rpc.correlation.id";
  public static final String MCP_RPC_METHOD = "mcp.rpc.method";
  public static final String MCP_TOOL_NAME = "mcp.tool.name";
  public static final String MCP_TRANSPORT_NAME = "mcp.transport.name";
  public static final String MCP_SESSION_PHASE = "mcp.session.phase";
  public static final String CLIENT_ADDRESS = "client.address";
  public static final String ERROR_TYPE = "error.type";
  public static final String TRACE_ID = "trace_id";
  public static final String SPAN_ID = "span_id";

  private McpCorrelaFieldCatal() {
  }
}
