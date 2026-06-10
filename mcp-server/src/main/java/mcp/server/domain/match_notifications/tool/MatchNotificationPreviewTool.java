package mcp.server.domain.match_notifications.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.match_notifications.application.preview.MatchNotificationPreviewService;
import mcp.server.domain.match_notifications.web.MatchNotificationWebContract;
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
import static mcp.server.foundation.tool_interface.ToolReqsSupport.ToolReqSupportRequiredLong;

@Component
public final class MatchNotificationPreviewTool {

  public static final String INSPECT_PREVIEWS_TOOL_NAME = "matchNotifications.inspectPreviews";
  public static final String PREVIEW_MATCH_TOOL_NAME = "matchNotifications.previewMatch";
  private static final String MATCH_ID_ARGUMENT = "matchId";

  private final MatchNotificationPreviewService matchPreviewService;
  private final ObjectMapper objectMapper;

  public MatchNotificationPreviewTool(
      MatchNotificationPreviewService matchPreviewService,
      ObjectMapper objectMapper) {
    this.matchPreviewService = Objects.requireNonNull(matchPreviewService, "matchPreviewService");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public ToolInterface inspectPreviewsTool() {
    return new InspectPreviewsImpl();
  }

  public ToolInterface previewMatchTool() {
    return new PreviewMatchImpl();
  }

  private final class InspectPreviewsImpl implements ToolInterface {

    @Override
    public String getName() {
      return INSPECT_PREVIEWS_TOOL_NAME;
    }

    @Override
    public String getDescription() {
      return "Inspect current match notification previews without sending email. "
          + "Use this read-only capability when an MCP client needs the current candidate-to-mission "
          + "match notification preview queue for OPS review.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(Map.of(), List.of());
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return closedObjectSchema(
          Map.of(
              "count", integerSchema(),
              "previews", arraySchema(Map.of("type", "object", "additionalProperties", true))),
          List.of("count", "previews"));
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
    public boolean getOpenWorldHint() {
      return false;
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      List<MatchNotificationWebContract.MatchNotificationPreviewView> previews = matchPreviewService.previewMatches();
      LinkedHashMap<String, Object> structuredContent = new LinkedHashMap<>();
      structuredContent.put("count", previews.size());
      structuredContent.put(
          "previews",
          objectMapper.convertValue(
              previews,
              new TypeReference<List<LinkedHashMap<String, Object>>>() {
              }));
      return ToolResponse.ToolRespStructured(
          Map.copyOf(structuredContent),
          "Match notification previews inspected without sending email.");
    }
  }

  private final class PreviewMatchImpl implements ToolInterface {

    @Override
    public String getName() {
      return PREVIEW_MATCH_TOOL_NAME;
    }

    @Override
    public String getDescription() {
      return "Preview one match notification email for a persisted candidate-to-slot match without sending email. "
          + "Use this read-only capability when an MCP client needs subject, body and evidence brief for one match.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(
          Map.of(
              MATCH_ID_ARGUMENT,
              integerSchema("candidate_to_slot_match identifier to preview")),
          List.of(MATCH_ID_ARGUMENT));
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return closedObjectSchema(
          Map.of(
              "matchId", integerSchema(),
              "matchIds", arraySchema(integerSchema()),
              "candidateProfileId", integerSchema(),
              "missionId", integerSchema(),
              "groupedMatchCount", integerSchema(),
              "subject", stringSchema(),
              "evidenceBrief", stringSchema(),
              "textBody", stringSchema(),
              "htmlBody", stringSchema(),
              "generatedAt", stringSchema()),
          List.of(
              "matchId",
              "matchIds",
              "candidateProfileId",
              "missionId",
              "groupedMatchCount",
              "subject",
              "evidenceBrief",
              "textBody",
              "htmlBody",
              "generatedAt"));
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
    public boolean getOpenWorldHint() {
      return false;
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      long matchId = ToolReqSupportRequiredLong(req, MATCH_ID_ARGUMENT);
      MatchNotificationWebContract.MatchNotificationPreviewView preview = matchPreviewService.previewMatch(matchId);
      Map<String, Object> structuredContent = objectMapper.convertValue(
          preview,
          new TypeReference<LinkedHashMap<String, Object>>() {
          });
      return ToolResponse.ToolRespStructured(
          structuredContent,
          "Match notification preview created without sending email.");
    }
  }

}
