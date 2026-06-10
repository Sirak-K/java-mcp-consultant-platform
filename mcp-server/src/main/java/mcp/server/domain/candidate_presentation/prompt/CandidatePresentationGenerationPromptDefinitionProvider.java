package mcp.server.domain.candidate_presentation.prompt;

import java.util.List;

import mcp.server.domain.candidate_presentation.tool.CandidatePresentationGenerationPreparationTool;
import mcp.server.domain.candidate_presentation.tool.CandidatePresentationGenerationResultRecordingTool;
import mcp.server.foundation.prompt_interface.PromptArgumentSchema;
import mcp.server.foundation.prompt_interface.PromptDefin;
import mcp.server.foundation.prompt_interface.PromptDefinProvid;

import org.springframework.stereotype.Component;

@Component
public final class CandidatePresentationGenerationPromptDefinitionProvider implements PromptDefinProvid {

    private static final String CANDIDATE_PRESENTATION_GENERATION_PROMPT = "candidatePresentation.generateDraftPrompt";
    private static final String CANDIDATE_PRESENTATION_GENERATION_PROMPT_RESOURCE = "prompts/candidate_presentation_generation_prompt.md";

    @Override
    public List<PromptDefin> PromptDefinProvidListDefinitions() {
        return List.of(new PromptDefin(
                CANDIDATE_PRESENTATION_GENERATION_PROMPT,
                "Generate Candidate Presentation Draft",
                "Server-owned Candidate Presentation generation prompt for customer-facing draft generation.",
                PromptArgumentSchema.PromptArgSchemaEmptyObject(),
                CANDIDATE_PRESENTATION_GENERATION_PROMPT_RESOURCE,
                List.of(
                        CandidatePresentationGenerationPreparationTool.COLLECT_EVIDENCE_TOOL_NAME,
                        CandidatePresentationGenerationPreparationTool.GET_GENERATION_CONTRACT_TOOL_NAME,
                        CandidatePresentationGenerationResultRecordingTool.RECORD_GENERATED_CONTENT_TOOL_NAME,
                        CandidatePresentationGenerationResultRecordingTool.RECORD_GENERATION_FAILURE_TOOL_NAME)));
    }
}
