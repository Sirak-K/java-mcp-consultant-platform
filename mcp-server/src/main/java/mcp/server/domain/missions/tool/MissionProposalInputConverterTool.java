package mcp.server.domain.missions.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mcp.server.domain.missions.application.intake.MissionProposalIntake;

import mcp.server.domain.missions.application.intake.MissionProposalWorkingCopyService;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolReqs;
import mcp.server.foundation.tool_interface.ToolResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.arraySchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.closedObjectSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.integerSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.stringSchema;
import static mcp.server.foundation.tool_interface.ToolReqsSupport.ToolReqSupportRequiredText;

@Component
public final class MissionProposalInputConverterTool {

  public static final String PREVIEW_FROM_TEXT_TOOL_NAME = "missionProposal.previewFromText";
  private static final String SOURCE_TEXT_ARGUMENT = "roleAndRequirementsText";

  private final MissionProposalWorkingCopyService workingCopyService;
  private final ObjectMapper objectMapper;

  public MissionProposalInputConverterTool(
      MissionProposalWorkingCopyService workingCopyService,
      ObjectMapper objectMapper) {
    this.workingCopyService = Objects.requireNonNull(workingCopyService, "workingCopyService");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public ToolInterface previewFromTextTool() {
    return new PreviewFromTextImpl();
  }

  private final class PreviewFromTextImpl implements ToolInterface {

    @Override
    public String getName() {
      return PREVIEW_FROM_TEXT_TOOL_NAME;
    }

    @Override
    public String getDescription() {
      return "Convert mission free-text requirements into the canonical mission proposal workingCopy. "
          + "Use this tool for mission proposal intake when an MCP client needs structured mission slot data. "
          + "The tool uses the same backend service as the public UI and returns proposalWorkingCopy, evidence and missingFields.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(
          properties(
              entry(
                  SOURCE_TEXT_ARGUMENT,
                  Map.of(
                      "type", "string",
                      "description", "Mission free-text from the Roll & Krav intake field"))),
          List.of(SOURCE_TEXT_ARGUMENT));
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return outputSchema();
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      String sourceText = ToolReqSupportRequiredText(req, SOURCE_TEXT_ARGUMENT);
      MissionProposalIntake.PreviewView preview = workingCopyService.previewMissionProposal(
          new MissionProposalIntake.PreviewInput(sourceText));
      Map<String, Object> structuredContent = objectMapper.convertValue(
          preview,
          new TypeReference<LinkedHashMap<String, Object>>() {
          });
      return ToolResponse.ToolRespStructured(
          structuredContent,
          "Mission proposal workingCopy preview created. Review missingFields and evidence before submit.");
    }
  }

  private Map<String, Object> outputSchema() {
    return closedObjectSchema(
        properties(
            entry("proposalWorkingCopy", proposalWorkingCopySchema()),
            entry("evidence", arraySchema(evidenceSchema())),
            entry("missingFields", arraySchema(stringSchema()))),
        List.of("proposalWorkingCopy", "evidence", "missingFields"));
  }

  private Map<String, Object> proposalWorkingCopySchema() {
    return closedObjectSchema(
        properties(
            entry("customerName", stringSchema()),
            entry("customerEmail", stringSchema()),
            entry("missionTitle", stringSchema()),
            entry("missionSlots", arraySchema(missionSlotSchema())),
            entry("startDate", stringSchema()),
            entry("endDate", stringSchema()),
            entry("workMode", stringSchema())),
        List.of(
            "customerName",
            "customerEmail",
            "missionTitle",
            "missionSlots",
            "startDate",
            "endDate",
            "workMode"));
  }

  private Map<String, Object> missionSlotSchema() {
    return closedObjectSchema(
        properties(
            entry("roleId", integerSchema()),
            entry("requiredRoleExperienceYears", integerSchema()),
            entry("requiredSkills", arraySchema(requiredSkillSchema()))),
        List.of("roleId", "requiredRoleExperienceYears", "requiredSkills"));
  }

  private Map<String, Object> requiredSkillSchema() {
    return closedObjectSchema(
        properties(
            entry("skillId", integerSchema()),
            entry("skillCategory", stringSchema()),
            entry("skillLevelId", integerSchema())),
        List.of("skillId", "skillCategory", "skillLevelId"));
  }

  private Map<String, Object> evidenceSchema() {
    return closedObjectSchema(
        properties(
            entry("field", stringSchema()),
            entry("value", stringSchema()),
            entry("sourceText", stringSchema()),
            entry("confidence", nullableNumberSchema())),
        List.of("field", "value", "sourceText"));
  }

  @SafeVarargs
  private final Map<String, Object> properties(Map.Entry<String, Object>... entries) {
    LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : entries) {
      properties.put(entry.getKey(), entry.getValue());
    }
    return Map.copyOf(properties);
  }

  private Map.Entry<String, Object> entry(String key, Object value) {
    return Map.entry(key, value);
  }

  private Map<String, Object> nullableNumberSchema() {
    return Map.of("type", List.of("number", "null"));
  }
}
