package mcp.server.domain.match_notifications.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.match_notifications.application.preview.MatchNotificationPreviewService;
import mcp.server.domain.match_notifications.model.MatchNotificationSendSource;
import mcp.server.domain.match_notifications.web.MatchNotificationWebContract;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.tool_interface.ToolExecCtx;
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
public final class MatchNotificationSendTool {

  public static final String SEND_EMAIL_TOOL_NAME = "matchNotifications.sendEmail";
  private static final String MATCH_ID_ARGUMENT = "matchId";
  private static final String CONFIRM_REAL_SEND_ARGUMENT = "confirmRealSend";

  private final MatchNotificationPreviewService matchPreviewService;
  private final ObjectMapper objectMapper;

  public MatchNotificationSendTool(
      MatchNotificationPreviewService matchPreviewService,
      ObjectMapper objectMapper) {
    this.matchPreviewService = Objects.requireNonNull(matchPreviewService, "matchPreviewService");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public ToolInterface sendEmailTool() {
    return new SendEmailImpl();
  }

  private final class SendEmailImpl implements ToolInterface {

    @Override
    public String getName() {
      return SEND_EMAIL_TOOL_NAME;
    }

    @Override
    public String getDescription() {
      return "Send one match notification email for a persisted candidate-to-slot match. "
          + "This capability performs a real external side effect and requires platform-ops context, "
          + "explicit confirmation, real-send environment gates and existing delivery audit.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(
          Map.of(
              MATCH_ID_ARGUMENT,
              integerSchema("candidate_to_slot_match identifier to send"),
              CONFIRM_REAL_SEND_ARGUMENT,
              booleanSchema("must be true to confirm a real email send")),
          List.of(MATCH_ID_ARGUMENT, CONFIRM_REAL_SEND_ARGUMENT));
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return closedObjectSchema(
          outputProperties(),
          List.of(
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
              "messageRfc822"));
    }

    @Override
    public boolean getReadOnlyHint() {
      return false;
    }

    @Override
    public boolean getDestructiveHint() {
      return true;
    }

    @Override
    public boolean getIdempotentHint() {
      return false;
    }

    @Override
    public boolean getOpenWorldHint() {
      return true;
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      throw new IllegalStateException("Match notification email send requires authenticated platform ops execution context.");
    }

    @Override
    public ToolResponse execute(ToolReqs req, ToolExecCtx context) {
      requirePlatformOpsContext(context);
      requireRealSendConfirmation(req);
      long matchId = ToolReqSupportRequiredLong(req, MATCH_ID_ARGUMENT);
      MatchNotificationWebContract.MatchNotificationSendView sendView = matchPreviewService.sendMatchNotificationEmail(
          matchId,
          MatchNotificationSendSource.PLATFORM_OPS_TOOL);
      Map<String, Object> structuredContent = objectMapper.convertValue(
          sendView,
          new TypeReference<LinkedHashMap<String, Object>>() {
          });
      return ToolResponse.ToolRespStructured(
          structuredContent,
          "Match notification email sent with delivery audit.");
    }
  }

  private void requirePlatformOpsContext(ToolExecCtx context) {
    if (context == null) {
      throw new IllegalStateException("Match notification email send requires platform ops execution context.");
    }
    context.ToolExecCtxThrowIfCancelled();
    ReqsAuthBinding authBinding = context.ToolExecCtxGetReqsAuthBinding();
    if (context.ToolExecCtxGetOperatingSurface() != OperatingSurface.PLATFORM_OPS
        || authBinding == null
        || !authBinding.ReqsAuthBindingIsPlatformBound()
        || authBinding.principal() == null
        || !authBinding.principal().ReqsPrincipalIsPlatformOps()) {
      throw new IllegalStateException("Match notification email send requires platform ops request binding.");
    }
  }

  private void requireRealSendConfirmation(ToolReqs req) {
    boolean confirmed = req.ToolReqParam(CONFIRM_REAL_SEND_ARGUMENT).ToolParamAsBoolean(false);
    if (!confirmed) {
      throw new IllegalArgumentException("confirmRealSend must be true for match notification email send.");
    }
  }

  private Map<String, Object> outputProperties() {
    LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
    properties.put("matchId", integerSchema());
    properties.put("matchIds", arraySchema(integerSchema()));
    properties.put("candidateProfileId", integerSchema());
    properties.put("missionId", integerSchema());
    properties.put("groupedMatchCount", integerSchema());
    properties.put("to", stringSchema());
    properties.put("from", stringSchema());
    properties.put("subject", stringSchema());
    properties.put("transport", stringSchema());
    properties.put("status", stringSchema());
    properties.put("transportRef", stringSchema());
    properties.put("sentAt", stringSchema());
    properties.put("deliveryId", integerSchema());
    properties.put("deliveryStatus", stringSchema());
    properties.put("messageRfc822", stringSchema());
    return Map.copyOf(properties);
  }
}
