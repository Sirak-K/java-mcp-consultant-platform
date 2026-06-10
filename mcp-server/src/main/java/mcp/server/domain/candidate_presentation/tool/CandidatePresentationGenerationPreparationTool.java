package mcp.server.domain.candidate_presentation.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import mcp.server.domain.candidate_presentation.application.evidence.CandidatePresentationEvidenceView;
import mcp.server.domain.candidate_presentation.application.evidence.CandidatePresentationEvidenceService;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationContractService;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactLogService;
import mcp.server.foundation.tool_interface.ToolExecCtx;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolReqs;
import mcp.server.foundation.tool_interface.ToolResponse;

import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.closedObjectSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.integerSchema;
import static mcp.server.foundation.tool_interface.ToolReqsSupport.ToolReqSupportRequiredLong;

@Component
public final class CandidatePresentationGenerationPreparationTool {

  public static final String COLLECT_EVIDENCE_TOOL_NAME = "candidatePresentation.collectEvidence";
  public static final String GET_GENERATION_CONTRACT_TOOL_NAME = "candidatePresentation.getGenerationContract";
  private static final String MATCH_ID_ARGUMENT = "matchId";

  private final CandidatePresentationEvidenceService evidenceService;
  private final CandidatePresentationGenerationContractService generationContractService;
  private final CandidatePresentationArtifactLogService logService;
  private final ObjectMapper objectMapper;

  public CandidatePresentationGenerationPreparationTool(
      CandidatePresentationEvidenceService evidenceService,
      CandidatePresentationGenerationContractService generationContractService,
      CandidatePresentationArtifactLogService logService,
      ObjectMapper objectMapper) {
    this.evidenceService = Objects.requireNonNull(evidenceService, "evidenceService");
    this.generationContractService = Objects.requireNonNull(generationContractService, "generationContractService");
    this.logService = Objects.requireNonNull(logService, "logService");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public ToolInterface collectEvidenceTool() {
    return new CollectEvidenceImpl();
  }

  public ToolInterface getGenerationContractTool() {
    return new GetGenerationContractImpl();
  }

  private final class CollectEvidenceImpl implements ToolInterface {

    @Override
    public String getName() {
      return COLLECT_EVIDENCE_TOOL_NAME;
    }

    @Override
    public String getTitle() {
      return "Collect Candidate Presentation Evidence";
    }

    @Override
    public String getDescription() {
      return "Collect canonical Candidate Presentation evidence for one persisted match. "
          + "Use this read-only capability before generating customer-facing Candidate Presentation content. "
          + "The tool returns backend-owned mission, candidate profile and matching evidence.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(
          Map.of(
              MATCH_ID_ARGUMENT,
              integerSchema("Persisted candidate-to-mission-slot match identifier.")),
          List.of(MATCH_ID_ARGUMENT));
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return closedObjectSchema(
          Map.of(
              "matchContext", objectSchema(),
              "missionContext", objectSchema(),
              "candidateContext", objectSchema(),
              "skillEvidence", objectSchema(),
              "experienceEvidence", objectSchema(),
              "internalEvidenceTrace", objectSchema()),
          List.of(
              "matchContext",
              "missionContext",
              "candidateContext",
              "skillEvidence",
              "experienceEvidence",
              "internalEvidenceTrace"));
    }

    @Override
    public boolean getReadOnlyHint() {
      return true;
    }

    @Override
    public boolean getDestructiveHint() {
      return false;
    }

    @Override
    public boolean getIdempotentHint() {
      return true;
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
      long matchId = ToolReqSupportRequiredLong(req, MATCH_ID_ARGUMENT);
      logService.logMcpToolCalled(getName(), matchId, null);
      CandidatePresentationEvidenceView evidence = evidenceService.collectEvidence(matchId);
      return ToolResponse.ToolRespStructured(
          structuredContent(evidence),
          "Candidate Presentation evidence collected from canonical runtime data.");
    }
  }

  private final class GetGenerationContractImpl implements ToolInterface {

    @Override
    public String getName() {
      return GET_GENERATION_CONTRACT_TOOL_NAME;
    }

    @Override
    public String getTitle() {
      return "Get Candidate Presentation Generation Contract";
    }

    @Override
    public String getDescription() {
      return "Return the backend-owned Candidate Presentation generation output contract. "
          + "AI runtimes use this read-only capability to validate model output before recording a generation result.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(Map.of(), List.of());
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return generationContractService.generationContractOutputSchema();
    }

    @Override
    public boolean getReadOnlyHint() {
      return true;
    }

    @Override
    public boolean getDestructiveHint() {
      return false;
    }

    @Override
    public boolean getIdempotentHint() {
      return true;
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
      logService.logMcpToolCalled(getName(), null, null);
      return ToolResponse.ToolRespStructured(
          generationContractService.generationContractPayload(),
          "Candidate Presentation generation contract returned.");
    }
  }

  private static Map<String, Object> objectSchema() {
    return Map.of("type", "object", "additionalProperties", true);
  }

  private static void throwIfCancelled(ToolExecCtx context) {
    if (context != null) {
      context.ToolExecCtxThrowIfCancelled();
    }
  }

  private Map<String, Object> structuredContent(Object value) {
    return objectMapper.convertValue(
        value,
        new TypeReference<LinkedHashMap<String, Object>>() {
        });
  }
}
