package mcp.server.domain.match_notifications.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.match_notifications.application.preview.MatchNotificationPreviewService;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolReqs;

class MatchNotificationToolSchemaTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void previewToolsExposeReadOnlyMcpSchemasWithoutLegacyMailNames() {
    MatchNotificationPreviewTool toolFactory = new MatchNotificationPreviewTool(
        mock(MatchNotificationPreviewService.class),
        OBJECT_MAPPER);

    ToolInterface inspectPreviews = toolFactory.inspectPreviewsTool();
    ToolInterface previewMatch = toolFactory.previewMatchTool();

    assertThat(inspectPreviews.getName())
        .isEqualTo(MatchNotificationPreviewTool.INSPECT_PREVIEWS_TOOL_NAME);
    assertThat(inspectPreviews.getReadOnlyHint()).isTrue();
    assertThat(inspectPreviews.getDestructiveHint()).isFalse();
    assertThat(inspectPreviews.getIdempotentHint()).isTrue();
    assertThat(inspectPreviews.getOpenWorldHint()).isFalse();
    assertThat(requiredFields(inspectPreviews.getInputSchema())).isEmpty();
    assertThat(requiredFields(inspectPreviews.getOutputSchema())).containsExactly("count", "previews");

    assertThat(previewMatch.getName())
        .isEqualTo(MatchNotificationPreviewTool.PREVIEW_MATCH_TOOL_NAME);
    assertThat(previewMatch.getReadOnlyHint()).isTrue();
    assertThat(previewMatch.getDestructiveHint()).isFalse();
    assertThat(previewMatch.getIdempotentHint()).isTrue();
    assertThat(previewMatch.getOpenWorldHint()).isFalse();
    assertThat(requiredFields(previewMatch.getInputSchema())).containsExactly("matchId");
    assertThat(requiredFields(previewMatch.getOutputSchema())).containsExactly(
        "matchId",
        "matchIds",
        "candidateProfileId",
        "missionId",
        "groupedMatchCount",
        "subject",
        "evidenceBrief",
        "textBody",
        "htmlBody",
        "generatedAt");

    assertNoLegacyMailText(inspectPreviews);
    assertNoLegacyMailText(previewMatch);
  }

  @Test
  void sendEmailToolExposesRealSideEffectMetadataAndConfirmationSchema() {
    MatchNotificationSendTool toolFactory = new MatchNotificationSendTool(
        mock(MatchNotificationPreviewService.class),
        OBJECT_MAPPER);

    ToolInterface sendEmail = toolFactory.sendEmailTool();

    assertThat(sendEmail.getName())
        .isEqualTo(MatchNotificationSendTool.SEND_EMAIL_TOOL_NAME);
    assertThat(sendEmail.getReadOnlyHint()).isFalse();
    assertThat(sendEmail.getDestructiveHint()).isTrue();
    assertThat(sendEmail.getIdempotentHint()).isFalse();
    assertThat(sendEmail.getOpenWorldHint()).isTrue();
    assertThat(requiredFields(sendEmail.getInputSchema())).containsExactly("matchId", "confirmRealSend");
    assertThat(requiredFields(sendEmail.getOutputSchema())).containsExactly(
        "matchId",
        "matchIds",
        "candidateProfileId",
        "missionId",
        "groupedMatchCount",
        "to",
        "from",
        "subject",
        "transport",
        "status",
        "transportRef",
        "sentAt",
        "deliveryId",
        "deliveryStatus",
        "messageRfc822");

    assertThatThrownBy(() -> sendEmail.execute(ToolReqs.ToolReqFromRpc(
        MatchNotificationSendTool.SEND_EMAIL_TOOL_NAME,
        OBJECT_MAPPER.createObjectNode())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("authenticated platform ops execution context");
    assertNoLegacyMailText(sendEmail);
  }

  @SuppressWarnings("unchecked")
  private static List<String> requiredFields(Map<String, Object> schema) {
    return (List<String>) schema.get("required");
  }

  private static void assertNoLegacyMailText(ToolInterface tool) {
    assertThat(tool.getName()).doesNotContainIgnoringCase("ampm");
    assertThat(tool.getTitle()).doesNotContainIgnoringCase("ampm");
    assertThat(tool.getDescription()).doesNotContainIgnoringCase("ampm");
    assertThat(json(tool.getInputSchema())).doesNotContainIgnoringCase("ampm");
    assertThat(json(tool.getOutputSchema())).doesNotContainIgnoringCase("ampm");
  }

  private static String json(Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
