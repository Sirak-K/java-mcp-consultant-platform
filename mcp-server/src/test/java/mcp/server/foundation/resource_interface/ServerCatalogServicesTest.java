package mcp.server.foundation.resource_interface;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ServerCatalogServicesTest {

  private final ProjectCatalogJsonLoader catalogJsonLoader = new ProjectCatalogJsonLoader(new ObjectMapper());

  @Test
  void loadsServerCapabilitiesManifestAndMcpPrimitiveCatalogs() {
    ServerCapabilitiesManifestService capabilities = new ServerCapabilitiesManifestService(catalogJsonLoader);
    McpToolCatalogService tools = new McpToolCatalogService(catalogJsonLoader);
    McpPromptCatalogService prompts = new McpPromptCatalogService(catalogJsonLoader);
    McpResourceCatalogService resources = new McpResourceCatalogService(catalogJsonLoader);

    assertThat(capabilities.listServerCapabilities())
        .extracting(ServerCapabilityCatalog.ServerCapability::id)
        .contains("candidatePresentation", "matchNotification", "missionProposal");

    tools.validateToolReferences(List.of(
        "ops.healthcheck",
        "candidatePresentation.collectEvidence",
        "candidatePresentation.getGenerationContract",
        "candidatePresentation.recordGeneratedContent",
        "candidatePresentation.recordGenerationFailure",
        "matchNotifications.sendEmail",
        "missionProposal.previewFromText"));
    prompts.validatePromptReferences(List.of("candidatePresentation.generateDraftPrompt"));

    assertThat(resources.resourceUri("server_capabilities")).isEqualTo("resource://server/capabilities");
    assertThat(resources.resourceName("tools_catalog")).isEqualTo("tools/catalog");
  }
}
