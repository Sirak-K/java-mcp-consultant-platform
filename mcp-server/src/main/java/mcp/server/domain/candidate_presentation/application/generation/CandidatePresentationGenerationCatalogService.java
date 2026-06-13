package mcp.server.domain.candidate_presentation.application.generation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.object;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.optionalText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireUniqueTextValues;

@Service
public final class CandidatePresentationGenerationCatalogService {

  private static final Path CANDIDATE_PRESENTATION_GENERATION_CATALOG_DIR = Path.of(
      "candidate_presentation",
      "generation");
  private static final String GENERATION_CONTENT_STRUCTURE_CATALOG = "generation_content_structure.json";
  private static final String GENERATION_CONTENT_CONSTRAINTS_CATALOG = "generation_content_constraints.json";
  private static final String GENERATION_POLICY_CATALOG = "generation_policy.json";
  private static final String GENERATION_EVIDENCE_TRACES_CATALOG = "generation_evidence_traces.json";
  private static final String GENERATION_OUTPUT_CONTRACT_SCHEMA_CATALOG = "generation_output_contract.schema.json";
  private static final String GENERATION_CONTENT_STRUCTURE_CATALOG_ID = "generation_content_structure";
  private static final String GENERATION_CONTENT_CONSTRAINTS_CATALOG_ID = "generation_content_constraints";
  private static final String GENERATION_POLICY_CATALOG_ID = "generation_policy";
  private static final String GENERATION_EVIDENCE_TRACES_CATALOG_ID = "generation_evidence_traces";
  private static final String GENERATION_OUTPUT_CONTRACT_SCHEMA_CATALOG_ID = "generation_output_contract_schema";
  private static final String CATALOG_LABEL = "Candidate Presentation generation catalog";

  private final ObjectMapper objectMapper;
  private final ProjectCatalogJsonLoader catalogJsonLoader;
  private final JsonNode generationContentStructureCatalog;
  private final JsonNode generationContentConstraintsCatalog;
  private final JsonNode generationPolicyCatalog;
  private final JsonNode generationEvidenceTracesCatalog;
  private final JsonNode generationOutputContractSchemaCatalog;

