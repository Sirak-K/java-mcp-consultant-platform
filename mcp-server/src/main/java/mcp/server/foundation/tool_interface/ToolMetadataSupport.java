package mcp.server.foundation.tool_interface;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class ToolMetadataSupport {

  private ToolMetadataSupport() {
  }

  static Map<String, Object> ToolMetaSupDefaultOutputSchema() {
    return Map.of(
        "$schema", "https://json-schema.org/draft/2020-12/schema",
        "type", "object",
        "additionalProperties", true);
  }

  static String ToolMetaSupDefaultTitle(String toolName) {

    Objects.requireNonNull(toolName, "toolName");

    String normalized = toolName.trim();
    if (normalized.isEmpty()) {
      return toolName;
    }

    return normalized;
  }

  static List<String> ToolMetaSupDefaultCategories(String toolName) {

    Objects.requireNonNull(toolName, "toolName");

    if (toolName.startsWith("system.")) {
      return List.of("foundation", "system");
    }

    int delimiter = toolName.indexOf('.');
    if (delimiter <= 0) {
      return List.of("general");
    }

    return List.of(toolName.substring(0, delimiter));
  }

  static boolean ToolMetaSupDeriveDestructiveHint(String toolName) {
    String action = actionToken(toolName);
    return action.startsWith("delete")
        || action.startsWith("detach");
  }

  static boolean ToolMetaSupDeriveReadOnlyHint(String toolName) {
    String action = actionToken(toolName);
    return action.startsWith("get")
        || action.startsWith("list")
        || action.startsWith("find")
        || action.startsWith("lookup")
        || action.startsWith("inspect")
        || action.startsWith("preview")
        || "ping".equals(action)
        || "healthcheck".equals(action);
  }

  static boolean ToolMetaSupDeriveIdempotentHint(String toolName) {
    String action = actionToken(toolName);
    return ToolMetaSupDeriveReadOnlyHint(toolName)
        || action.startsWith("setas");
  }

  static boolean ToolMetaSupDefaultOpenWorldHint() {
    return true;
  }

  static String ToolMetaSupDefaultTaskSupport() {
    return "forbidden";
  }

  static String ToolMetaSupNormalizeTaskSupport(String taskSupport) {

    if (taskSupport == null || taskSupport.isBlank()) {
      return ToolMetaSupDefaultTaskSupport();
    }

    String normalized = taskSupport.trim().toLowerCase(Locale.ROOT);
    if ("forbidden".equals(normalized)
        || "optional".equals(normalized)
        || "required".equals(normalized)) {
      return normalized;
    }

    throw new IllegalArgumentException("Unsupported taskSupport: " + taskSupport);
  }

  static List<Map<String, Object>> ToolMetaSupGenerateExamples(
      String toolName,
      Map<String, Object> inputSchema) {

    Objects.requireNonNull(toolName, "toolName");
    Objects.requireNonNull(inputSchema, "inputSchema");

    Map<String, Object> arguments = generateExampleArguments(inputSchema);

    return List.of(
        Map.of(
            "title", "Example request",
            "tool", toolName,
            "arguments", arguments));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> generateExampleArguments(Map<String, Object> inputSchema) {

    Object propertiesRaw = inputSchema.get("properties");
    if (!(propertiesRaw instanceof Map<?, ?> propertiesMapRaw)) {
      return Map.of();
    }

    Map<String, Object> properties = (Map<String, Object>) propertiesMapRaw;
    List<String> requiredProperties = new ArrayList<>();
    Object requiredRaw = inputSchema.get("required");
    if (requiredRaw instanceof Iterable<?> iterable) {
      for (Object candidate : iterable) {
        if (candidate instanceof String propertyName && !propertyName.isBlank()) {
          requiredProperties.add(propertyName);
        }
      }
    }

    java.util.LinkedHashMap<String, Object> exampleArguments = new java.util.LinkedHashMap<>();
    if (!requiredProperties.isEmpty()) {
      for (String requiredProperty : requiredProperties) {
        exampleArguments.put(requiredProperty, exampleValueFor(requiredProperty, properties.get(requiredProperty)));
      }
      return Map.copyOf(exampleArguments);
    }

    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      exampleArguments.put(entry.getKey(), exampleValueFor(entry.getKey(), entry.getValue()));
      break;
    }

    return exampleArguments.isEmpty() ? Map.of() : Map.copyOf(exampleArguments);
  }

  @SuppressWarnings("unchecked")
  private static Object exampleValueFor(String propertyName, Object propertySchemaRaw) {

    if (!(propertySchemaRaw instanceof Map<?, ?> propertySchemaMapRaw)) {
      return "example-" + propertyName;
    }

    Map<String, Object> propertySchema = (Map<String, Object>) propertySchemaMapRaw;
    Object enumRaw = propertySchema.get("enum");
    if (enumRaw instanceof List<?> enumValues && !enumValues.isEmpty()) {
      return enumValues.get(0);
    }

    String type = String.valueOf(propertySchema.getOrDefault("type", "string")).toLowerCase(Locale.ROOT);
    return switch (type) {
      case "boolean" -> Boolean.TRUE;
      case "integer" -> 1;
      case "number" -> 1.0;
      case "array" -> List.of();
      case "object" -> Map.of();
      default -> exampleStringFor(propertyName);
    };
  }

  private static String exampleStringFor(String propertyName) {
    String normalized = propertyName.toLowerCase(Locale.ROOT);

    if (normalized.endsWith("id")) {
      return "example-id";
    }
    if (normalized.contains("email")) {
      return "example@example.com";
    }
    if (normalized.contains("phone")) {
      return "+46-555-0100";
    }
    if (normalized.contains("name")) {
      return "Example " + propertyName;
    }
    if (normalized.contains("title")) {
      return "Example Title";
    }

    return "example-" + propertyName;
  }

  private static String actionToken(String toolName) {
    Objects.requireNonNull(toolName, "toolName");

    int delimiter = toolName.lastIndexOf('.');
    if (delimiter < 0 || delimiter == toolName.length() - 1) {
      return toolName.toLowerCase(Locale.ROOT);
    }

    return toolName.substring(delimiter + 1).toLowerCase(Locale.ROOT);
  }
}
