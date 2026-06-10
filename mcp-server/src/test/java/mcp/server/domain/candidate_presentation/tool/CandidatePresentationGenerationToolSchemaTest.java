package mcp.server.domain.candidate_presentation.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactLogService;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactService;
import mcp.server.domain.candidate_presentation.application.evidence.CandidatePresentationEvidenceService;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationContractService;
import mcp.server.foundation.tool_interface.ToolInterface;

class CandidatePresentationGenerationToolSchemaTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void preparationToolsExposeReadOnlyMcpSchemasForEvidenceAndGenerationContract() {
        CandidatePresentationGenerationContractService contractService =
                mock(CandidatePresentationGenerationContractService.class);
        when(contractService.generationContractOutputSchema()).thenReturn(Map.of(
                "type", "object",
                "required", List.of("contractName")));
        CandidatePresentationGenerationPreparationTool toolFactory =
                new CandidatePresentationGenerationPreparationTool(
                        mock(CandidatePresentationEvidenceService.class),
                        contractService,
                        mock(CandidatePresentationArtifactLogService.class),
                        OBJECT_MAPPER);

        ToolInterface collectEvidence = toolFactory.collectEvidenceTool();
        ToolInterface getGenerationContract = toolFactory.getGenerationContractTool();

        assertThat(collectEvidence.getName())
                .isEqualTo(CandidatePresentationGenerationPreparationTool.COLLECT_EVIDENCE_TOOL_NAME);
        assertThat(collectEvidence.getReadOnlyHint()).isTrue();
        assertThat(collectEvidence.getDestructiveHint()).isFalse();
        assertThat(collectEvidence.getIdempotentHint()).isTrue();
        assertThat(requiredFields(collectEvidence.getInputSchema())).containsExactly("matchId");
        assertThat(requiredFields(collectEvidence.getOutputSchema())).containsExactly(
                "matchContext",
                "missionContext",
                "candidateContext",
                "skillEvidence",
                "experienceEvidence",
                "internalEvidenceTrace");
        assertThat(getGenerationContract.getName())
                .isEqualTo(CandidatePresentationGenerationPreparationTool.GET_GENERATION_CONTRACT_TOOL_NAME);
        assertThat(getGenerationContract.getReadOnlyHint()).isTrue();
        assertThat(requiredFields(getGenerationContract.getInputSchema())).isEmpty();
        assertThat(getGenerationContract.getOutputSchema()).containsEntry("type", "object");
        assertNoLegacyVf4Text(collectEvidence);
        assertNoLegacyVf4Text(getGenerationContract);
    }

    @Test
    void resultRecordingToolsExposeWritebackSchemasForGeneratedContentAndFailures() {
        CandidatePresentationGenerationResultRecordingTool toolFactory =
                new CandidatePresentationGenerationResultRecordingTool(
                        mock(CandidatePresentationArtifactService.class),
                        mock(CandidatePresentationArtifactLogService.class),
                        OBJECT_MAPPER);

        ToolInterface recordGeneratedContent = toolFactory.recordGeneratedContentTool();
        ToolInterface recordGenerationFailure = toolFactory.recordGenerationFailureTool();

        assertThat(recordGeneratedContent.getName())
                .isEqualTo(CandidatePresentationGenerationResultRecordingTool.RECORD_GENERATED_CONTENT_TOOL_NAME);
        assertThat(recordGeneratedContent.getReadOnlyHint()).isFalse();
        assertThat(recordGeneratedContent.getDestructiveHint()).isFalse();
        assertThat(recordGeneratedContent.getIdempotentHint()).isFalse();
        assertThat(requiredFields(recordGeneratedContent.getInputSchema())).containsExactly(
                "artifactId",
                "presentationTitle",
                "customerFacingContentJson",
                "opsReviewContentJson",
                "evidenceTraceJson");
        assertThat(recordGenerationFailure.getName())
                .isEqualTo(CandidatePresentationGenerationResultRecordingTool.RECORD_GENERATION_FAILURE_TOOL_NAME);
        assertThat(recordGenerationFailure.getReadOnlyHint()).isFalse();
        assertThat(recordGenerationFailure.getDestructiveHint()).isFalse();
        assertThat(recordGenerationFailure.getIdempotentHint()).isFalse();
        assertThat(requiredFields(recordGenerationFailure.getInputSchema())).containsExactly(
                "artifactId",
                "failureMessage",
                "failureStage",
                "runId");
        assertNoLegacyVf4Text(recordGeneratedContent);
        assertNoLegacyVf4Text(recordGenerationFailure);
    }

    @SuppressWarnings("unchecked")
    private static List<String> requiredFields(Map<String, Object> schema) {
        return (List<String>) schema.get("required");
    }

    private static void assertNoLegacyVf4Text(ToolInterface tool) {
        assertThat(tool.getName()).doesNotContainIgnoringCase("vf4");
        assertThat(tool.getTitle()).doesNotContainIgnoringCase("vf4");
        assertThat(tool.getDescription()).doesNotContainIgnoringCase("vf4");
        assertThat(json(tool.getInputSchema())).doesNotContainIgnoringCase("vf4");
        assertThat(json(tool.getOutputSchema())).doesNotContainIgnoringCase("vf4");
    }

    private static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
