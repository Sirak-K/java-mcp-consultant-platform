package mcp.server.foundation.prompt_interface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable wrapper around the JSON-schema-like argument contract for a prompt.
 */
public final class PromptArgumentSchema {

  private final Map<String, Object> schema;

  public PromptArgumentSchema(Map<String, Object> schema) {
    this.schema = Map.copyOf(Objects.requireNonNull(schema, "schema"));
  }

  public Map<String, Object> PromptArgSchemaToMcpFormat() {
    return schema;
  }

  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> PromptArgSchemaToPromptArgumentsMcpFormat() {

    Object propertiesRaw = schema.get("properties");
    if (!(propertiesRaw instanceof Map<?, ?> propertiesMapRaw)) {
      return List.of();
    }

    List<String> requiredProperties = List.of();
    Object requiredRaw = schema.get("required");
    if (requiredRaw instanceof List<?> requiredList) {
      requiredProperties = requiredList.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .toList();
    }

    java.util.LinkedHashSet<String> requiredSet = new java.util.LinkedHashSet<>(requiredProperties);
    java.util.ArrayList<Map<String, Object>> arguments = new java.util.ArrayList<>();

    for (Map.Entry<?, ?> entry : propertiesMapRaw.entrySet()) {
      if (!(entry.getKey() instanceof String argumentName) || argumentName.isBlank()) {
        continue;
      }

      LinkedHashMap<String, Object> argument = new LinkedHashMap<>();
      argument.put("name", argumentName);
      if (entry.getValue() instanceof Map<?, ?> argumentSchemaRaw) {
        Map<String, Object> argumentSchema = (Map<String, Object>) argumentSchemaRaw;
        putTextIfPresent(argument, "title", argumentSchema.get("title"));
        putTextIfPresent(argument, "description", argumentSchema.get("description"));
      }
      if (requiredSet.contains(argumentName)) {
        argument.put("required", true);
      }
      arguments.add(Map.copyOf(argument));
    }

    return List.copyOf(arguments);
  }

  public static PromptArgumentSchema PromptArgSchemaEmptyObject() {
    return new PromptArgumentSchema(Map.of(
        "type", "object",
        "properties", Map.of()));
  }

  private static void putTextIfPresent(
      LinkedHashMap<String, Object> target,
      String key,
      Object value) {

    if (value instanceof String text && !text.isBlank()) {
      target.put(key, text);
    }
  }
}
