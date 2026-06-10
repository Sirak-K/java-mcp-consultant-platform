package mcp.server.domain.candidate_presentation.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactView;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationGeneratedContentCommand;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationGenerationFailureCommand;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactService;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactLogService;
import mcp.server.foundation.tool_interface.ToolExecCtx;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolReqs;
import mcp.server.foundation.tool_interface.ToolResponse;

import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.closedObjectSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.integerSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.stringSchema;
import static mcp.server.foundation.tool_interface.ToolReqsSupport.ToolReqSupportOptionalText;
import static mcp.server.foundation.tool_interface.ToolReqsSupport.ToolReqSupportRequiredLong;
import static mcp.server.foundation.tool_interface.ToolReqsSupport.ToolReqSupportRequiredText;

@Component
public final class CandidatePresentationGenerationResultRecordingTool {

  public static final String RECORD_GENERATED_CONTENT_TOOL_NAME = "candidatePresentation.recordGeneratedContent";
  public static final String RECORD_GENERATION_FAILURE_TOOL_NAME = "candidatePresentation.recordGenerationFailure";
  private static final String ARTIFACT_ID_ARGUMENT = "artifactId";
  private static final String PRESENTATION_TITLE_ARGUMENT = "presentationTitle";
  private static final String CUSTOMER_FACING_CONTENT_JSON_ARGUMENT = "customerFacingContentJson";
  private static final String OPERATIONS_REVIEW_CONTENT_JSON_ARGUMENT = "opsReviewContentJson";
  private static final String EVIDENCE_TRACE_JSON_ARGUMENT = "evidenceTraceJson";
  private static final String FAILURE_MESSAGE_ARGUMENT = "failureMessage";
  private static final String FAILURE_DETAIL_ARGUMENT = "failureDetail";
  private static final String FAILURE_STAGE_ARGUMENT = "failureStage";
  private static final String RUN_ID_ARGUMENT = "runId";
  private static final String MODEL_ALIAS_ARGUMENT = "modelAlias";

  private final CandidatePresentationArtifactService artifactService;
  private final CandidatePresentationArtifactLogService logService;
  private final ObjectMapper objectMapper;

  public CandidatePresentationGenerationResultRecordingTool(
      CandidatePresentationArtifactService artifactService,
      CandidatePresentationArtifactLogService logService,
      ObjectMapper objectMapper) {
    this.artifactService = Objects.requireNonNull(artifactService, "artifactService");
    this.logService = Objects.requireNonNull(logService, "logService");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public ToolInterface recordGeneratedContentTool() {
    return new RecordGeneratedContentImpl();
  }

  public ToolInterface recordGenerationFailureTool() {
    return new RecordGenerationFailureImpl();
  }

  private final class RecordGeneratedContentImpl implements ToolInterface {

    @Override
    public String getName() {
      return RECORD_GENERATED_CONTENT_TOOL_NAME;
    }

    @Override
    public String getTitle() {
      return "Record Generated Candidate Presentation Content";
    }

    @Override
    public String getDescription() {
      return "Record generated Candidate Presentation content for an existing artifact. "
          + "This capability writes customer-facing content, operations review content and evidence trace, "
          + "then lets backend lifecycle rules mark the artifact as GENERATED.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(
          Map.of(
              ARTIFACT_ID_ARGUMENT,
              integerSchema("Persisted Candidate Presentation artifact identifier."),
              PRESENTATION_TITLE_ARGUMENT,
              stringSchema("Presentation title; backend stores the canonical Candidate Presentation title format."),
              CUSTOMER_FACING_CONTENT_JSON_ARGUMENT,
              stringSchema("JSON string containing generated customer-facing presentation content."),
              OPERATIONS_REVIEW_CONTENT_JSON_ARGUMENT,
              stringSchema("JSON string containing operations review notes and internal validation content."),
              EVIDENCE_TRACE_JSON_ARGUMENT,
              stringSchema("JSON string containing evidence trace for the generated content.")),
          List.of(
              ARTIFACT_ID_ARGUMENT,
              PRESENTATION_TITLE_ARGUMENT,
              CUSTOMER_FACING_CONTENT_JSON_ARGUMENT,
              OPERATIONS_REVIEW_CONTENT_JSON_ARGUMENT,
              EVIDENCE_TRACE_JSON_ARGUMENT));
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return artifactViewSchema();
    }

    @Override
    public boolean getReadOnlyHint() {
      return false;
    }

    @Override
    public boolean getDestructiveHint() {
      return false;
    }

    @Override
    public boolean getIdempotentHint() {
      return false;
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      return execute(req, null);
    }

    @Override
    public ToolResponse execute(
        ToolReqs req,
        ToolExecCtx context) {
      throwIfCancelled(context);
      long artifactId = ToolReqSupportRequiredLong(req, ARTIFACT_ID_ARGUMENT);
      logService.logMcpToolCalled(getName(), null, artifactId);
      CandidatePresentationGeneratedContentCommand input = new CandidatePresentationGeneratedContentCommand(
          artifactId,
          ToolReqSupportRequiredText(req, PRESENTATION_TITLE_ARGUMENT),
          ToolReqSupportRequiredText(req, CUSTOMER_FACING_CONTENT_JSON_ARGUMENT),
          ToolReqSupportRequiredText(req, OPERATIONS_REVIEW_CONTENT_JSON_ARGUMENT),
          ToolReqSupportRequiredText(req, EVIDENCE_TRACE_JSON_ARGUMENT));
      CandidatePresentationArtifactView artifact = artifactService.recordGeneratedContent(input);
      return ToolResponse.ToolRespStructured(
          structuredContent(artifact),
          "Candidate Presentation generated content recorded.");
    }
  }

