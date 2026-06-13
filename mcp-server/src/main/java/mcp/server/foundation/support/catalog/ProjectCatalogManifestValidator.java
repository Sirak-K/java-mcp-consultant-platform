package mcp.server.foundation.support.catalog;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.object;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.optionalText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredBoolean;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.textList;

@Service
public final class ProjectCatalogManifestValidator {

  private static final Path MANIFEST_PATH = Path.of("catalog_manifest.json");
  private static final String CATALOG_LABEL = "Project catalog manifest";
  private static final String EXPECTED_CATALOG_ID = "project_catalog_manifest";
  private static final String RESOURCE_URI_PREFIX = "resource://";
  private static final Set<String> OWNER_SURFACES = Set.of(
      "candidate_presentation",
      "cv_extraction",
      "match_notifications",
      "missions",
      "server",
      "system_operations");
  private static final Set<String> CATALOG_KINDS = Set.of(
      "candidate_presentation_generation_content_constraints",
      "candidate_presentation_generation_content_structure",
      "candidate_presentation_generation_evidence_traces",
      "candidate_presentation_generation_output_contract_schema",
      "candidate_presentation_generation_policy",
      "cv_extraction_content_blocks",
      "cv_extraction_content_constraints",
      "cv_extraction_text_detection",
      "email_template",
      "mcp_prompt_catalog",
      "mcp_resource_catalog",
      "mcp_tool_catalog",
      "server_capabilities_manifest",
      "runtime_triage_symptoms",
      "text_detection_semantics");

