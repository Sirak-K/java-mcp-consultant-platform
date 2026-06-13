package mcp.server.foundation.spring_integration;

import jakarta.annotation.PostConstruct;
import mcp.server.domain.candidate_presentation.resource.CandidatePresentationGenerationContractResourceProvider;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationContractService;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.RoleJpaRepo;
import mcp.server.domain.reference_data.resource.RoleCatalogResourceProvider;
import mcp.server.domain.reference_data.resource.SkillLevelCatalogResourceProvider;
import mcp.server.domain.reference_data.resource.SkillCatalogResourceProvider;
import mcp.server.foundation.audit.AuditEntryJpaRepo;
import mcp.server.foundation.control_plane.PlatformControlPlaneStore;
import mcp.server.foundation.observability.runtime.RTVisibilityService;
import mcp.server.foundation.prompt_interface.PromptReg;
import mcp.server.foundation.resource_interface.ServerCapabilitiesManifestResourceProvider;
import mcp.server.foundation.resource_interface.ServerCapabilitiesManifestService;
import mcp.server.foundation.resource_interface.McpPromptCatalogService;
import mcp.server.foundation.resource_interface.RecentAuditResrcProvid;
import mcp.server.foundation.resource_interface.McpResourceCatalogService;
import mcp.server.foundation.resource_interface.McpToolCatalogService;
import mcp.server.foundation.resource_interface.ResrcDefin;
import mcp.server.foundation.resource_interface.ResrcProvid;
import mcp.server.foundation.resource_interface.ResrcReg;
import mcp.server.foundation.resource_interface.RTOverviewResrcProvid;
import mcp.server.foundation.resource_interface.ToolCatalogResourceProvider;
import mcp.server.foundation.rpc.RPCCapaDscr;
import mcp.server.foundation.tool_interface.ToolReg;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Objects;

/**
 * MCP resource surface wiring.
 *
 * <p>
 * Discovery metadata is loaded from the MCP resource catalog, while this
 * configuration owns provider binding and dependency wiring.
 */
@Configuration
public class SpringMcpResrcCfg {

        private static final String RESOURCE_KEY_TOOLS_CATALOG = "tools_catalog";
        private static final String RESOURCE_KEY_SERVER_CAPABILITIES = "server_capabilities";
        private static final String RESOURCE_KEY_OPS_RUNTIME_OVERVIEW = "ops_runtime_overview";
        private static final String RESOURCE_KEY_CONSULTANT_ROLES = "consultant_roles";
        private static final String RESOURCE_KEY_SKILL_LEVELS = "skill_levels";
        private static final String RESOURCE_KEY_TECHNICAL_SKILLS = "technical_skills";
        private static final String RESOURCE_KEY_RECENT_AUDIT_ENTRIES = "recent_audit_entries";
        private static final String RESOURCE_KEY_CANDIDATE_PRESENTATION_GENERATION_CONTRACT = "candidate_presentation_generation_contract";

        private final ResrcReg resrcReg;
        private final McpResourceCatalogService resourceCatalogService;
        private final ToolReg toolReg;
        private final PromptReg promptReg;
        private final McpToolCatalogService toolCatalogService;
        private final McpPromptCatalogService promptCatalogService;
        private final RTVisibilityService runtimeVisibilityService;
        private final PlatformControlPlaneStore platformControlPlaneStore;
        private final CustomerDataModeRTPolicy customerDataModeRTPolicy;
        private final AuditEntryJpaRepo auditEntryJpaRepo;
        private final RoleJpaRepo roleJpaRepo;
        private final SkillCatalogLookup skillCatalogLookup;
        private final CompetencyLevelLookupJpaRepo competencyLevelLookupJpaRepo;
        private final ServerCapabilitiesManifestService serverCapabilitiesManifestService;
        private final CandidatePresentationGenerationContractService candidatePresentationGenerationContractService;
        private final String applicationName;
        private final String applicationVersion;

        public SpringMcpResrcCfg(
                        ResrcReg resrcReg,
                        McpResourceCatalogService resourceCatalogService,
                        ToolReg toolReg,
                        PromptReg promptReg,
                        McpToolCatalogService toolCatalogService,
                        McpPromptCatalogService promptCatalogService,
                        RTVisibilityService runtimeVisibilityService,
                        PlatformControlPlaneStore platformControlPlaneStore,
                        CustomerDataModeRTPolicy customerDataModeRTPolicy,
                        AuditEntryJpaRepo auditEntryJpaRepo,
                        RoleJpaRepo roleJpaRepo,
                        SkillCatalogLookup skillCatalogLookup,
                        CompetencyLevelLookupJpaRepo competencyLevelLookupJpaRepo,
                        ServerCapabilitiesManifestService serverCapabilitiesManifestService,
                        CandidatePresentationGenerationContractService candidatePresentationGenerationContractService,
                        @Value("${spring.application.name:mcp-server}") String applicationName,
                        @Value("${mcp.server.version:dev}") String applicationVersion) {

                this.resrcReg = Objects.requireNonNull(resrcReg, "resrcReg");
                this.resourceCatalogService = Objects.requireNonNull(
                                resourceCatalogService,
                                "resourceCatalogService");
                this.toolReg = Objects.requireNonNull(toolReg, "toolReg");
                this.promptReg = Objects.requireNonNull(promptReg, "promptReg");
                this.toolCatalogService = Objects.requireNonNull(toolCatalogService, "toolCatalogService");
                this.promptCatalogService = Objects.requireNonNull(promptCatalogService, "promptCatalogService");
                this.runtimeVisibilityService = Objects.requireNonNull(runtimeVisibilityService,
                                "runtimeVisibilityService");
                this.platformControlPlaneStore = Objects.requireNonNull(platformControlPlaneStore,
                                "platformControlPlaneStore");
                this.customerDataModeRTPolicy = Objects.requireNonNull(customerDataModeRTPolicy,
                                "customerDataModeRTPolicy");
                this.auditEntryJpaRepo = Objects.requireNonNull(auditEntryJpaRepo, "auditEntryJpaRepo");
                this.roleJpaRepo = Objects.requireNonNull(roleJpaRepo, "roleJpaRepo");
                this.skillCatalogLookup = Objects.requireNonNull(skillCatalogLookup, "skillCatalogLookup");
                this.competencyLevelLookupJpaRepo = Objects.requireNonNull(competencyLevelLookupJpaRepo,
                                "competencyLevelLookupJpaRepo");
                this.serverCapabilitiesManifestService = Objects.requireNonNull(
                                serverCapabilitiesManifestService,
                                "serverCapabilitiesManifestService");
                this.candidatePresentationGenerationContractService = Objects.requireNonNull(
                                candidatePresentationGenerationContractService,
                                "candidatePresentationGenerationContractService");
                this.applicationName = Objects.requireNonNull(applicationName, "applicationName");
                this.applicationVersion = Objects.requireNonNull(applicationVersion, "applicationVersion");
        }

