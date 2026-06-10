package mcp.server.foundation.tool_interface;

public final class ToolExecCancelledExcep extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ToolExecCancelledExcep(String message) {
    super(message);
  }
}
