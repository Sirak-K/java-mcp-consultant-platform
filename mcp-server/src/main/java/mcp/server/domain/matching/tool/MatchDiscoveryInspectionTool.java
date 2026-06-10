package mcp.server.domain.matching.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.matching.application.viewer.MatchViewerQueryService;
import mcp.server.domain.matching.api.MatchViewerReadModel;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolReqs;
import mcp.server.foundation.tool_interface.ToolResponse;
import org.springframework.stereotype.Component;

import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.arraySchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.booleanSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.closedObjectSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.integerSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.stringSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public final class MatchDiscoveryInspectionTool {

  public static final String INSPECT_TOOL_NAME = "matchDiscovery.inspect";

  private final MatchViewerQueryService matchViewerService;
  private final ObjectMapper objectMapper;

  public MatchDiscoveryInspectionTool(
      MatchViewerQueryService matchViewerService,
      ObjectMapper objectMapper) {
    this.matchViewerService = Objects.requireNonNull(matchViewerService, "matchViewerService");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public ToolInterface inspectTool() {
    return new InspectImpl();
  }

  private final class InspectImpl implements ToolInterface {

    @Override
    public String getName() {
      return INSPECT_TOOL_NAME;
    }

    @Override
    public String getDescription() {
      return "Inspect current Match Discovery state from the canonical Match Viewer backend service. "
          + "Use this read-only capability when an MCP client needs missions, mission slots and candidate profile matches "
          + "without recalculating score or mutating matching data.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(Map.of(), List.of());
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return closedObjectSchema(
          Map.of(
              "generatedAt", stringSchema(),
              "missions", arraySchema(missionSchema())),
          List.of("generatedAt", "missions"));
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
    public boolean getOpenWorldHint() {
      return false;
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      MatchViewerReadModel.MatchViewerView matchViewer = matchViewerService.matchViewer();
      Map<String, Object> structuredContent = objectMapper.convertValue(
          matchViewer,
          new TypeReference<LinkedHashMap<String, Object>>() {
          });
      return ToolResponse.ToolRespStructured(
          structuredContent,
          "Match Discovery inspected from canonical Match Viewer backend service.");
    }
  }

  private static Map<String, Object> missionSchema() {
    return closedObjectSchema(
        Map.of(
            "missionId", integerSchema(),
            "missionTitle", nullableStringSchema(),
            "customerEmail", nullableStringSchema(),
            "customerName", nullableStringSchema(),
            "workMode", nullableStringSchema(),
            "status", nullableStringSchema(),
            "startDate", nullableStringSchema(),
            "endDate", nullableStringSchema(),
            "slots", arraySchema(slotSchema())),
        List.of(
            "missionId",
            "missionTitle",
            "customerEmail",
            "customerName",
            "workMode",
            "status",
            "startDate",
            "endDate",
            "slots"));
  }

  private static Map<String, Object> slotSchema() {
    return closedObjectSchema(
        Map.of(
            "missionSlotId", integerSchema(),
            "missionSlotNumber", integerSchema(),
            "roleTitle", nullableStringSchema(),
            "requiredRoleExperienceYears", integerSchema(),
            "requiredSkills", arraySchema(requiredSkillSchema()),
            "matches", arraySchema(matchSchema())),
        List.of(
            "missionSlotId",
            "missionSlotNumber",
            "roleTitle",
            "requiredRoleExperienceYears",
            "requiredSkills",
            "matches"));
  }

  private static Map<String, Object> requiredSkillSchema() {
    return closedObjectSchema(
        Map.of(
            "skillId", integerSchema(),
            "skillTitle", nullableStringSchema(),
            "skillLevelId", integerSchema(),
            "skillLevelName", nullableStringSchema(),
            "skillCategory", nullableStringSchema()),
        List.of("skillId", "skillTitle", "skillLevelId", "skillLevelName", "skillCategory"));
  }

  private static Map<String, Object> matchSchema() {
    return closedObjectSchema(
        Map.of(
            "matchId", integerSchema(),
            "score", integerSchema(),
            "scoreLabel", nullableStringSchema(),
            "roleMatched", booleanSchema(),
            "workModeMatched", booleanSchema(),
            "matchedSkillCount", integerSchema(),
            "requiredSkillCount", integerSchema(),
            "matchedSkills", arraySchema(stringSchema()),
            "matchedAt", nullableStringSchema(),
            "candidateCard", candidateCardSchema()),
        List.of(
            "matchId",
            "score",
            "scoreLabel",
            "roleMatched",
            "workModeMatched",
            "matchedSkillCount",
            "requiredSkillCount",
            "matchedSkills",
            "matchedAt",
            "candidateCard"));
  }

  private static Map<String, Object> candidateCardSchema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("candidateProfileId", integerSchema());
    properties.put("candidateName", nullableStringSchema());
    properties.put("roleTitle", nullableStringSchema());
    properties.put("roleExperienceLevel", nullableStringSchema());
    properties.put("roleExperienceYears", nullableIntegerSchema());
    properties.put("availabilityStatus", nullableStringSchema());
    properties.put("country", nullableStringSchema());
    properties.put("locationFlexibility", nullableStringSchema());
    properties.put("workMode", nullableStringSchema());
    properties.put("primarySkills", arraySchema(candidateSkillSchema()));
    properties.put("secondarySkills", arraySchema(candidateSkillSchema()));
    return closedObjectSchema(
        properties,
        List.of(
            "candidateProfileId",
            "candidateName",
            "roleTitle",
            "roleExperienceLevel",
            "roleExperienceYears",
            "availabilityStatus",
            "country",
            "locationFlexibility",
            "workMode",
            "primarySkills",
            "secondarySkills"));
  }

  private static Map<String, Object> candidateSkillSchema() {
    return closedObjectSchema(
        Map.of(
            "title", stringSchema(),
            "skillLevel", nullableStringSchema()),
        List.of("title", "skillLevel"));
  }

  private static Map<String, Object> nullableStringSchema() {
    return Map.of("type", List.of("string", "null"));
  }

  private static Map<String, Object> nullableIntegerSchema() {
    return Map.of("type", List.of("integer", "null"));
  }

}
