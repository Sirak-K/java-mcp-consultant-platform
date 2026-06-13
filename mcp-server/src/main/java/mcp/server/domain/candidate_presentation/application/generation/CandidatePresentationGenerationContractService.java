package mcp.server.domain.candidate_presentation.application.generation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_presentation.exception.CandidatePresentationException;

@Service
public class CandidatePresentationGenerationContractService {

  private static final List<String> COLLECT_EVIDENCE_REQUIRED_FIELDS = List.of(
      "matchContext",
      "missionContext",
      "candidateContext",
      "skillEvidence",
      "experienceEvidence",
      "internalEvidenceTrace");

  private final ObjectMapper objectMapper;
  private final CandidatePresentationGenerationCatalogService catalogService;

  public CandidatePresentationGenerationContractService(
      ObjectMapper objectMapper,
      CandidatePresentationGenerationCatalogService catalogService) {
    this.objectMapper = objectMapper;
    this.catalogService = catalogService;
  }

  public Map<String, Object> generationContractPayload() {
    LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
    payload.put("contractName", catalogService.contractName());
    payload.put("responseFormatType", catalogService.responseFormatType());
    payload.put("requiredFields", catalogService.generationOutputRequiredFields());
    payload.put("customerFacingContentRequiredSections", catalogService.customerFacingRequiredSections());
    payload.put("opsReviewContentRequiredSections", catalogService.opsReviewRequiredSections());
    payload.put("evidenceTraceRequiredFields", catalogService.evidenceTraceRequiredFields());
    payload.put("collectEvidenceRequiredFields", COLLECT_EVIDENCE_REQUIRED_FIELDS);
    payload.put("customerFacingContentDisallowedRuntimeTerms",
        catalogService.customerFacingDisallowedRuntimeTerms());
    payload.put("customerFacingContentDisallowedTextFragments",
        catalogService.customerFacingDisallowedTextFragments());
    payload.put("customerFacingContentDisallowedUnicodeScripts",
        catalogService.customerFacingDisallowedUnicodeScripts());
    payload.put("generationContext", catalogService.canonicalGenerationContextPayload());
    payload.put("jsonSchema", generationOutputJsonSchema());
    return payload;
  }

  public Map<String, Object> generationContractOutputSchema() {
    LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
    properties.put("contractName", stringSchema("Canonical generation contract name."));
    properties.put("responseFormatType", stringSchema("Response format type used by the inference runtime."));
    properties.put("requiredFields", stringArraySchema());
    properties.put("customerFacingContentRequiredSections", stringArraySchema());
    properties.put("opsReviewContentRequiredSections", stringArraySchema());
    properties.put("evidenceTraceRequiredFields", stringArraySchema());
    properties.put("collectEvidenceRequiredFields", stringArraySchema());
    properties.put("customerFacingContentDisallowedRuntimeTerms", stringArraySchema());
    properties.put("customerFacingContentDisallowedTextFragments", stringArraySchema());
    properties.put("customerFacingContentDisallowedUnicodeScripts", stringArraySchema());
    properties.put("generationContext", Map.of("type", "object", "additionalProperties", true));
    properties.put("jsonSchema", Map.of("type", "object", "additionalProperties", true));
    return closedObjectSchema(properties, List.copyOf(properties.keySet()));
  }

  public Map<String, Object> generationOutputJsonSchema() {
    return catalogService.generationOutputJsonSchemaPayload();
  }

  public void validateGeneratedContentJson(
      String customerFacingContentJson,
      String opsReviewContentJson,
      String evidenceTraceJson) {
    validateExactTextObject(
        parseJson(customerFacingContentJson, "customerFacingContentJson"),
        catalogService.customerFacingRequiredSections(),
        "customerFacingContentJson",
        true);
    validateExactTextObject(
        parseJson(opsReviewContentJson, "opsReviewContentJson"),
        catalogService.opsReviewRequiredSections(),
        "opsReviewContentJson",
        false);
    validateEvidenceTrace(
        parseJson(evidenceTraceJson, "evidenceTraceJson"));
  }

