package mcp.server.foundation.tool_interface;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ToolResponse
 *
 * MCP tool result wrapper.
 */
public final class ToolResponse {

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private enum Kind {
    TEXT,
    STRUCTURED,
    ERROR
  }

  private final Kind kind;
  private final List<Map<String, Object>> content;
  private final Map<String, Object> json;
  private final boolean error;

  private ToolResponse(
      Kind kind,
      List<Map<String, Object>> content,
      Map<String, Object> json,
      boolean error) {
    this.kind = Objects.requireNonNull(kind, "kind");
    this.content = List.copyOf(Objects.requireNonNull(content, "content").stream()
        .map(Map::copyOf)
        .collect(Collectors.toList()));
    this.json = copyJsonAllowingNullValues(json);
    this.error = error;
  }

  public static ToolResponse ToolRespText(String text) {

    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Text must not be null or blank");
    }

    return new ToolResponse(Kind.TEXT, List.of(textContent(text)), null, false);
  }

  public static ToolResponse ToolRespSuccess(Map<String, Object> json) {

    if (json == null) {
      throw new IllegalArgumentException("json must not be null");
    }

    return new ToolResponse(Kind.STRUCTURED, List.of(textContent(toJsonText(json))), json, false);
  }

  public static ToolResponse ToolRespStructured(Map<String, Object> structuredContent, String text) {

    if (structuredContent == null) {
      throw new IllegalArgumentException("structuredContent must not be null");
    }
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("text must not be null or blank");
    }

    return new ToolResponse(Kind.STRUCTURED, List.of(textContent(text)), structuredContent, false);
  }

  public static ToolResponse ToolRespError(String text) {

    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("text must not be null or blank");
    }

    return new ToolResponse(Kind.ERROR, List.of(textContent(text)), null, true);
  }

  public Map<String, Object> ToolRespToMcpFormat() {

    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("content", content);
    if (kind == Kind.STRUCTURED) {
      result.put("structuredContent", json);
    }
    if (error) {
      result.put("isError", true);
    }
    return Map.copyOf(result);
  }

  private static Map<String, Object> textContent(String text) {
    return Map.of(
        "type", "text",
        "text", text);
  }

  private static String toJsonText(Map<String, Object> json) {
    try {
      return JSON_MAPPER.writeValueAsString(json);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("json must be serializable", ex);
    }
  }

  private static Map<String, Object> copyJsonAllowingNullValues(Map<String, Object> json) {
    if (json == null) {
      return Map.of();
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(json));
  }
}
