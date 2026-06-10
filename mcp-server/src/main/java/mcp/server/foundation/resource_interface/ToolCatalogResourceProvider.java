package mcp.server.foundation.resource_interface;

import mcp.server.foundation.tool_interface.ToolDefinition;
import mcp.server.foundation.tool_interface.ToolReg;
import mcp.server.foundation.rpc.RPCCapaDscr;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ToolCatalogResourceProvider implements ResrcProvid {

  private final String resourceUri;
  private final String resourceName;
  private final ToolReg toolRegistry;

  public ToolCatalogResourceProvider(
      String resourceUri,
      String resourceName,
      ToolReg toolRegistry) {
    this.resourceUri = Objects.requireNonNull(resourceUri, "resourceUri");
    this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
    this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
  }

  @Override
  public Map<String, Object> ResourceProvRead() {
    return readCatalog(null);
  }

  public Map<String, Object> ResourceProvReadFiltered(List<String> toolNames) {
    Objects.requireNonNull(toolNames, "toolNames");
    LinkedHashSet<String> requestedToolNames = new LinkedHashSet<>();
    for (String toolName : toolNames) {
      if (toolName != null && !toolName.isBlank()) {
        requestedToolNames.add(toolName.trim());
      }
    }
    return readCatalog(requestedToolNames);
  }

  private Map<String, Object> readCatalog(Set<String> requestedToolNames) {

    List<ToolDefinition> definitions = toolRegistry.ToolRegListDefinitions()
        .stream()
        .filter(definition -> requestedToolNames == null
            || requestedToolNames.contains(definition.ToolDefGetName()))
        .toList();

    List<Map<String, Object>> tools = definitions.stream()
        .map(ToolDefinition::ToolDefToMcpFormat)
        .toList();

    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("uri", resourceUri);
    result.put("resource", resourceName);
    result.put("mcpProtocolVersion", RPCCapaDscr.MCP_PROTOCOL_VERSION);
    result.put("supportedProtocolVersions", RPCCapaDscr.RPCCapaDescSupportedProtocolVersions());
    result.put("contract", Map.of(
        "metadataPlacement", "_meta",
        "structuredContent", true,
        "jsonTextFallback", true,
        "taskSupportDefault", "forbidden"));
    result.put("toolCount", tools.size());
    result.put("tools", tools);

    if (requestedToolNames != null) {
      Set<String> matchedToolNames = definitions.stream()
          .map(ToolDefinition::ToolDefGetName)
          .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
      List<String> missingToolNames = requestedToolNames.stream()
          .filter(toolName -> !matchedToolNames.contains(toolName))
          .toList();
      result.put("requestedToolNames", List.copyOf(requestedToolNames));
      result.put("missingToolNames", missingToolNames);
    }

    return Map.copyOf(result);
  }
}
