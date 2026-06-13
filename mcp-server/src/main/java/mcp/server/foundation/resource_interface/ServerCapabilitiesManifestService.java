package mcp.server.foundation.resource_interface;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import mcp.server.foundation.resource_interface.ServerCapabilityCatalog.CapabilityRiskClass;
import mcp.server.foundation.resource_interface.ServerCapabilityCatalog.ServerCapability;
import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.textList;

@Service
public final class ServerCapabilitiesManifestService {

  private static final Path SERVER_CAPABILITIES_MANIFEST_PATH = Path.of(
      "server",
      "server_capabilities_manifest.json");
  private static final String CATALOG_LABEL = "Server capability manifest";
  private static final String EXPECTED_CATALOG_ID = "server_capabilities_manifest";

  private final List<ServerCapability> capabilities;

  public ServerCapabilitiesManifestService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    this.capabilities = loadCapabilities(catalogJsonLoader);
  }

  public List<ServerCapability> listServerCapabilities() {
    return capabilities;
  }

  private static List<ServerCapability> loadCapabilities(ProjectCatalogJsonLoader catalogJsonLoader) {
    JsonNode root = catalogJsonLoader.loadCatalogObject(SERVER_CAPABILITIES_MANIFEST_PATH);
    requireCatalogId(root, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(root, CATALOG_LABEL, "server_capabilities_manifest_version");

    Set<String> capabilityIds = new LinkedHashSet<>();
    List<ServerCapability> loadedCapabilities = array(root, CATALOG_LABEL, "server_capabilities").stream()
        .map(ServerCapabilitiesManifestService::capabilityFrom)
        .toList();

    for (ServerCapability capability : loadedCapabilities) {
      if (!capabilityIds.add(capability.id())) {
        throw new IllegalStateException("Duplicate server capability id: " + capability.id());
      }
    }
    return List.copyOf(loadedCapabilities);
  }

  private static ServerCapability capabilityFrom(JsonNode node) {
    return new ServerCapability(
        requiredText(node, CATALOG_LABEL, "server_capability_id"),
        requiredText(node, CATALOG_LABEL, "server_capability_title"),
        textList(node, CATALOG_LABEL, "backend_endpoints"),
        textList(node, CATALOG_LABEL, "mcp_tools"),
        textList(node, CATALOG_LABEL, "mcp_resources"),
        textList(node, CATALOG_LABEL, "mcp_prompts"),
        textList(node, CATALOG_LABEL, "react_surfaces"),
        textList(node, CATALOG_LABEL, "persistence_anchors"),
        riskClass(requiredText(node, CATALOG_LABEL, "server_capability_risk_class")));
  }

  private static CapabilityRiskClass riskClass(String rawRiskClass) {
    try {
      return CapabilityRiskClass.valueOf(rawRiskClass);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Unknown server capability risk_class: " + rawRiskClass, exception);
    }
  }

}
