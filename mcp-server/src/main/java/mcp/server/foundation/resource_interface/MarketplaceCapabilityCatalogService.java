package mcp.server.foundation.resource_interface;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import mcp.server.foundation.resource_interface.MarketplaceCapabilityCatalog.CapabilityRiskClass;
import mcp.server.foundation.resource_interface.MarketplaceCapabilityCatalog.MarketplaceCapability;
import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.textList;

@Service
public final class MarketplaceCapabilityCatalogService {

  private static final Path MARKETPLACE_CAPABILITY_CATALOG_PATH = Path.of(
      "foundation",
      "marketplace_capabilities",
      "marketplace_capability_contract_catalog.json");
  private static final String CATALOG_LABEL = "Marketplace capability catalog";
  private static final String EXPECTED_CATALOG_ID = "marketplace_capability_contract_catalog";

  private final List<MarketplaceCapability> capabilities;

  public MarketplaceCapabilityCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    this.capabilities = loadCapabilities(catalogJsonLoader);
  }

  public List<MarketplaceCapability> listCapabilities() {
    return capabilities;
  }

  private static List<MarketplaceCapability> loadCapabilities(ProjectCatalogJsonLoader catalogJsonLoader) {
    JsonNode root = catalogJsonLoader.loadCatalogObject(MARKETPLACE_CAPABILITY_CATALOG_PATH);
    requireCatalogId(root, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(root, CATALOG_LABEL, "marketplace_capability_catalog_version");

    Set<String> capabilityIds = new LinkedHashSet<>();
    List<MarketplaceCapability> loadedCapabilities = array(root, CATALOG_LABEL, "marketplace_capabilities").stream()
        .map(MarketplaceCapabilityCatalogService::capabilityFrom)
        .toList();

    for (MarketplaceCapability capability : loadedCapabilities) {
      if (!capabilityIds.add(capability.id())) {
        throw new IllegalStateException("Duplicate marketplace capability id: " + capability.id());
      }
    }
    return List.copyOf(loadedCapabilities);
  }

  private static MarketplaceCapability capabilityFrom(JsonNode node) {
    return new MarketplaceCapability(
        requiredText(node, CATALOG_LABEL, "capability_id"),
        requiredText(node, CATALOG_LABEL, "capability_title"),
        textList(node, CATALOG_LABEL, "backend_endpoints"),
        textList(node, CATALOG_LABEL, "mcp_tools"),
        textList(node, CATALOG_LABEL, "mcp_resources"),
        textList(node, CATALOG_LABEL, "react_surfaces"),
        textList(node, CATALOG_LABEL, "persistence_anchors"),
        riskClass(requiredText(node, CATALOG_LABEL, "risk_class")));
  }

  private static CapabilityRiskClass riskClass(String rawRiskClass) {
    try {
      return CapabilityRiskClass.valueOf(rawRiskClass);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Unknown marketplace capability risk_class: " + rawRiskClass, exception);
    }
  }

}
