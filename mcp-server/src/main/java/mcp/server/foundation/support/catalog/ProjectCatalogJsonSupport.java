package mcp.server.foundation.support.catalog;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Shared JSON catalog node validation helpers.
 */
public final class ProjectCatalogJsonSupport {

  private ProjectCatalogJsonSupport() {
  }

  public static void requireCatalogId(
      JsonNode root,
      String catalogLabel,
      String expectedCatalogId) {

    String catalogId = requiredText(root, catalogLabel, "catalog_id");
    if (!expectedCatalogId.equals(catalogId)) {
      throw new IllegalStateException("Unexpected " + catalogLabel + " catalog_id: " + catalogId);
    }
  }

  public static List<JsonNode> array(
      JsonNode root,
      String catalogLabel,
      String fieldName) {

    JsonNode value = root == null ? null : root.path(fieldName);
    if (value == null || !value.isArray()) {
      throw new IllegalStateException(catalogLabel + " field must be an array: " + fieldName);
    }
    return java.util.stream.StreamSupport.stream(value.spliterator(), false).toList();
  }

  public static JsonNode object(
      JsonNode root,
      String catalogLabel,
      String fieldName) {

    JsonNode value = root == null ? null : root.path(fieldName);
    if (value == null || !value.isObject()) {
      throw new IllegalStateException(catalogLabel + " field must be an object: " + fieldName);
    }
    return value;
  }

  public static String requiredText(
      JsonNode node,
      String catalogLabel,
      String fieldName) {

    JsonNode value = node == null ? null : node.get(fieldName);
    if (value == null || !value.isTextual()) {
      throw new IllegalStateException(catalogLabel + " field is required: " + fieldName);
    }
    return requiredValue(value.asText(), catalogLabel, fieldName);
  }

  public static String optionalText(JsonNode node, String fieldName) {
    JsonNode value = node == null ? null : node.get(fieldName);
    if (value == null || !value.isTextual()) {
      return "";
    }
    return value.asText().trim();
  }

  public static String requiredArrayText(
      JsonNode node,
      String catalogLabel,
      String fieldName) {

    if (node == null || !node.isTextual()) {
      throw new IllegalStateException(catalogLabel + " array item must be text: " + fieldName);
    }
    return requiredValue(node.asText(), catalogLabel, fieldName);
  }

  public static List<String> textList(
      JsonNode root,
      String catalogLabel,
      String fieldName) {

    return array(root, catalogLabel, fieldName).stream()
        .map(value -> requiredArrayText(value, catalogLabel, fieldName))
        .toList();
  }

  public static int requiredInt(
      JsonNode node,
      String catalogLabel,
      String fieldName) {

    JsonNode value = node == null ? null : node.get(fieldName);
    if (value == null || !value.canConvertToInt()) {
      throw new IllegalStateException(catalogLabel + " integer field is required: " + fieldName);
    }
    return value.asInt();
  }

  public static boolean requiredBoolean(
      JsonNode node,
      String catalogLabel,
      String fieldName) {

    JsonNode value = node == null ? null : node.get(fieldName);
    if (value == null || !value.isBoolean()) {
      throw new IllegalStateException(catalogLabel + " field must be boolean: " + fieldName);
    }
    return value.asBoolean();
  }

  public static <K, V> void putUnique(
      Map<K, V> values,
      K key,
      V value,
      String catalogLabel,
      String label) {

    Objects.requireNonNull(values, "values");
    if (values.putIfAbsent(key, value) != null) {
      throw new IllegalStateException("Duplicate " + catalogLabel + " " + label + ": " + key);
    }
  }

  public static void requireUniqueTextValues(
      List<String> values,
      String catalogLabel,
      String fieldName) {

    Set<String> uniqueValues = new LinkedHashSet<>();
    for (String value : Objects.requireNonNull(values, "values")) {
      if (!uniqueValues.add(value)) {
        throw new IllegalStateException(catalogLabel + " field must be unique: " + fieldName);
      }
    }
  }

  public static <K, V> V requiredMapValue(
      Map<K, V> values,
      K key,
      String catalogLabel,
      String label) {

    V value = Objects.requireNonNull(values, "values").get(key);
    if (value == null) {
      throw new IllegalStateException(catalogLabel + " entry is missing: " + label + " " + key);
    }
    return value;
  }

  public static String requiredValue(
      String value,
      String catalogLabel,
      String fieldName) {

    String trimmed = value == null ? "" : value.trim();
    if (trimmed.isBlank()) {
      throw new IllegalStateException(catalogLabel + " field must not be blank: " + fieldName);
    }
    return trimmed;
  }
}
