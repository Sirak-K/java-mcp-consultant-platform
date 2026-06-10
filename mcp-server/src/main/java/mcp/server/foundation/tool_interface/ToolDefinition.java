package mcp.server.foundation.tool_interface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ToolDefinition
 *
 * Metadata + implementation wrapper.
 * Foundation-level.
 */
public final class ToolDefinition {

  private final String name;
  private final String title;
  private final String description;
  private final Map<String, Object> inputSchema;
  private final Map<String, Object> outputSchema;
  private final boolean readOnlyHint;
  private final boolean destructiveHint;
  private final boolean idempotentHint;
  private final boolean openWorldHint;
  private final List<Map<String, Object>> icons;
  private final String taskSupport;
  private final List<String> categories;
  private final List<Map<String, Object>> examples;
  private final ToolExecPolicy executionPolicy;
  private final ToolInterface implementation;

  public ToolDefinition(
      String name,
      String description,
      Map<String, Object> inputSchema,
      ToolInterface implementation) {

    this(
        name,
        description,
        inputSchema,
        ToolMetadataSupport.ToolMetaSupDefaultOutputSchema(),
        false,
        false,
        List.of(),
        List.of(),
        new ToolExecPolicy(10_000L, 32, true, false),
        implementation);
  }

  public ToolDefinition(
      String name,
      String description,
      Map<String, Object> inputSchema,
      Map<String, Object> outputSchema,
      boolean destructiveHint,
      boolean idempotentHint,
      List<String> categories,
      List<Map<String, Object>> examples,
      ToolExecPolicy executionPolicy,
      ToolInterface implementation) {

    this(
        name,
        ToolMetadataSupport.ToolMetaSupDefaultTitle(name),
        description,
        inputSchema,
        outputSchema,
        ToolMetadataSupport.ToolMetaSupDeriveReadOnlyHint(name),
        destructiveHint,
        idempotentHint,
        ToolMetadataSupport.ToolMetaSupDefaultOpenWorldHint(),
        List.of(),
        ToolMetadataSupport.ToolMetaSupDefaultTaskSupport(),
        categories,
        examples,
        executionPolicy,
        implementation);
  }

  public ToolDefinition(
      String name,
      String title,
      String description,
      Map<String, Object> inputSchema,
      Map<String, Object> outputSchema,
      boolean readOnlyHint,
      boolean destructiveHint,
      boolean idempotentHint,
      boolean openWorldHint,
      List<Map<String, Object>> icons,
      String taskSupport,
      List<String> categories,
      List<Map<String, Object>> examples,
      ToolExecPolicy executionPolicy,
      ToolInterface implementation) {

    this.name = requireNonBlank(name, "name");
    this.title = requireNonBlank(title, "title");
    this.description = requireNonBlank(description, "description");
    this.inputSchema = Map.copyOf(Objects.requireNonNull(inputSchema, "inputSchema"));
    this.outputSchema = Map.copyOf(Objects.requireNonNull(outputSchema, "outputSchema"));
    this.readOnlyHint = readOnlyHint;
    this.destructiveHint = destructiveHint;
    this.idempotentHint = idempotentHint;
    this.openWorldHint = openWorldHint;
    this.icons = copyMapList(icons, "icons");
    this.taskSupport = ToolMetadataSupport.ToolMetaSupNormalizeTaskSupport(taskSupport);
    this.categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
    this.examples = copyMapList(examples, "examples");
    this.executionPolicy = Objects.requireNonNull(executionPolicy, "executionPolicy");
    this.implementation = Objects.requireNonNull(implementation, "implementation");
  }

  public String ToolDefGetName() {
    return name;
  }

  public String ToolDefGetTitle() {
    return title;
  }

  public String ToolDefGetDescription() {
    return description;
  }

  public Map<String, Object> ToolDefGetInputSchema() {
    return inputSchema;
  }

  public Map<String, Object> ToolDefGetOutputSchema() {
    return outputSchema;
  }

  public boolean ToolDefGetReadOnlyHint() {
    return readOnlyHint;
  }

  public boolean ToolDefGetDestructiveHint() {
    return destructiveHint;
  }

  public boolean ToolDefGetIdempotentHint() {
    return idempotentHint;
  }

  public boolean ToolDefGetOpenWorldHint() {
    return openWorldHint;
  }

  public List<Map<String, Object>> ToolDefGetIcons() {
    return icons;
  }

  public String ToolDefGetTaskSupport() {
    return taskSupport;
  }

  public List<String> ToolDefGetCategories() {
    return categories;
  }

  public List<Map<String, Object>> ToolDefGetExamples() {
    return examples;
  }

  public ToolExecPolicy ToolDefGetExecPolicy() {
    return executionPolicy;
  }

  public ToolInterface ToolDefGetImplementation() {
    return implementation;
  }

  public Map<String, Object> ToolDefToMcpFormat() {

    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("name", name);
    result.put("title", title);
    result.put("description", description);
    result.put("inputSchema", inputSchema);
    result.put("outputSchema", outputSchema);
    result.put("annotations", ToolDefBuildAnnotations());
    if (!icons.isEmpty()) {
      result.put("icons", icons);
    }
    result.put("execution", Map.of("taskSupport", taskSupport));
    result.put("_meta", ToolDefBuildProjectMeta());
    return Map.copyOf(result);
  }

  public static ToolDefinition ToolDefFromToolInterface(
      ToolInterface tool,
      ToolExecPolicy executionPolicy) {

    Objects.requireNonNull(tool, "tool");
    Objects.requireNonNull(executionPolicy, "executionPolicy");

    return new ToolDefinition(
        tool.getName(),
        tool.getTitle(),
        tool.getDescription(),
        tool.getInputSchema(),
        tool.getOutputSchema(),
        tool.getReadOnlyHint(),
        tool.getDestructiveHint(),
        tool.getIdempotentHint(),
        tool.getOpenWorldHint(),
        tool.getIcons(),
        tool.getTaskSupport(),
        tool.getCategories(),
        tool.getExamples(),
        executionPolicy,
        tool);
  }

  private Map<String, Object> ToolDefBuildAnnotations() {
    return Map.of(
        "title", title,
        "readOnlyHint", readOnlyHint,
        "destructiveHint", destructiveHint,
        "idempotentHint", idempotentHint,
        "openWorldHint", openWorldHint);
  }

  private Map<String, Object> ToolDefBuildProjectMeta() {
    return Map.of(
        "categories", categories,
        "examples", examples,
        "executionPolicy", Map.of(
            "timeoutMs", executionPolicy.timeoutMillis(),
            "maxConcurrency", executionPolicy.maxConcurrency(),
            "cancellable", executionPolicy.cancellable(),
            "progressEnabled", executionPolicy.progressEnabled()));
  }

  private static List<Map<String, Object>> copyMapList(
      List<Map<String, Object>> values,
      String fieldName) {

    Objects.requireNonNull(values, fieldName);
    return values.stream()
        .map(value -> Map.copyOf(Objects.requireNonNull(value, fieldName + " entry")))
        .toList();
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName);
    String normalized = value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