  public ProjectCatalogManifestValidator(ProjectCatalogJsonLoader catalogJsonLoader) {
    validate(Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader"));
  }

  static void validate(ProjectCatalogJsonLoader catalogJsonLoader) {
    JsonNode manifest = catalogJsonLoader.loadCatalogObject(MANIFEST_PATH);
    requireCatalogId(manifest, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(manifest, CATALOG_LABEL, "catalog_manifest_version");

    Set<Path> registeredPaths = new LinkedHashSet<>();
    Set<String> registeredIds = new LinkedHashSet<>();
    for (JsonNode entry : array(manifest, CATALOG_LABEL, "catalogs")) {
      validateEntry(catalogJsonLoader, entry, registeredPaths, registeredIds);
    }

    Set<Path> actualPaths = new LinkedHashSet<>(catalogJsonLoader.listCatalogJsonPaths());
    actualPaths.remove(MANIFEST_PATH);
    requireSamePathSet(actualPaths, registeredPaths);
  }

  private static void validateEntry(
      ProjectCatalogJsonLoader catalogJsonLoader,
      JsonNode entry,
      Set<Path> registeredPaths,
      Set<String> registeredIds) {

    String catalogId = requiredText(entry, CATALOG_LABEL, "catalog_id");
    Path catalogPath = requireCatalogPath(requiredText(entry, CATALOG_LABEL, "path"));
    String ownerSurface = requireKnownValue(
        requiredText(entry, CATALOG_LABEL, "owner_surface"),
        OWNER_SURFACES,
        "owner_surface");
    requireOwnerPathAlignment(catalogPath, ownerSurface);
    requiredText(entry, CATALOG_LABEL, "owner_package");
    requireKnownValue(
        requiredText(entry, CATALOG_LABEL, "catalog_kind"),
        CATALOG_KINDS,
        "catalog_kind");
    requireNonEmptyTextList(entry, "runtime_consumers");
    validateMcpExposure(object(entry, CATALOG_LABEL, "mcp_exposure"));

    if (!registeredPaths.add(catalogPath)) {
      throw new IllegalStateException("Duplicate catalog manifest path: " + displayPath(catalogPath));
    }
    if (!registeredIds.add(catalogId)) {
      throw new IllegalStateException("Duplicate catalog manifest catalog_id: " + catalogId);
    }

    JsonNode catalogRoot = catalogJsonLoader.loadCatalogObject(catalogPath);
    requireCatalogId(catalogRoot, CATALOG_LABEL + " entry " + displayPath(catalogPath), catalogId);
    for (String fieldName : requireNonEmptyTextList(entry, "required_root_fields")) {
      if (!catalogRoot.has(fieldName) || catalogRoot.get(fieldName).isNull()) {
        throw new IllegalStateException(
            "Catalog manifest required field is missing: " + displayPath(catalogPath) + " -> " + fieldName);
      }
    }
  }

  private static Path requireCatalogPath(String pathValue) {
    Path catalogPath = Path.of(pathValue);
    if (catalogPath.isAbsolute()) {
      throw new IllegalStateException("Catalog manifest path must be relative: " + pathValue);
    }
    Path normalized = catalogPath.normalize();
    if (!catalogPath.equals(normalized)) {
      throw new IllegalStateException("Catalog manifest path must be normalized: " + pathValue);
    }
    if (catalogPath.equals(MANIFEST_PATH)) {
      throw new IllegalStateException("Catalog manifest must not register itself.");
    }
    for (Path part : catalogPath) {
      String value = part.toString();
      if (value.isBlank() || ".".equals(value) || "..".equals(value)) {
        throw new IllegalStateException("Catalog manifest path contains an invalid segment: " + pathValue);
      }
    }
    if (!displayPath(catalogPath).endsWith(".json")) {
      throw new IllegalStateException("Catalog manifest path must point to a JSON file: " + pathValue);
    }
    return catalogPath;
  }

  private static void requireOwnerPathAlignment(Path catalogPath, String ownerSurface) {
    if (catalogPath.getNameCount() < 2 || !ownerSurface.equals(catalogPath.getName(0).toString())) {
      throw new IllegalStateException("Catalog manifest owner_surface must match first path segment: "
          + displayPath(catalogPath));
    }
  }

  private static String requireKnownValue(String value, Set<String> allowedValues, String fieldName) {
    if (!allowedValues.contains(value)) {
      throw new IllegalStateException("Catalog manifest field has unsupported value: " + fieldName + " -> " + value);
    }
    return value;
  }

  private static List<String> requireNonEmptyTextList(JsonNode node, String fieldName) {
    List<String> values = textList(node, CATALOG_LABEL, fieldName);
    if (values.isEmpty()) {
      throw new IllegalStateException("Catalog manifest field must not be empty: " + fieldName);
    }
    return values;
  }

  private static void validateMcpExposure(JsonNode exposure) {
    boolean exposed = requiredBoolean(exposure, CATALOG_LABEL, "exposed");
    List<String> resourceUris = textList(exposure, CATALOG_LABEL, "resource_uris");
    if (exposed && resourceUris.isEmpty()) {
      throw new IllegalStateException("Catalog manifest exposed MCP catalog must declare resource_uris.");
    }
    if (!exposed && !resourceUris.isEmpty()) {
      throw new IllegalStateException("Catalog manifest non-exposed catalog must not declare resource_uris.");
    }
    for (String resourceUri : resourceUris) {
      if (!resourceUri.startsWith(RESOURCE_URI_PREFIX)) {
        throw new IllegalStateException("Catalog manifest resource URI must start with "
            + RESOURCE_URI_PREFIX + ": " + resourceUri);
      }
    }
    String obsoleteSingleUri = optionalText(exposure, "resource_uri");
    if (!obsoleteSingleUri.isBlank()) {
      throw new IllegalStateException("Catalog manifest mcp_exposure must use resource_uris array.");
    }
  }

  private static void requireSamePathSet(Set<Path> actualPaths, Set<Path> registeredPaths) {
    Set<Path> missingRegistrations = new LinkedHashSet<>(actualPaths);
    missingRegistrations.removeAll(registeredPaths);
    if (!missingRegistrations.isEmpty()) {
      throw new IllegalStateException("Catalog files are missing from manifest: "
          + displayPaths(missingRegistrations));
    }

    Set<Path> missingFiles = new LinkedHashSet<>(registeredPaths);
    missingFiles.removeAll(actualPaths);
    if (!missingFiles.isEmpty()) {
      throw new IllegalStateException("Catalog manifest entries point to missing files: "
          + displayPaths(missingFiles));
    }
  }

  private static String displayPaths(Set<Path> paths) {
    return paths.stream()
        .map(ProjectCatalogManifestValidator::displayPath)
        .toList()
        .toString();
  }

  private static String displayPath(Path path) {
    return path.toString().replace('\\', '/');
  }
}
