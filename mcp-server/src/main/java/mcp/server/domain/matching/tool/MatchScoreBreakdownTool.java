package mcp.server.domain.matching.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.matching.application.viewer.MatchScoreBreakdownQueryService;
import mcp.server.domain.matching.api.MatchScoreBreakdownView;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolReqs;
import mcp.server.foundation.tool_interface.ToolResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.arraySchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.booleanSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.closedObjectSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.integerSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.stringSchema;
import static mcp.server.foundation.tool_interface.ToolReqsSupport.ToolReqSupportRequiredLong;

@Component
public final class MatchScoreBreakdownTool {

  public static final String INSPECT_TOOL_NAME = "inspectMatchScoreBreakdown";
  private static final String MATCH_ID_ARGUMENT = "matchId";

  private final MatchScoreBreakdownQueryService scoreBreakdownQueryService;
  private final ObjectMapper objectMapper;

  public MatchScoreBreakdownTool(
      MatchScoreBreakdownQueryService scoreBreakdownQueryService,
      ObjectMapper objectMapper) {
    this.scoreBreakdownQueryService = Objects.requireNonNull(scoreBreakdownQueryService, "scoreBreakdownQueryService");
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
      return "Inspect the canonical Candidate-to-Mission-Slot Match Score breakdown for one persisted match. "
          + "Use this match inspection capability when an MCP client needs score factors, threshold decision, "
          + "matched skills and missing or weak factors without recalculating score outside Java.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(
          Map.of(
              MATCH_ID_ARGUMENT,
              Map.of(
                  "type", "integer",
                  "description", "persisted match identifier to inspect")),
          List.of(MATCH_ID_ARGUMENT));
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return closedObjectSchema(
          Map.of(
              "matchId", integerSchema(),
              "score", integerSchema(),
              "scoreLabel", stringSchema(),
              "discoveryThreshold", integerSchema(),
              "passedDiscoveryThreshold", booleanSchema(),
              "decision", stringSchema(),
              "factors", arraySchema(factorSchema()),
              "matchedSkills", arraySchema(stringSchema()),
              "missingOrWeakFactors", arraySchema(stringSchema()),
              "matchedAt", nullableStringSchema()),
          List.of(
              "matchId",
              "score",
              "scoreLabel",
              "discoveryThreshold",
              "passedDiscoveryThreshold",
              "decision",
              "factors",
              "matchedSkills",
              "missingOrWeakFactors",
              "matchedAt"));
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      long matchId = ToolReqSupportRequiredLong(req, MATCH_ID_ARGUMENT);
      MatchScoreBreakdownView breakdown = scoreBreakdownQueryService.inspectMatchScoreBreakdown(matchId);
      Map<String, Object> structuredContent = objectMapper.convertValue(
          breakdown,
          new TypeReference<LinkedHashMap<String, Object>>() {
          });
      return ToolResponse.ToolRespStructured(
          structuredContent,
          "Match score breakdown inspected from canonical persisted match snapshot.");
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
  }

  private static Map<String, Object> factorSchema() {
    return closedObjectSchema(
        Map.of(
            "factor", stringSchema(),
            "matched", booleanSchema(),
            "matchedCount", integerSchema(),
            "requiredCount", integerSchema(),
            "scorePerInstance", integerSchema(),
            "points", integerSchema(),
            "evidence", arraySchema(stringSchema()),
            "note", stringSchema()),
        List.of(
            "factor",
            "matched",
            "matchedCount",
            "requiredCount",
            "scorePerInstance",
            "points",
            "evidence",
            "note"));
  }

  private static Map<String, Object> nullableStringSchema() {
    return Map.of("type", List.of("string", "null"));
  }

}
