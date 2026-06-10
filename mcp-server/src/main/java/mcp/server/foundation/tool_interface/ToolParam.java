package mcp.server.foundation.tool_interface;

import com.fasterxml.jackson.databind.JsonNode;

public final class ToolParam {

  private final JsonNode node;

  public ToolParam(JsonNode node) {
    this.node = node;
  }

  public boolean ToolParamIsMissingOrNull() {
    return node == null || node.isNull();
  }

  public String ToolParamAsText() {

    if (ToolParamIsMissingOrNull()) {
      throw new IllegalArgumentException("Missing required parameter");
    }

    return node.asText();
  }

  public String ToolParamAsText(String defaultValue) {
    return ToolParamIsMissingOrNull() ? defaultValue : node.asText();
  }

  public boolean ToolParamAsBoolean(boolean defaultValue) {
    return ToolParamIsMissingOrNull() ? defaultValue : node.asBoolean();
  }

  public Integer ToolParamAsInt(Integer defaultValue) {

    if (ToolParamIsMissingOrNull()) {
      return defaultValue;
    }

    return node.asInt();
  }

  public long ToolParamAsLong() {

    if (ToolParamIsMissingOrNull()) {
      throw new IllegalArgumentException("Missing required parameter");
    }

    return node.asLong();
  }
}
