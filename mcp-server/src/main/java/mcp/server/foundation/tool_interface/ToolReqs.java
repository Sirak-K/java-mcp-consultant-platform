package mcp.server.foundation.tool_interface;

import com.fasterxml.jackson.databind.JsonNode;

public final class ToolReqs {

  private final String toolName;
  private final JsonNode arguments;

  private ToolReqs(String toolName, JsonNode arguments) {
    this.toolName = toolName;
    this.arguments = arguments;
  }

  public static ToolReqs ToolReqFromRpc(
      String toolName,
      JsonNode params) {

    if (toolName == null || toolName.isBlank()) {
      throw new IllegalArgumentException("Tool name must not be blank");
    }

    return new ToolReqs(toolName, params);
  }

  public String ToolReqGetToolName() {
    return toolName;
  }

  public JsonNode ToolReqGetArguments() {
    return arguments;
  }

  public ToolParam ToolReqParam(String name) {

    if (arguments == null || arguments.isNull()) {
      return new ToolParam(null);
    }

    return new ToolParam(arguments.get(name));
  }
}
