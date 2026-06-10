package mcp.server.foundation.resource_interface;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredBoolean;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredValue;

@Service
public final class McpResourceManifestCatalogService {

  private static final Path MCP_RESOURCE_MANIFEST_CATALOG_PATH = Path.of(
      "foundation",
      "mcp_resources",
      "mcp_resource_manifest_catalog.json");
  private static final String CATALOG_LABEL = "MCP resource catalog";
  private static final String EXPECTED_CATALOG_ID = "mcp_resource_manifest_catalog";
  private static final String RESOURCE_URI_PREFIX = "resource://";

  private final Map<String, ResrcManifestEntry> resourcesByKey;

  public McpResourceManifestCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    JsonNode root = catalogJsonLoader.loadCatalogObject(MCP_RESOURCE_MANIFEST_CATALOG_PATH);
    requireCatalogId(root, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(root, CATALOG_LABEL, "mcp_resource_manifest_catalog_version");
    this.resourcesByKey = loadResources(root);
  }

  public ResrcDefin resourceDefinition(
      String resourceKey,
      ResrcProvid provider) {

    ResrcManifestEntry entry = resource(resourceKey);
    return new ResrcDefin(
        entry.resourceUri(),
        entry.resourceName(),
        entry.resourceDescription(),
        ResrcDefin.MIME_TYPE_JSON,
        entry.dynamic(),
        provider);
  }

  public String resourceUri(String resourceKey) {
    return resource(resourceKey).resourceUri();
  }

  public String resourceName(String resourceKey) {
    return resource(resourceKey).resourceName();
  }

  private ResrcManifestEntry resource(String resourceKey) {
    String normalizedKey = requiredValue(resourceKey, CATALOG_LABEL, "resourceKey");
    ResrcManifestEntry entry = resourcesByKey.get(normalizedKey);
    if (entry == null) {
      throw new IllegalStateException("MCP resource catalog entry is missing: " + normalizedKey);
    }
    return entry;
  }

  private static Map<String, ResrcManifestEntry> loadResources(JsonNode root) {
    LinkedHashMap<String, ResrcManifestEntry> resources = new LinkedHashMap<>();
    Set<String> resourceUris = new LinkedHashSet<>();
    Set<String> resourceNames = new LinkedHashSet<>();
    for (JsonNode resourceNode : array(root, CATALOG_LABEL, "mcp_resources")) {
      ResrcManifestEntry entry = new ResrcManifestEntry(
          requiredText(resourceNode, CATALOG_LABEL, "resource_key"),
          requireResourceUri(resourceNode, "resource_uri"),
          requiredText(resourceNode, CATALOG_LABEL, "resource_name"),
          requiredText(resourceNode, CATALOG_LABEL, "resource_description"),
          requiredBoolean(resourceNode, CATALOG_LABEL, "resource_dynamic"),
          requiredText(resourceNode, CATALOG_LABEL, "resource_group"));

      ResrcManifestEntry previous = resources.putIfAbsent(entry.resourceKey(), entry);
      if (previous != null) {
        throw new IllegalStateException("Duplicate MCP resource catalog key: " + entry.resourceKey());
      }
      if (!resourceUris.add(entry.resourceUri())) {
        throw new IllegalStateException("Duplicate MCP resource catalog URI: " + entry.resourceUri());
      }
      if (!resourceNames.add(entry.resourceName())) {
        throw new IllegalStateException("Duplicate MCP resource catalog name: " + entry.resourceName());
      }
    }
    if (resources.isEmpty()) {
      throw new IllegalStateException("MCP resource catalog must contain at least one resource.");
    }
    return java.util.Collections.unmodifiableMap(resources);
  }

  private static String requireResourceUri(JsonNode node, String fieldName) {
    String resourceUri = requiredText(node, CATALOG_LABEL, fieldName);
    if (!resourceUri.startsWith(RESOURCE_URI_PREFIX)) {
      throw new IllegalStateException("MCP resource catalog URI must start with "
          + RESOURCE_URI_PREFIX + ": " + resourceUri);
    }
    return resourceUri;
  }

  private record ResrcManifestEntry(
      String resourceKey,
      String resourceUri,
      String resourceName,
      String resourceDescription,
      boolean dynamic,
      String resourceGroup) {
  }
}
