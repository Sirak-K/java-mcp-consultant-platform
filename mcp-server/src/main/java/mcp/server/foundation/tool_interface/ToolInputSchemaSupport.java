package mcp.server.foundation.tool_interface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds JSON Schema fragments for MCP tool input schemas.
 */
public final class ToolInputSchemaSupport {

    private ToolInputSchemaSupport() {
    }

    public static Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required) {
        return Map.of(
                "type", "object",
                "properties", Map.copyOf(Objects.requireNonNull(properties, "properties")),
                "required", List.copyOf(Objects.requireNonNull(required, "required")));
    }

    public static Map<String, Object> emptyObjectSchema() {
        return closedObjectSchema(Map.of(), List.of());
    }

    public static Map<String, Object> closedObjectSchema(
            Map<String, Object> properties,
            List<String> required) {

        LinkedHashMap<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.copyOf(Objects.requireNonNull(properties, "properties")));
        schema.put("required", List.copyOf(Objects.requireNonNull(required, "required")));
        schema.put("additionalProperties", false);
        return Map.copyOf(schema);
    }

    public static Map<String, Object> arraySchema(Map<String, Object> itemsSchema) {
        return Map.of(
                "type", "array",
                "items", Map.copyOf(Objects.requireNonNull(itemsSchema, "itemsSchema")));
    }

    public static Map<String, Object> integerSchema() {
        return Map.of("type", "integer");
    }

    public static Map<String, Object> integerSchema(String description) {
        return Map.of("type", "integer", "description", description);
    }

    public static Map<String, Object> integerMinimumSchema(
            int minimum,
            String description) {
        return Map.of(
                "type", "integer",
                "minimum", minimum,
                "description", description);
    }

    public static Map<String, Object> stringSchema(String description) {
        return Map.of("type", "string", "description", description);
    }

    public static Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    public static Map<String, Object> booleanSchema(String description) {
        return Map.of("type", "boolean", "description", description);
    }

    public static Map<String, Object> booleanSchema() {
        return Map.of("type", "boolean");
    }

    public static Map<String, Object> stringEnumSchema(
            List<String> values,
            String description) {
        return Map.of(
                "type", "string",
                "enum", List.copyOf(Objects.requireNonNull(values, "values")),
                "description", description);
    }
}
