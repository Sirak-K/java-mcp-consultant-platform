package mcp.server.foundation.support.catalog;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.putUnique;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredArrayText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;

@Service
public final class ProjectCatalogManifestService {

  static final Path MANIFEST_PATH = Path.of("catalog_manifest.json");

  private static final String CATALOG_LABEL = "project catalog manifest";
  private static final String CATALOG_ID = "project_catalog_manifest";

  private final ProjectCatalogJsonLoader catalogJsonLoader;

  public ProjectCatalogManifestService(ProjectCatalogJsonLoader catalogJsonLoader) {
    this.catalogJsonLoader = Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
  }

  public ValidationReport validateManifest() {
    JsonNode manifest = catalogJsonLoader.loadCatalogObject(MANIFEST_PATH);
    requireCatalogId(manifest, CATALOG_LABEL, CATALOG_ID);
    requiredText(manifest, CATALOG_LABEL, "catalog_manifest_version");
    Map<String, CatalogEntry> entriesById = new LinkedHashMap<>();
    Map<String, CatalogEntry> entriesByPath = new LinkedHashMap<>();
    for (JsonNode entryNode : array(manifest, CATALOG_LABEL, "catalogs")) {
      CatalogEntry entry = catalogEntryFrom(entryNode);
      putUnique(entriesById, entry.catalogId(), entry, CATALOG_LABEL, "catalog_id");
      putUnique(entriesByPath, entry.pathDisplay(), entry, CATALOG_LABEL, "path");
      validateCatalogFile(entry);
    }

    validateCompleteCatalogCoverage(entriesByPath.keySet());
    return new ValidationReport(entriesById.size());
  }

  private CatalogEntry catalogEntryFrom(JsonNode entryNode) {
    String catalogId = requiredText(entryNode, CATALOG_LABEL, "catalog_id");
    Path path = requiredRelativeJsonPath(requiredText(entryNode, CATALOG_LABEL, "path"));
    requiredText(entryNode, CATALOG_LABEL, "owner_surface");
    requiredText(entryNode, CATALOG_LABEL, "owner_package");
    requiredText(entryNode, CATALOG_LABEL, "catalog_kind");
    requiredText(entryNode, CATALOG_LABEL, "responsibility");
    List<String> requiredRootFields = array(entryNode, CATALOG_LABEL, "required_root_fields").stream()
        .map(value -> requiredArrayText(value, CATALOG_LABEL, "required_root_fields"))
        .toList();
    if (!requiredRootFields.contains("catalog_id")) {
      throw new IllegalStateException("project catalog manifest required_root_fields must include catalog_id");
    }

    List<String> runtimeConsumers = array(entryNode, CATALOG_LABEL, "runtime_consumers").stream()
        .map(value -> requiredArrayText(value, CATALOG_LABEL, "runtime_consumers"))
        .toList();
    if (runtimeConsumers.isEmpty()) {
      throw new IllegalStateException("project catalog manifest field must not be empty: runtime_consumers");
    }

    return new CatalogEntry(catalogId, path, displayPath(path), requiredRootFields);
  }

  private void validateCatalogFile(CatalogEntry entry) {
    JsonNode catalog = catalogJsonLoader.loadCatalogObject(entry.path());
    requireCatalogId(catalog, "catalog " + entry.catalogId(), entry.catalogId());
    for (String requiredRootField : entry.requiredRootFields()) {
      requireRootField(catalog, entry.catalogId(), requiredRootField);
    }
  }

  private void requireRootField(JsonNode catalog, String catalogId, String requiredRootField) {
    JsonNode value = catalog.get(requiredRootField);
    if (value == null || value.isNull()) {
      throw new IllegalStateException("catalog " + catalogId + " field is required: " + requiredRootField);
    }
  }

  private void validateCompleteCatalogCoverage(Set<String> manifestPaths) {
    Set<String> actualPaths = catalogJsonLoader.listCatalogJsonPaths().stream()
        .map(ProjectCatalogManifestService::displayPath)
        .filter(path -> !displayPath(MANIFEST_PATH).equals(path))
        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

    List<String> missingFromManifest = actualPaths.stream()
        .filter(path -> !manifestPaths.contains(path))
        .toList();
    if (!missingFromManifest.isEmpty()) {
      throw new IllegalStateException(
          "Catalog files are missing from project catalog manifest: " + String.join(", ", missingFromManifest));
    }

    List<String> missingFiles = manifestPaths.stream()
        .filter(path -> !actualPaths.contains(path))
        .toList();
    if (!missingFiles.isEmpty()) {
      throw new IllegalStateException(
          "Project catalog manifest references missing catalog files: " + String.join(", ", missingFiles));
    }
  }

  private static Path requiredRelativeJsonPath(String rawPath) {
    Path path = Path.of(rawPath);
    if (path.isAbsolute()) {
      throw new IllegalStateException("project catalog manifest path must be relative: " + rawPath);
    }

    Path normalized = path.normalize();
    if (!path.equals(normalized)) {
      throw new IllegalStateException("project catalog manifest path must be normalized: " + rawPath);
    }

    for (Path part : path) {
      String value = part.toString();
      if (value.isBlank() || ".".equals(value) || "..".equals(value)) {
        throw new IllegalStateException("project catalog manifest path contains an invalid segment: " + rawPath);
      }
    }

    if (!displayPath(path).endsWith(".json")) {
      throw new IllegalStateException("project catalog manifest path must point to a JSON file: " + rawPath);
    }

    return path;
  }

  private static String displayPath(Path path) {
    return path.toString().replace('\\', '/');
  }

  public record ValidationReport(int catalogCount) {
  }

  private record CatalogEntry(
      String catalogId,
      Path path,
      String pathDisplay,
      List<String> requiredRootFields) {
  }
}
