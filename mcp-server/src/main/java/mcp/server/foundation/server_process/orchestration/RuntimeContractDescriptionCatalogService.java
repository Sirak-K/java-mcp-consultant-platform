package mcp.server.foundation.server_process.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.putUnique;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredMapValue;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;

@Service
public final class RuntimeContractDescriptionCatalogService {

  private static final Path RUNTIME_CONTRACT_CATALOG_DIR = Path.of(
      "foundation",
      "runtime_contracts");
  private static final String CATALOG_LABEL = "Runtime contract catalog";
  private static final String OPERATING_SURFACE_DESCRIPTION_CATALOG =
      "operating_surface_description_catalog.json";
  private static final String REQUEST_LIFECYCLE_DESCRIPTION_CATALOG =
      "request_lifecycle_description_catalog.json";
  private static final String RUNTIME_SESSION_LIFECYCLE_DESCRIPTION_CATALOG =
      "runtime_session_lifecycle_description_catalog.json";

  private final Map<OperatingSurface, OperatingSurfaceDescription> operatingSurfaceDescriptions;
  private final Map<OperatingSurface, String> requestLifecycleSummaries;
  private final Map<RTMcpSessType, String> runtimeSessionLifecycleSummaries;

  public RuntimeContractDescriptionCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    this.operatingSurfaceDescriptions = loadOperatingSurfaceDescriptions(catalogJsonLoader);
    this.requestLifecycleSummaries = loadRequestLifecycleSummaries(catalogJsonLoader);
    this.runtimeSessionLifecycleSummaries = loadRuntimeSessionLifecycleSummaries(catalogJsonLoader);
  }

  public static RuntimeContractDescriptionCatalogService defaultCatalogService() {
    return new RuntimeContractDescriptionCatalogService(new ProjectCatalogJsonLoader(new ObjectMapper()));
  }

  public String operatingSurfaceDescription(OperatingSurface operatingSurface) {
    return operatingSurface(operatingSurface).description();
  }

  public String operatingSurfaceContractSummary(OperatingSurface operatingSurface) {
    return operatingSurface(operatingSurface).contractSummary();
  }

  public String requestLifecycleSummary(OperatingSurface operatingSurface) {
    return requiredSummary(
        requestLifecycleSummaries,
        Objects.requireNonNull(operatingSurface, "operatingSurface"),
        "request lifecycle summary");
  }

  public String runtimeSessionLifecycleSummary(RTMcpSessType sessionType) {
    return requiredSummary(
        runtimeSessionLifecycleSummaries,
        Objects.requireNonNull(sessionType, "sessionType"),
        "runtime session lifecycle summary");
  }

  private OperatingSurfaceDescription operatingSurface(OperatingSurface operatingSurface) {
    return requiredSummary(
        operatingSurfaceDescriptions,
        Objects.requireNonNull(operatingSurface, "operatingSurface"),
        "operating surface description");
  }

  private static Map<OperatingSurface, OperatingSurfaceDescription> loadOperatingSurfaceDescriptions(
      ProjectCatalogJsonLoader catalogJsonLoader) {

    JsonNode root = loadCatalog(
        catalogJsonLoader,
        OPERATING_SURFACE_DESCRIPTION_CATALOG,
        "operating_surface_description_catalog");
    EnumMap<OperatingSurface, OperatingSurfaceDescription> descriptions = new EnumMap<>(OperatingSurface.class);
    for (JsonNode node : array(root, CATALOG_LABEL, "operating_surface_descriptions")) {
      OperatingSurface surface = operatingSurface(node);
      putUnique(
          descriptions,
          surface,
          new OperatingSurfaceDescription(
              requiredText(node, CATALOG_LABEL, "surface_description"),
              requiredText(node, CATALOG_LABEL, "contract_summary")),
          CATALOG_LABEL,
          "operating surface description");
    }
    requireCoverage(descriptions, OperatingSurface.values(), "operating surface descriptions");
    return Map.copyOf(descriptions);
  }

  private static Map<OperatingSurface, String> loadRequestLifecycleSummaries(
      ProjectCatalogJsonLoader catalogJsonLoader) {

    JsonNode root = loadCatalog(
        catalogJsonLoader,
        REQUEST_LIFECYCLE_DESCRIPTION_CATALOG,
        "request_lifecycle_description_catalog");
    EnumMap<OperatingSurface, String> summaries = new EnumMap<>(OperatingSurface.class);
    for (JsonNode node : array(root, CATALOG_LABEL, "request_lifecycle_descriptions")) {
      putUnique(
          summaries,
          operatingSurface(node),
          requiredText(node, CATALOG_LABEL, "lifecycle_summary"),
          CATALOG_LABEL,
          "request lifecycle summary");
    }
    requireCoverage(summaries, OperatingSurface.values(), "request lifecycle summaries");
    return Map.copyOf(summaries);
  }

  private static Map<RTMcpSessType, String> loadRuntimeSessionLifecycleSummaries(
      ProjectCatalogJsonLoader catalogJsonLoader) {

    JsonNode root = loadCatalog(
        catalogJsonLoader,
        RUNTIME_SESSION_LIFECYCLE_DESCRIPTION_CATALOG,
        "runtime_session_lifecycle_description_catalog");
    EnumMap<RTMcpSessType, String> summaries = new EnumMap<>(RTMcpSessType.class);
    for (JsonNode node : array(root, CATALOG_LABEL, "runtime_session_lifecycle_descriptions")) {
      putUnique(
          summaries,
          runtimeSessionType(node),
          requiredText(node, CATALOG_LABEL, "lifecycle_summary"),
          CATALOG_LABEL,
          "runtime session lifecycle summary");
    }
    requireCoverage(summaries, RTMcpSessType.values(), "runtime session lifecycle summaries");
    return Map.copyOf(summaries);
  }

  private static JsonNode loadCatalog(
      ProjectCatalogJsonLoader catalogJsonLoader,
      String fileName,
      String expectedCatalogId) {

    JsonNode root = catalogJsonLoader.loadCatalogObject(RUNTIME_CONTRACT_CATALOG_DIR.resolve(fileName));
    requireCatalogId(root, CATALOG_LABEL, expectedCatalogId);
    requiredText(root, CATALOG_LABEL, "runtime_contract_catalog_version");
    return root;
  }

  private static OperatingSurface operatingSurface(JsonNode node) {
    return enumValue(
        OperatingSurface.class,
        requiredText(node, CATALOG_LABEL, "operating_surface"),
        "operating_surface");
  }

  private static RTMcpSessType runtimeSessionType(JsonNode node) {
    return enumValue(
        RTMcpSessType.class,
        requiredText(node, CATALOG_LABEL, "session_type"),
        "session_type");
  }

  private static <T extends Enum<T>> T enumValue(
      Class<T> enumType,
      String rawValue,
      String fieldName) {
    try {
      return Enum.valueOf(enumType, rawValue);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Unknown runtime contract catalog " + fieldName + ": " + rawValue, exception);
    }
  }

  private static <K, V> V requiredSummary(Map<K, V> values, K key, String label) {
    return requiredMapValue(values, key, CATALOG_LABEL, label);
  }

  private static <K, V> void requireCoverage(Map<K, V> values, K[] expectedValues, String label) {
    for (K expectedValue : expectedValues) {
      if (!values.containsKey(expectedValue)) {
        throw new IllegalStateException("Runtime contract catalog is missing " + label + ": " + expectedValue);
      }
    }
  }

  private record OperatingSurfaceDescription(
      String description,
      String contractSummary) {
  }
}
