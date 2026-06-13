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

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;
import mcp.server.foundation.tool_interface.ToolReg;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.object;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;

@Service
public final class McpToolCatalogService {

  private static final Path MCP_TOOL_CATALOG_PATH = Path.of("server", "mcp_tool_catalog.json");
  private static final String CATALOG_LABEL = "MCP tool catalog";
  private static final String EXPECTED_CATALOG_ID = "mcp_tool_catalog";

  private final Map<String, McpToolCatalogEntry> toolsByName;

  public McpToolCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    JsonNode root = catalogJsonLoader.loadCatalogObject(MCP_TOOL_CATALOG_PATH);
    requireCatalogId(root, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(root, CATALOG_LABEL, "mcp_tool_catalog_version");
    this.toolsByName = loadTools(root);
  }

  public void validateRegisteredTools(ToolReg toolReg) {
    validateToolReferences(Objects.requireNonNull(toolReg, "toolReg").ToolRegListNames());
  }

  public void validateToolReferences(Collection<String> toolNames) {
    List<String> missingTools = Objects.requireNonNull(toolNames, "toolNames").stream()
        .filter(toolName -> !toolsByName.containsKey(toolName))
        .distinct()
        .toList();
    if (!missingTools.isEmpty()) {
      throw new IllegalStateException("MCP tool catalog is missing tool definitions: " + missingTools);
    }
  }

  private static Map<String, McpToolCatalogEntry> loadTools(JsonNode root) {
    LinkedHashMap<String, McpToolCatalogEntry> tools = new LinkedHashMap<>();
    Set<String> toolNames = new LinkedHashSet<>();
    for (JsonNode toolNode : array(root, CATALOG_LABEL, "mcp_tools")) {
      McpToolCatalogEntry entry = new McpToolCatalogEntry(
          requiredText(toolNode, CATALOG_LABEL, "mcp_tool_name"),
          requiredText(toolNode, CATALOG_LABEL, "mcp_tool_description"));
      object(toolNode, CATALOG_LABEL, "mcp_tool_input_schema");
      object(toolNode, CATALOG_LABEL, "mcp_tool_output_schema");
      if (!toolNames.add(entry.name())) {
        throw new IllegalStateException("Duplicate MCP tool catalog name: " + entry.name());
      }
      tools.put(entry.name(), entry);
    }
    if (tools.isEmpty()) {
      throw new IllegalStateException("MCP tool catalog must contain at least one tool.");
    }
    return Map.copyOf(tools);
  }

  private record McpToolCatalogEntry(
      String name,
      String description) {
  }
}
