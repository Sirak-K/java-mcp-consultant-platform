package mcp.server.foundation.prompt_interface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Rendered prompt payload after loading and argument interpolation.
 */
public final class PromptRenderResult {

  private final PromptDefin definition;
  private final String renderedText;
  private final Map<String, Object> arguments;

  public PromptRenderResult(
      PromptDefin definition,
      String renderedText,
      Map<String, Object> arguments) {

    this.definition = Objects.requireNonNull(definition, "definition");
    this.renderedText = PromptSupport.requireNonBlank(renderedText, "renderedText");
    this.arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
  }

  public PromptDefin PromptRenderResGetDefin() {
    return definition;
  }

  public String PromptRenderResGetRenderedText() {
    return renderedText;
  }

  public Map<String, Object> PromptRenderResGetArguments() {
    return arguments;
  }

  public Map<String, Object> PromptRenderResToMcpFormat() {

    LinkedHashMap<String, Object> projectMeta = new LinkedHashMap<>(definition.PromptDefBuildProjectMeta());
    projectMeta.put("name", definition.PromptDefGetName());
    projectMeta.put("title", definition.PromptDefGetTitle());
    projectMeta.put("arguments", arguments);

    return Map.of(
        "description", definition.PromptDefGetDescription(),
        "messages", List.of(
            Map.of(
                "role", "user",
                "content", Map.of(
                    "type", "text",
                    "text", renderedText))),
        "_meta", Map.copyOf(projectMeta));
  }
}