  public CandidatePresentationGenerationCatalogService(
      ObjectMapper objectMapper,
      ProjectCatalogJsonLoader catalogJsonLoader) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.catalogJsonLoader = Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    this.generationContentStructureCatalog = readCatalog(
        GENERATION_CONTENT_STRUCTURE_CATALOG,
        GENERATION_CONTENT_STRUCTURE_CATALOG_ID,
        "generation_content_structure_version");
    this.generationContentConstraintsCatalog = readCatalog(
        GENERATION_CONTENT_CONSTRAINTS_CATALOG,
        GENERATION_CONTENT_CONSTRAINTS_CATALOG_ID,
        "generation_content_constraints_version");
    this.generationPolicyCatalog = readCatalog(
        GENERATION_POLICY_CATALOG,
        GENERATION_POLICY_CATALOG_ID,
        "generation_policy_version");
    this.generationEvidenceTracesCatalog = readCatalog(
        GENERATION_EVIDENCE_TRACES_CATALOG,
        GENERATION_EVIDENCE_TRACES_CATALOG_ID,
        "generation_evidence_traces_version");
    this.generationOutputContractSchemaCatalog = readCatalog(
        GENERATION_OUTPUT_CONTRACT_SCHEMA_CATALOG,
        GENERATION_OUTPUT_CONTRACT_SCHEMA_CATALOG_ID,
        "generation_output_contract_schema_version");
  }

  public Map<String, Object> generationContentStructurePayload() {
    return objectMap(generationContentStructureCatalog);
  }

  public Map<String, Object> generationContentConstraintsPayload() {
    return objectMap(generationContentConstraintsCatalog);
  }

  public Map<String, Object> generationPolicyPayload() {
    return objectMap(generationPolicyCatalog);
  }

  public Map<String, Object> generationEvidenceTracesPayload() {
    return objectMap(generationEvidenceTracesCatalog);
  }

  public Map<String, Object> generationOutputContractSchemaPayload() {
    return objectMap(generationOutputContractSchemaCatalog);
  }

  public Map<String, Object> generationOutputJsonSchemaPayload() {
    return objectMap(object(generationOutputContractSchemaCatalog, CATALOG_LABEL, "json_schema"));
  }

  public List<CandidatePresentationSectionSpec> customerFacingSectionSpecs() {
    return sectionSpecs("customer_facing_sections");
  }

  public List<CandidatePresentationSectionSpec> opsReviewSectionSpecs() {
    return sectionSpecs("ops_review_sections");
  }

  public Map<String, String> customerFacingDraftContent() {
    return draftContent(customerFacingSectionSpecs());
  }

  public Map<String, String> opsReviewDraftContent() {
    return draftContent(opsReviewSectionSpecs());
  }

  public List<String> customerFacingRequiredSections() {
    return customerFacingSectionSpecs().stream()
        .filter(CandidatePresentationSectionSpec::required)
        .map(CandidatePresentationSectionSpec::key)
        .toList();
  }

  public List<String> customerFacingDisallowedRuntimeTerms() {
    return stringList("customer_facing_disallowed_runtime_terms");
  }

  public List<String> customerFacingDisallowedTextFragments() {
    return stringList("customer_facing_disallowed_text_fragments");
  }

  public List<String> customerFacingDisallowedUnicodeScripts() {
    return stringList("customer_facing_disallowed_unicode_scripts");
  }

  public List<String> opsReviewRequiredSections() {
    return opsReviewSectionSpecs().stream()
        .filter(CandidatePresentationSectionSpec::required)
        .map(CandidatePresentationSectionSpec::key)
        .toList();
  }

  public List<String> evidenceTraceRequiredFields() {
    return evidenceTraceFieldSpecs().stream()
        .map(CandidatePresentationEvidenceTraceFieldSpec::key)
        .toList();
  }

  public List<CandidatePresentationEvidenceTraceFieldSpec> evidenceTraceFieldSpecs() {
    List<CandidatePresentationEvidenceTraceFieldSpec> fieldSpecs = array(
        generationEvidenceTracesCatalog,
        CATALOG_LABEL,
        "evidence_trace_fields").stream()
        .map(field -> new CandidatePresentationEvidenceTraceFieldSpec(
            requiredText(field, CATALOG_LABEL, "field_key"),
            requiredText(field, CATALOG_LABEL, "field_title"),
            requiredText(field, CATALOG_LABEL, "field_description")))
        .toList();
    requireUniqueTextValues(
        fieldSpecs.stream().map(CandidatePresentationEvidenceTraceFieldSpec::key).toList(),
        CATALOG_LABEL,
        "evidence_trace_fields.field_key");
    return fieldSpecs;
  }

  public List<String> generationOutputRequiredFields() {
    return contractTopLevelFields().stream()
        .filter(CandidatePresentationContractFieldSpec::required)
        .map(CandidatePresentationContractFieldSpec::key)
        .toList();
  }

  public List<CandidatePresentationContractFieldSpec> contractTopLevelFields() {
    List<CandidatePresentationContractFieldSpec> fieldSpecs = array(
        generationOutputContractSchemaCatalog,
        CATALOG_LABEL,
        "top_level_fields").stream()
        .map(field -> new CandidatePresentationContractFieldSpec(
            requiredText(field, CATALOG_LABEL, "contract_field_key"),
            requiredText(field, CATALOG_LABEL, "contract_field_description"),
            requiredText(field, CATALOG_LABEL, "contract_field_content_boundary"),
            field.path("contract_field_required").asBoolean(false)))
        .toList();
    requireUniqueTextValues(
        fieldSpecs.stream().map(CandidatePresentationContractFieldSpec::key).toList(),
        CATALOG_LABEL,
        "top_level_fields.contract_field_key");
    return fieldSpecs;
  }

  public String contractName() {
    return requiredText(generationOutputContractSchemaCatalog, CATALOG_LABEL, "contract_name");
  }

  public String responseFormatType() {
    return requiredText(generationOutputContractSchemaCatalog, CATALOG_LABEL, "response_format_type");
  }

  public String contractFieldDescription(String fieldKey) {
    return contractTopLevelFields().stream()
        .filter(field -> field.key().equals(fieldKey))
        .findFirst()
        .map(CandidatePresentationContractFieldSpec::description)
        .orElse("");
  }

  public Map<String, Object> canonicalGenerationContextPayload() {
    LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
    payload.put("generationPolicy", generationPolicyPayload());
    payload.put("generationContentStructure", generationContentStructurePayload());
    payload.put("generationContentConstraints", generationContentConstraintsPayload());
    payload.put("generationEvidenceTraces", generationEvidenceTracesPayload());
    payload.put("generationOutputContractSchema", generationOutputContractSchemaPayload());
    return java.util.Collections.unmodifiableMap(payload);
  }

  private List<CandidatePresentationSectionSpec> sectionSpecs(String fieldName) {
    List<CandidatePresentationSectionSpec> specs = array(generationContentStructureCatalog, CATALOG_LABEL, fieldName).stream()
        .map(section -> new CandidatePresentationSectionSpec(
            requiredText(section, CATALOG_LABEL, "section_key"),
            requiredText(section, CATALOG_LABEL, "section_title"),
            optionalText(section, "section_title_template"),
            requiredText(section, CATALOG_LABEL, "section_audience"),
            section.path("section_order").asInt(0),
            section.path("section_required").asBoolean(false),
            section.path("section_markdown_level").asInt(1),
            optionalText(section, "section_parent_title"),
            requiredText(section, CATALOG_LABEL, "section_description"),
            requiredText(section, CATALOG_LABEL, "section_content_boundary"),
            optionalText(section, "section_draft_placeholder"),
            section.path("section_max_length").asInt(500)))
        .toList();
    requireUniqueTextValues(
        specs.stream().map(CandidatePresentationSectionSpec::key).toList(),
        CATALOG_LABEL,
        fieldName + ".section_key");
    return specs;
  }

  private List<String> stringList(String fieldName) {
    return array(generationContentConstraintsCatalog, CATALOG_LABEL, fieldName).stream()
        .map(JsonNode::asText)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toList();
  }

  private JsonNode readCatalog(String fileName, String expectedCatalogId, String versionKey) {
    JsonNode root = catalogJsonLoader.loadCatalogObject(CANDIDATE_PRESENTATION_GENERATION_CATALOG_DIR.resolve(fileName));
    requireCatalogId(root, CATALOG_LABEL, expectedCatalogId);
    requiredText(root, CATALOG_LABEL, versionKey);
    return root;
  }

  private Map<String, Object> objectMap(JsonNode node) {
    return objectMapper.convertValue(
        node,
        new TypeReference<Map<String, Object>>() {
        });
  }

  private static Map<String, String> draftContent(List<CandidatePresentationSectionSpec> sectionSpecs) {
    LinkedHashMap<String, String> draft = new LinkedHashMap<>();
    sectionSpecs.stream()
        .filter(CandidatePresentationSectionSpec::required)
        .forEach(sectionSpec -> draft.put(sectionSpec.key(), sectionSpec.draftPlaceholder()));
    return java.util.Collections.unmodifiableMap(draft);
  }

  public record CandidatePresentationSectionSpec(
      String key,
      String title,
      String titleTemplate,
      String audience,
      int order,
      boolean required,
      int markdownLevel,
      String parentTitle,
      String description,
      String contentBoundary,
      String draftPlaceholder,
      int maxLength) {
  }

  public record CandidatePresentationContractFieldSpec(
      String key,
      String description,
      String contentBoundary,
      boolean required) {
  }

  public record CandidatePresentationEvidenceTraceFieldSpec(
      String key,
      String title,
      String description) {
  }
}
