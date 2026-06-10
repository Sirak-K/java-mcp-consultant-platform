package mcp.server.foundation.prompt_interface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata + resource binding for a server-owned MCP prompt.
 */
public final class PromptDefin {

  private final String name;
  private final String title;
  private final String description;
  private final PromptArgumentSchema argumentSchema;
  private final String resourcePath;
  private final List<Map<String, Object>> icons;
  private final List<String> recommendedTools;

  public PromptDefin(
      String name,
      String title,
      String description,
      PromptArgumentSchema argumentSchema,
      String resourcePath,
      List<String> recommendedTools) {

    this(
        name,
        title,
        description,
        argumentSchema,
        resourcePath,
        List.of(),
        recommendedTools);
  }

  public PromptDefin(
      String name,
      String title,
      String description,
      PromptArgumentSchema argumentSchema,
      String resourcePath,
      List<Map<String, Object>> icons,
      List<String> recommendedTools) {

    this.name = PromptSupport.requireNonBlank(name, "name");
    this.title = PromptSupport.requireNonBlank(title, "title");
    this.description = PromptSupport.requireNonBlank(description, "description");
    this.argumentSchema = Objects.requireNonNull(argumentSchema, "argumentSchema");
    this.resourcePath = PromptSupport.requireNonBlank(resourcePath, "resourcePath");
    this.icons = copyMapList(icons, "icons");
    this.recommendedTools = List.copyOf(Objects.requireNonNull(recommendedTools, "recommendedTools"));
  }

  public String PromptDefGetName() {
    return name;
  }

  public String PromptDefGetTitle() {
    return title;
  }

  public String PromptDefGetDescription() {
    return description;
  }

  public PromptArgumentSchema PromptDefGetArgumentSchema() {
    return argumentSchema;
  }

  public String PromptDefGetResrcPath() {
    return resourcePath;
  }

  public List<Map<String, Object>> PromptDefGetIcons() {
    return icons;
  }

  public List<String> PromptDefGetRecommendedTools() {
    return recommendedTools;
  }

  public Map<String, Object> PromptDefToMcpFormat() {

    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("name", name);
    result.put("title", title);
    result.put("description", description);
    List<Map<String, Object>> promptArguments = argumentSchema.PromptArgSchemaToPromptArgumentsMcpFormat();
    if (!promptArguments.isEmpty()) {
      result.put("arguments", promptArguments);
    }
    if (!icons.isEmpty()) {
      result.put("icons", icons);
    }
    result.put("_meta", PromptDefBuildProjectMeta());
    return Map.copyOf(result);
  }

  Map<String, Object> PromptDefBuildProjectMeta() {
    return Map.of(
        "resourcePath", resourcePath,
        "recommendedTools", recommendedTools);
  }

  private static List<Map<String, Object>> copyMapList(
      List<Map<String, Object>> values,
      String fieldName) {

    Objects.requireNonNull(values, fieldName);
    return values.stream()
        .map(value -> Map.copyOf(Objects.requireNonNull(value, fieldName + " entry")))
        .toList();
  }
}
