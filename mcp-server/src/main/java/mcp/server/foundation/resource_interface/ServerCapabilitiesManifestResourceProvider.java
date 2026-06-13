package mcp.server.foundation.resource_interface;

import mcp.server.foundation.resource_interface.ServerCapabilityCatalog.ServerCapability;
import mcp.server.foundation.prompt_interface.PromptReg;
import mcp.server.foundation.rpc.RPCCapaDscr;
import mcp.server.foundation.tool_interface.ToolReg;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP resource exposing the server capability contract catalog.
 */
public final class ServerCapabilitiesManifestResourceProvider implements ResrcProvid {

    private final String resourceUri;
    private final String resourceName;
    private final ResrcReg resourceReg;
    private final ToolReg toolReg;
    private final PromptReg promptReg;
    private final McpToolCatalogService toolCatalogService;
    private final McpPromptCatalogService promptCatalogService;
    private final ServerCapabilitiesManifestService serverCapabilitiesManifestService;

    public ServerCapabilitiesManifestResourceProvider(
            String resourceUri,
            String resourceName,
            ResrcReg resourceReg,
            ToolReg toolReg,
            PromptReg promptReg,
            McpToolCatalogService toolCatalogService,
            McpPromptCatalogService promptCatalogService,
            ServerCapabilitiesManifestService serverCapabilitiesManifestService) {
        this.resourceUri = Objects.requireNonNull(resourceUri, "resourceUri");
        this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
        this.resourceReg = Objects.requireNonNull(resourceReg, "resourceReg");
        this.toolReg = Objects.requireNonNull(toolReg, "toolReg");
        this.promptReg = Objects.requireNonNull(promptReg, "promptReg");
        this.toolCatalogService = Objects.requireNonNull(toolCatalogService, "toolCatalogService");
        this.promptCatalogService = Objects.requireNonNull(promptCatalogService, "promptCatalogService");
        this.serverCapabilitiesManifestService = Objects.requireNonNull(
                serverCapabilitiesManifestService,
                "serverCapabilitiesManifestService");
    }

    @Override
    public Map<String, Object> ResourceProvRead() {
        Map<String, Map<String, Object>> resourcesByUri = resourcesByUri();
        List<ServerCapability> serverCapabilities = serverCapabilitiesManifestService.listServerCapabilities();
        toolCatalogService.validateRegisteredTools(toolReg);
        promptCatalogService.validateRegisteredPrompts(promptReg);
        validateMcpTools(serverCapabilities);
        validateMcpResources(serverCapabilities, resourcesByUri);
        validateMcpPrompts(serverCapabilities);
        List<Map<String, Object>> capabilities = serverCapabilities.stream()
                .map(capability -> capabilityEntry(capability, resourcesByUri))
                .toList();

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("uri", resourceUri);
        result.put("resource", resourceName);
        result.put("trinityLayer", "Resources");
        result.put("mcpProtocolVersion", RPCCapaDscr.MCP_PROTOCOL_VERSION);
        result.put("supportedProtocolVersions", RPCCapaDscr.RPCCapaDescSupportedProtocolVersions());
        result.put("runtimePosture", runtimePosture());
        result.put("capabilityCount", capabilities.size());
        result.put("capabilities", capabilities);
        return Map.copyOf(result);
    }

    private Map<String, Object> capabilityEntry(
            ServerCapability capability,
            Map<String, Map<String, Object>> resourcesByUri) {

        McpResourceResolution resourceResolution = resolveMcpResources(capability, resourcesByUri);

        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", capability.id());
        entry.put("title", capability.title());
        entry.put("riskClass", capability.riskClass().name());
        entry.put("backendEndpoints", capability.backendEndpoints());
        entry.put("mcpTools", capability.mcpTools());
        entry.put("mcpResources", capability.mcpResources());
        entry.put("mcpPrompts", capability.mcpPrompts());
        entry.put("mcpSurfaces", capability.mcpSurfaces());
        entry.put("registeredResourceCount", resourceResolution.registeredResourceCount());
        entry.put("resources", resourceResolution.resources());
        entry.put("missingResourceUris", resourceResolution.missingResourceUris());
        entry.put("reactSurfaces", capability.reactSurfaces());
        entry.put("persistenceAnchors", capability.persistenceAnchors());
        return Map.copyOf(entry);
    }