        @PostConstruct
        public void registerResources() {
                Map<String, Object> productIdentity = Map.of(
                                "name", applicationName,
                                "version", applicationVersion,
                                "domain", "staffing-platform");

                Map<String, Object> runtimeContract = Map.of(
                                "protocolVersion", RPCCapaDscr.MCP_PROTOCOL_VERSION,
                                "persistenceMode", customerDataModeRTPolicy.CustomerDataModeRTPolicyActiveMode(),
                                "transport", "streamable-http");

                registerResources(
                                resourceDefinition(
                                                RESOURCE_KEY_TOOLS_CATALOG,
                                                new ToolCatalogResourceProvider(
                                                                resourceUri(RESOURCE_KEY_TOOLS_CATALOG),
                                                                resourceName(RESOURCE_KEY_TOOLS_CATALOG),
                                                                toolReg)),
                                resourceDefinition(
                                                RESOURCE_KEY_SERVER_CAPABILITIES,
                                                new ServerCapabilitiesManifestResourceProvider(
                                                                resourceUri(RESOURCE_KEY_SERVER_CAPABILITIES),
                                                                resourceName(RESOURCE_KEY_SERVER_CAPABILITIES),
                                                                resrcReg,
                                                                toolReg,
                                                                promptReg,
                                                                toolCatalogService,
                                                                promptCatalogService,
                                                                serverCapabilitiesManifestService)),
                                resourceDefinition(
                                                RESOURCE_KEY_OPS_RUNTIME_OVERVIEW,
                                                new RTOverviewResrcProvid(
                                                                resourceName(RESOURCE_KEY_OPS_RUNTIME_OVERVIEW),
                                                                runtimeVisibilityService,
                                                                platformControlPlaneStore,
                                                                productIdentity, runtimeContract)),
                                resourceDefinition(
                                                RESOURCE_KEY_CONSULTANT_ROLES,
                                                new RoleCatalogResourceProvider(
                                                                resourceUri(RESOURCE_KEY_CONSULTANT_ROLES),
                                                                resourceName(RESOURCE_KEY_CONSULTANT_ROLES),
                                                                roleJpaRepo)),
                                resourceDefinition(
                                                RESOURCE_KEY_SKILL_LEVELS,
                                                new SkillLevelCatalogResourceProvider(
                                                                resourceUri(RESOURCE_KEY_SKILL_LEVELS),
                                                                resourceName(RESOURCE_KEY_SKILL_LEVELS),
                                                                competencyLevelLookupJpaRepo)),
                                resourceDefinition(
                                                RESOURCE_KEY_TECHNICAL_SKILLS,
                                                new SkillCatalogResourceProvider(
                                                                resourceUri(RESOURCE_KEY_TECHNICAL_SKILLS),
                                                                resourceName(RESOURCE_KEY_TECHNICAL_SKILLS),
                                                                skillCatalogLookup)),
                                resourceDefinition(
                                                RESOURCE_KEY_RECENT_AUDIT_ENTRIES,
                                                new RecentAuditResrcProvid(
                                                                resourceName(RESOURCE_KEY_RECENT_AUDIT_ENTRIES),
                                                                auditEntryJpaRepo)),
                                resourceDefinition(
                                                RESOURCE_KEY_CANDIDATE_PRESENTATION_GENERATION_CONTRACT,
                                                new CandidatePresentationGenerationContractResourceProvider(
                                                                resourceUri(
                                                                                RESOURCE_KEY_CANDIDATE_PRESENTATION_GENERATION_CONTRACT),
                                                                resourceName(
                                                                                RESOURCE_KEY_CANDIDATE_PRESENTATION_GENERATION_CONTRACT),
                                                                candidatePresentationGenerationContractService)));
        }

        private void registerResources(ResrcDefin... resources) {
                if (resources == null || resources.length == 0) {
                        throw new IllegalStateException("MCP resource registration has no resources");
                }
                for (ResrcDefin resource : resources) {
                        registerResource(resource);
                }
        }

        private ResrcDefin resourceDefinition(
                        String resourceKey,
                        ResrcProvid provider) {

                return resourceCatalogService.resourceDefinition(
                                resourceKey,
                                provider);
        }

        private String resourceUri(String resourceKey) {
                return resourceCatalogService.resourceUri(resourceKey);
        }

        private String resourceName(String resourceKey) {
                return resourceCatalogService.resourceName(resourceKey);
        }

        private void registerResource(ResrcDefin resource) {
                ResrcDefin requiredResource = Objects.requireNonNull(resource, "resource");
                resrcReg.ResrcRegRegister(requiredResource);
        }
}