  private final class RecordGenerationFailureImpl implements ToolInterface {

    @Override
    public String getName() {
      return RECORD_GENERATION_FAILURE_TOOL_NAME;
    }

    @Override
    public String getTitle() {
      return "Record Candidate Presentation Generation Failure";
    }

    @Override
    public String getDescription() {
      return "Record Candidate Presentation generation failure for an existing artifact. "
          + "This capability writes failure information into existing operations review and evidence trace fields, "
          + "then lets backend lifecycle rules mark the artifact as GENERATION_FAILED.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(
          Map.of(
              ARTIFACT_ID_ARGUMENT,
              integerSchema("Persisted Candidate Presentation artifact identifier."),
              FAILURE_MESSAGE_ARGUMENT,
              stringSchema("Short operations-readable failure message."),
              FAILURE_DETAIL_ARGUMENT,
              stringSchema("Optional detailed failure context."),
              FAILURE_STAGE_ARGUMENT,
              stringSchema("Runtime stage where generation failed."),
              RUN_ID_ARGUMENT,
              stringSchema("Generation run identifier."),
              MODEL_ALIAS_ARGUMENT,
              stringSchema("Optional inference model alias.")),
          List.of(
              ARTIFACT_ID_ARGUMENT,
              FAILURE_MESSAGE_ARGUMENT,
              FAILURE_STAGE_ARGUMENT,
              RUN_ID_ARGUMENT));
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return artifactViewSchema();
    }

    @Override
    public boolean getReadOnlyHint() {
      return false;
    }

    @Override
    public boolean getDestructiveHint() {
      return false;
    }

    @Override
    public boolean getIdempotentHint() {
      return false;
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      return execute(req, null);
    }

    @Override
    public ToolResponse execute(
        ToolReqs req,
        ToolExecCtx context) {
      throwIfCancelled(context);
      long artifactId = ToolReqSupportRequiredLong(req, ARTIFACT_ID_ARGUMENT);
      logService.logMcpToolCalled(getName(), null, artifactId);
      CandidatePresentationGenerationFailureCommand input = new CandidatePresentationGenerationFailureCommand(
          artifactId,
          ToolReqSupportRequiredText(req, FAILURE_MESSAGE_ARGUMENT),
          ToolReqSupportOptionalText(req, FAILURE_DETAIL_ARGUMENT),
          ToolReqSupportRequiredText(req, FAILURE_STAGE_ARGUMENT),
          ToolReqSupportRequiredText(req, RUN_ID_ARGUMENT),
          ToolReqSupportOptionalText(req, MODEL_ALIAS_ARGUMENT));
      CandidatePresentationArtifactView artifact = artifactService.recordGenerationFailure(input);
      return ToolResponse.ToolRespStructured(
          structuredContent(artifact),
          "Candidate Presentation generation failure recorded.");
    }
  }

  private static void throwIfCancelled(ToolExecCtx context) {
    if (context != null) {
      context.ToolExecCtxThrowIfCancelled();
    }
  }

  private static Map<String, Object> artifactViewSchema() {
    LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
    properties.put("id", integerSchema());
    properties.put("sourceCandidateToSlotMatchId", integerSchema());
    properties.put("candProfileId", integerSchema());
    properties.put("missionId", integerSchema());
    properties.put("missionSlotId", integerSchema());
    properties.put("artifactStatus", stringSchema());
    properties.put("presentationTitle", stringSchema());
    properties.put("customerFacingContentJson", stringSchema());
    properties.put("opsReviewContentJson", stringSchema());
    properties.put("evidenceTraceJson", stringSchema());
    properties.put("createdAt", stringSchema());
    properties.put("updatedAt", stringSchema());
    return closedObjectSchema(
        Map.copyOf(properties),
        List.of(
            "id",
            "sourceCandidateToSlotMatchId",
            "candProfileId",
            "missionId",
            "missionSlotId",
            "artifactStatus",
            "presentationTitle",
            "customerFacingContentJson",
            "opsReviewContentJson",
            "evidenceTraceJson",
            "createdAt",
            "updatedAt"));
  }

  private Map<String, Object> structuredContent(Object value) {
    return objectMapper.convertValue(
        value,
        new TypeReference<LinkedHashMap<String, Object>>() {
        });
  }
}
