package mcp.server.domain.candidate_presentation.application.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_presentation.exception.CandidatePresentationException;
import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

class CandidatePresentationGenerationContractServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CandidatePresentationGenerationCatalogService catalogService =
            new CandidatePresentationGenerationCatalogService(
                    objectMapper,
                    new ProjectCatalogJsonLoader(objectMapper));
    private final CandidatePresentationGenerationContractService contractService =
            new CandidatePresentationGenerationContractService(objectMapper, catalogService);

    @Test
    void generationOutputSchemaUsesCatalogBackedClosedContract() {
        Map<String, Object> schema = contractService.generationOutputJsonSchema();
        Map<String, Object> properties = objectMap(schema.get("properties"));
        Map<String, Object> customerFacingContent = objectMap(properties.get("customerFacingContent"));
        Map<String, Object> opsReviewContent = objectMap(properties.get("opsReviewContent"));
        Map<String, Object> evidenceTrace = objectMap(properties.get("evidenceTrace"));

        assertThat(schema).containsEntry("type", "object");
        assertThat(schema).containsEntry("additionalProperties", false);
        assertThat(list(schema.get("required")))
                .containsExactlyElementsOf(catalogService.generationOutputRequiredFields());
        assertThat(properties.keySet()).containsExactly(
                "presentationTitle",
                "customerFacingContent",
                "opsReviewContent",
                "evidenceTrace");
        assertThat(customerFacingContent).containsEntry("additionalProperties", false);
        assertThat(list(customerFacingContent.get("required")))
                .containsExactlyElementsOf(catalogService.customerFacingRequiredSections());
        assertThat(opsReviewContent).containsEntry("additionalProperties", false);
        assertThat(list(opsReviewContent.get("required")))
                .containsExactlyElementsOf(catalogService.opsReviewRequiredSections());
        assertThat(evidenceTrace).containsEntry("type", "array");
        assertThat(evidenceTrace).containsEntry("minItems", 1);
    }

    @Test
    void validateGeneratedContentJsonAcceptsExactGeneratedContentContract() {
        contractService.validateGeneratedContentJson(
                validCustomerFacingContentJson(),
                validOpsReviewContentJson(),
                validEvidenceTraceJson());
    }

    @Test
    void validateGeneratedContentJsonRejectsUnsupportedCustomerFacingFields() {
        LinkedHashMap<String, String> customerFacingContent = generatedTextByKey(
                catalogService.customerFacingRequiredSections(),
                "Customer-ready section ");
        customerFacingContent.put("extraField", "This field is not part of the backend-owned contract.");

        assertThatThrownBy(() -> contractService.validateGeneratedContentJson(
                json(customerFacingContent),
                validOpsReviewContentJson(),
                validEvidenceTraceJson()))
                .isInstanceOf(CandidatePresentationException.class)
                .hasMessageContaining("unsupported fields");
    }

    @Test
    void validateGeneratedContentJsonRejectsCustomerFacingRuntimeTerms() {
        String disallowedTerm = catalogService.customerFacingDisallowedRuntimeTerms().get(0);
        LinkedHashMap<String, String> customerFacingContent = generatedTextByKey(
                catalogService.customerFacingRequiredSections(),
                "Customer-ready section ");
        customerFacingContent.replace(
                catalogService.customerFacingRequiredSections().get(0),
                "This customer-facing content leaks " + disallowedTerm);

        assertThatThrownBy(() -> contractService.validateGeneratedContentJson(
                json(customerFacingContent),
                validOpsReviewContentJson(),
                validEvidenceTraceJson()))
                .isInstanceOf(CandidatePresentationException.class)
                .hasMessageContaining(disallowedTerm);
    }

    private String validCustomerFacingContentJson() {
        return json(generatedTextByKey(
                catalogService.customerFacingRequiredSections(),
                "Customer-ready section "));
    }

    private String validOpsReviewContentJson() {
        return json(generatedTextByKey(
                catalogService.opsReviewRequiredSections(),
                "Operations review section "));
    }

    private String validEvidenceTraceJson() {
        return json(List.of(generatedTextByKey(
                catalogService.evidenceTraceRequiredFields(),
                "Evidence trace field ")));
    }

    private static LinkedHashMap<String, String> generatedTextByKey(
            List<String> keys,
            String prefix) {
        LinkedHashMap<String, String> generatedText = new LinkedHashMap<>();
        for (String key : keys) {
            generatedText.put(key, prefix + key);
        }
        return generatedText;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Object value) {
        return (List<String>) value;
    }
}
