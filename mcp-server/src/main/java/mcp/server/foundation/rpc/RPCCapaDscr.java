package mcp.server.foundation.rpc;

import mcp.server.foundation.prompt_interface.PromptDefin;
import mcp.server.foundation.prompt_interface.PromptReg;
import mcp.server.foundation.prompt_interface.PromptRenderResult;
import mcp.server.foundation.resource_interface.ResrcDefin;
import mcp.server.foundation.resource_interface.ResrcReg;
import mcp.server.foundation.tool_interface.ToolDefinition;
import mcp.server.foundation.tool_interface.ToolReg;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RPCCapaDscr
 *
 * Syfte:
 * - Centraliserar capability-identiteter som används i RPC-lagret.
 * - Definierar "system" som unik routing-nyckel för foundation-capabilities.
 */
public final class RPCCapaDscr {

        /**
         * Prefix = "system" måste vara unik routing-nyckel.
         */
        public static final String RPC_PREFIX_SYSTEM = "system";

        /**
         * MCP protocol version this server implements.
         */
        public static final String MCP_PROTOCOL_VERSION = "2025-11-25";

        private static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of(MCP_PROTOCOL_VERSION);

        public RPCCapaDscr() {
                // No-arg ctor krävs av Spring @Bean wiring.
        }

        // =========================================================
        // Capability payloads used by RPCMetResol
        // =========================================================

        public Map<String, Object> RPCCapaDescBuildInitializeResult(ResrcReg resourceReg) {
                return RPCCapaDescBuildInitializeResult(
                                resourceReg,
                                new PromptReg(),
                                MCP_PROTOCOL_VERSION);
        }

        public Map<String, Object> RPCCapaDescBuildInitializeResult(
                        ResrcReg resourceReg,
                        PromptReg promptReg) {

                return RPCCapaDescBuildInitializeResult(
                                resourceReg,
                                promptReg,
                                MCP_PROTOCOL_VERSION);
        }

        public Map<String, Object> RPCCapaDescBuildInitializeResult(
                        ResrcReg resourceReg,
                        PromptReg promptReg,
                        String negotiatedProtocolVersion) {

                String protocolVersion = RPCCapaDescRequireSupportedProtocolVersion(negotiatedProtocolVersion);

                LinkedHashMap<String, Object> capabilities = new LinkedHashMap<>();
                capabilities.put("tools", Map.of());

                if (resourceReg != null && resourceReg.ResrcRegSize() > 0) {
                        capabilities.put("resources", Map.of());
                }

                if (promptReg != null && promptReg.PromptRegSize() > 0) {
                        capabilities.put("prompts", Map.of());
                }

                return Map.of(
                                "protocolVersion", protocolVersion,
                                "capabilities", Map.copyOf(capabilities),
                                "serverInfo", Map.of(
                                                "name", "mcp-java-server",
                                                "version", "dev"));
        }

        public String RPCCapaDescResolveNegotiatedProtocolVersion(String requestedProtocolVersion) {

                String requested = normalizeProtocolVersion(requestedProtocolVersion);
                if (requested == null) {
                        throw RPCMappedExcep.RPCMappedExcepInvalidParams(
                                        RPCMetName.INITIALIZE + " requires params.protocolVersion");
                }

                if (SUPPORTED_PROTOCOL_VERSIONS.contains(requested)) {
                        return requested;
                }

                return MCP_PROTOCOL_VERSION;
        }

        public static boolean RPCCapaDescIsSupportedProtocolVersion(String protocolVersion) {

                String normalized = normalizeProtocolVersion(protocolVersion);
                return normalized != null && SUPPORTED_PROTOCOL_VERSIONS.contains(normalized);
        }

        public static List<String> RPCCapaDescSupportedProtocolVersions() {
                return SUPPORTED_PROTOCOL_VERSIONS;
        }

        private static String RPCCapaDescRequireSupportedProtocolVersion(String protocolVersion) {

                String normalized = normalizeProtocolVersion(protocolVersion);
                if (!RPCCapaDescIsSupportedProtocolVersion(normalized)) {
                        throw RPCMappedExcep.RPCMappedExcepInvalidParams(
                                        "Unsupported negotiated protocol version");
                }
                return normalized;
        }

        private static String normalizeProtocolVersion(String protocolVersion) {

                if (protocolVersion == null) {
                        return null;
                }

                String normalized = protocolVersion.trim();
                return normalized.isEmpty() ? null : normalized;
        }

        public Map<String, Boolean> RPCCapaDescBuildSessionsSubscribeResult() {
                // Minimal deterministisk ack (best-effort subscribe-semantik)
                return Map.of("subscribed", true);
        }

        public Map<String, Object> RPCCapaDescBuildToolsListResult(ToolReg toolRegistry) {

                Objects.requireNonNull(toolRegistry, "toolRegistry");

                List<Map<String, Object>> tools = toolRegistry.ToolRegListDefinitions()
                                .stream()
                                .map(ToolDefinition::ToolDefToMcpFormat)
                                .toList();

                return Map.of("tools", tools);
        }

        public Map<String, Object> RPCCapaDescBuildResourcesListResult(ResrcReg resourceReg) {

                Objects.requireNonNull(resourceReg, "resourceReg");

                List<Map<String, Object>> resources = resourceReg.ResrcRegListDefinitions()
                                .stream()
                                .map(ResrcDefin::ResrcDefToMcpFormat)
                                .toList();

                return Map.of("resources", resources);
        }

        public Map<String, Object> RPCCapaDescBuildResourceTemplatesListResult(ResrcReg resourceReg) {

                Objects.requireNonNull(resourceReg, "resourceReg");

                List<Map<String, Object>> resourceTemplates = resourceReg.ResrcRegListDefinitions()
                                .stream()
                                .filter(ResrcDefin::ResrcDefIsDynamic)
                                .map(ResrcDefin::ResrcDefToMcpTemplateFormat)
                                .toList();

                return Map.of("resourceTemplates", resourceTemplates);
        }

        public Map<String, Object> RPCCapaDescBuildPromptsListResult(PromptReg promptReg) {

                Objects.requireNonNull(promptReg, "promptReg");

                List<Map<String, Object>> prompts = promptReg.PromptRegListDefinitions()
                                .stream()
                                .map(PromptDefin::PromptDefToMcpFormat)
                                .toList();

                return Map.of("prompts", prompts);
        }

        public Map<String, Object> RPCCapaDescBuildPromptsGetResult(PromptRenderResult promptRenderResult) {

                Objects.requireNonNull(promptRenderResult, "promptRenderResult");
                return promptRenderResult.PromptRenderResToMcpFormat();
        }
}