    private void validateMcpPrompts(List<ServerCapability> capabilities) {
        Set<String> registeredPromptNames = promptReg.PromptRegListDefinitions().stream()
                .map(prompt -> prompt.PromptDefGetName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        promptCatalogService.validatePromptReferences(registeredPromptNames);
        List<String> missingPromptNames = capabilities.stream()
                .flatMap(capability -> capability.mcpPrompts().stream())
                .filter(promptName -> !registeredPromptNames.contains(promptName))
                .distinct()
                .toList();
        if (!missingPromptNames.isEmpty()) {
            throw new IllegalStateException("Server capability manifest references unregistered MCP prompts: "
                    + missingPromptNames);
        }
    }

    private Map<String, Object> runtimePosture() {
        return Map.of(
                "foundationRuntime", "mcp-server/foundation",
                "sdkRuntimeDependency", false,
                "primaryTransport", "streamable-http",
                "statefulSessionHeader", "MCP-Session-Id",
                "postInitProtocolVersionHeader", "MCP-Protocol-Version",
                "fullOAuthResourceServerCompliance", false,
                "projectMetadataPlacement", "_meta",
                "structuredToolOutput", true);
    }

    private void validateMcpTools(List<ServerCapability> capabilities) {
        Set<String> registeredToolNames = toolReg.ToolRegListNames();
        toolCatalogService.validateToolReferences(registeredToolNames);
        List<String> missingToolNames = capabilities.stream()
                .flatMap(capability -> capability.mcpTools().stream())
                .filter(toolName -> !registeredToolNames.contains(toolName))
                .distinct()
                .toList();
        if (!missingToolNames.isEmpty()) {
            throw new IllegalStateException("Server capability manifest references unregistered MCP tools: "
                    + missingToolNames);
        }
    }

    private void validateMcpResources(
            List<ServerCapability> capabilities,
            Map<String, Map<String, Object>> resourcesByUri) {

        List<String> missingResourceUris = capabilities.stream()
                .flatMap(capability -> capability.mcpResources().stream())
                .filter(resourceUri -> !resourcesByUri.containsKey(resourceUri))
                .distinct()
                .toList();
        if (!missingResourceUris.isEmpty()) {
            throw new IllegalStateException("Server capability manifest references unregistered MCP resources: "
                    + missingResourceUris);
        }
    }

    private McpResourceResolution resolveMcpResources(
            ServerCapability capability,
            Map<String, Map<String, Object>> resourcesByUri) {

        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(resourcesByUri, "resourcesByUri");

        List<Map<String, Object>> resources = capability.mcpResources().stream()
                .map(resourcesByUri::get)
                .filter(Objects::nonNull)
                .toList();
        Set<String> registeredResourceUris = resources.stream()
                .map(resource -> String.valueOf(resource.get("uri")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missingResourceUris = capability.mcpResources().stream()
                .filter(resourceUri -> !registeredResourceUris.contains(resourceUri))
                .toList();
        return new McpResourceResolution(resources, missingResourceUris);
    }

    private Map<String, Map<String, Object>> resourcesByUri() {
        return resourceReg.ResrcRegListDefinitions().stream()
                .map(ResrcDefin::ResrcDefToMcpFormat)
                .collect(Collectors.toMap(
                        resource -> String.valueOf(resource.get("uri")),
                        resource -> resource,
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    private record McpResourceResolution(
            List<Map<String, Object>> resources,
            List<String> missingResourceUris) {

        private McpResourceResolution {
            resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
            missingResourceUris = List.copyOf(Objects.requireNonNull(missingResourceUris, "missingResourceUris"));
        }

        private int registeredResourceCount() {
            return resources.size();
        }
    }
}