  private static Map<String, Object> stringSchema(String description) {
    LinkedHashMap<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "string");
    schema.put("description", description);
    return schema;
  }

  private static Map<String, Object> stringArraySchema() {
    return Map.of(
        "type", "array",
        "items", Map.of("type", "string"));
  }

  private static Map<String, Object> closedObjectSchema(
      Map<String, Object> properties,
      List<String> required) {
    LinkedHashMap<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", required);
    schema.put("additionalProperties", false);
    return schema;
  }

  private JsonNode parseJson(String rawJson, String fieldName) {
    try {
      return objectMapper.readTree(rawJson);
    } catch (JsonProcessingException exception) {
      throw CandidatePresentationException.invalidRequest(fieldName + " must contain valid JSON", exception);
    }
  }

  private void validateExactTextObject(
      JsonNode node,
      List<String> requiredFields,
      String fieldName,
      boolean customerFacing) {
    if (node == null || !node.isObject()) {
      throw CandidatePresentationException.invalidRequest(fieldName + " must be a JSON object");
    }
    Set<String> actualFields = new LinkedHashSet<>();
    node.fieldNames().forEachRemaining(actualFields::add);
    validateExactFields(actualFields, requiredFields, fieldName);
    for (String requiredField : requiredFields) {
      requireNodeText(node.get(requiredField), fieldName + "." + requiredField, customerFacing);
    }
  }

  private void validateEvidenceTrace(JsonNode node) {
    if (node == null || !node.isArray() || node.isEmpty()) {
      throw CandidatePresentationException.invalidRequest("evidenceTraceJson must be a non-empty JSON array");
    }
    for (int index = 0; index < node.size(); index++) {
      JsonNode entry = node.get(index);
      if (entry == null || !entry.isObject()) {
        throw CandidatePresentationException.invalidRequest("evidenceTraceJson[" + index + "] must be a JSON object");
      }
      Set<String> actualFields = new LinkedHashSet<>();
      entry.fieldNames().forEachRemaining(actualFields::add);
      String entryName = "evidenceTraceJson[" + index + "]";
      List<String> evidenceTraceRequiredFields = catalogService.evidenceTraceRequiredFields();
      validateExactFields(actualFields, evidenceTraceRequiredFields, entryName);
      for (String requiredField : evidenceTraceRequiredFields) {
        requireNodeText(entry.get(requiredField), entryName + "." + requiredField, false);
      }
    }
  }

  private void validateExactFields(
      Set<String> actualFields,
      List<String> requiredFields,
      String fieldName) {
    Set<String> requiredFieldSet = new LinkedHashSet<>(requiredFields);
    List<String> missingFields = new ArrayList<>(requiredFieldSet);
    missingFields.removeAll(actualFields);
    if (!missingFields.isEmpty()) {
      throw CandidatePresentationException.invalidRequest(fieldName + " is missing required fields: " + String.join(", ", missingFields));
    }
    List<String> unsupportedFields = new ArrayList<>(actualFields);
    unsupportedFields.removeAll(requiredFieldSet);
    if (!unsupportedFields.isEmpty()) {
      throw CandidatePresentationException.invalidRequest(fieldName + " contains unsupported fields: " + String.join(", ", unsupportedFields));
    }
  }

  private void requireNodeText(JsonNode node, String fieldName, boolean customerFacing) {
    if (node == null || !node.isTextual() || node.asText().trim().isBlank()) {
      throw CandidatePresentationException.invalidRequest(fieldName + " must be non-empty text");
    }
    String text = node.asText();
    if (text.contains("```")) {
      throw CandidatePresentationException.invalidRequest(fieldName + " must not contain Markdown fences");
    }
    if (customerFacing) {
      validateCustomerFacingText(text, fieldName);
    }
  }

  private void validateCustomerFacingText(String text, String fieldName) {
    for (String term : catalogService.customerFacingDisallowedRuntimeTerms()) {
      if (!term.isBlank() && text.contains(term)) {
        throw CandidatePresentationException.invalidRequest(fieldName + " contains customer-facing runtime term: " + term);
      }
    }
    for (String fragment : catalogService.customerFacingDisallowedTextFragments()) {
      if (!fragment.isBlank() && text.contains(fragment)) {
        throw CandidatePresentationException.invalidRequest(fieldName + " contains customer-facing corrupted text fragment");
      }
    }
    Set<String> disallowedScripts = new LinkedHashSet<>(
        catalogService.customerFacingDisallowedUnicodeScripts());
    text.codePoints().forEach(codePoint -> {
      Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
      if (disallowedScripts.contains(script.name())) {
        throw CandidatePresentationException.invalidRequest(fieldName + " contains customer-facing disallowed unicode script: " + script.name());
      }
    });
  }
}
