package mcp.server.foundation.resource_interface;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import mcp.server.foundation.prompt_interface.PromptReg;
import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;

@Service
public final class McpPromptCatalogService {

  private static final Path MCP_PROMPT_CATALOG_PATH = Path.of("server", "mcp_prompt_catalog.json");
  private static final String CATALOG_LABEL = "MCP prompt catalog";
  private static final String EXPECTED_CATALOG_ID = "mcp_prompt_catalog";

  private final Map<String, McpPromptCatalogEntry> promptsByName;

  public McpPromptCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    JsonNode root = catalogJsonLoader.loadCatalogObject(MCP_PROMPT_CATALOG_PATH);
    requireCatalogId(root, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(root, CATALOG_LABEL, "mcp_prompt_catalog_version");
    this.promptsByName = loadPrompts(root);
  }

  public void validateRegisteredPrompts(PromptReg promptReg) {
    validatePromptReferences(Objects.requireNonNull(promptReg, "promptReg").PromptRegListDefinitions().stream()
        .map(prompt -> prompt.PromptDefGetName())
        .toList());
  }

  public void validatePromptReferences(Collection<String> promptNames) {
    List<String> missingPrompts = Objects.requireNonNull(promptNames, "promptNames").stream()
        .filter(promptName -> !promptsByName.containsKey(promptName))
        .distinct()
        .toList();
    if (!missingPrompts.isEmpty()) {
      throw new IllegalStateException("MCP prompt catalog is missing prompt definitions: " + missingPrompts);
    }
  }

  private static Map<String, McpPromptCatalogEntry> loadPrompts(JsonNode root) {
    LinkedHashMap<String, McpPromptCatalogEntry> prompts = new LinkedHashMap<>();
    Set<String> promptNames = new LinkedHashSet<>();
    for (JsonNode promptNode : array(root, CATALOG_LABEL, "mcp_prompts")) {
      McpPromptCatalogEntry entry = new McpPromptCatalogEntry(
          requiredText(promptNode, CATALOG_LABEL, "mcp_prompt_name"),
          requiredText(promptNode, CATALOG_LABEL, "mcp_prompt_description"));
      array(promptNode, CATALOG_LABEL, "mcp_prompt_arguments");
      if (!promptNames.add(entry.name())) {
        throw new IllegalStateException("Duplicate MCP prompt catalog name: " + entry.name());
      }
      prompts.put(entry.name(), entry);
    }
    return Map.copyOf(prompts);
  }

  private record McpPromptCatalogEntry(
      String name,
      String description) {
  }
}
