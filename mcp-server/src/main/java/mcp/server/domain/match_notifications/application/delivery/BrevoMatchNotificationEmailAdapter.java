package mcp.server.domain.match_notifications.application.delivery;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.match_notifications.exception.MatchNotificationDeliveryException;
import mcp.server.domain.match_notifications.web.MatchNotificationWebContract;

@Component
public final class BrevoMatchNotificationEmailAdapter {

  public record BrevoEmailConfig(
      String apiKey,
      String senderEmail,
      String senderName,
      String replyToEmail,
      String replyToName,
      boolean realSendEnabled) {
  }

  public record BrevoSendRequest(
      MatchNotificationWebContract.MatchNotificationPreviewView preview,
      String recipientEmail,
      String recipientName,
      BrevoEmailConfig config) {
  }

  public record BrevoSendResult(String messageId, String responseRef) {
  }

  private static final URI BREVO_SEND_EMAIL_URI = URI.create("https://api.brevo.com/v3/smtp/email");

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public BrevoMatchNotificationEmailAdapter(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  public BrevoEmailConfig loadConfig() {
    String apiKey = requiredEnv("MATCH_NOTIFICATION_BREVO_API_KEY");
    String senderEmail = requiredEnv("MATCH_NOTIFICATION_BREVO_SENDER_EMAIL");
    String senderName = envOrDefault("MATCH_NOTIFICATION_BREVO_SENDER_NAME", "Consultant Match Ops");
    String replyToEmail = envOrDefault("MATCH_NOTIFICATION_BREVO_REPLY_TO_EMAIL", senderEmail);
    String replyToName = envOrDefault("MATCH_NOTIFICATION_BREVO_REPLY_TO_NAME", senderName);
    validateBrevoSender(senderEmail);
    boolean realSendEnabled = "true".equalsIgnoreCase(envOrDefault("MATCH_NOTIFICATION_REAL_SEND_ENABLED", "false"));
    return new BrevoEmailConfig(apiKey, senderEmail, senderName, replyToEmail, replyToName, realSendEnabled);
  }

  public BrevoSendResult send(BrevoSendRequest request) {
    BrevoSendRequest safeRequest = Objects.requireNonNull(request, "request");
    String requestBody = buildBrevoRequestBody(safeRequest);
    HttpRequest httpRequest = HttpRequest.newBuilder(BREVO_SEND_EMAIL_URI)
        .timeout(Duration.ofSeconds(20))
        .header("accept", "application/json")
        .header("api-key", safeRequest.config().apiKey())
        .header("content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
        .build();
    try {
      HttpResponse<String> httpResponse = httpClient.send(
          httpRequest,
          HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
        throw MatchNotificationDeliveryException.badGateway(
            "Brevo send failed with status " + httpResponse.statusCode() + ": " + httpResponse.body());
      }
      return new BrevoSendResult(brevoMessageId(httpResponse.body()), compactText(httpResponse.body(), 1000));
    } catch (IOException excep) {
      throw MatchNotificationDeliveryException.badGateway("Brevo send failed", excep);
    } catch (InterruptedException excep) {
      Thread.currentThread().interrupt();
      throw MatchNotificationDeliveryException.badGateway("Brevo send interrupted", excep);
    }
  }

  private String buildBrevoRequestBody(BrevoSendRequest request) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sender", Map.of(
        "name", request.config().senderName(),
        "email", request.config().senderEmail()));
    payload.put("to", List.of(Map.of(
        "email", request.recipientEmail(),
        "name", request.recipientName())));
    payload.put("replyTo", Map.of(
        "email", request.config().replyToEmail(),
        "name", request.config().replyToName()));
    payload.put("subject", request.preview().subject());
    payload.put("htmlContent", request.preview().htmlBody());
    payload.put("textContent", request.preview().textBody());
    payload.put("tags", List.of("match-notification", "candidate-to-slot-match"));
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException excep) {
      throw MatchNotificationDeliveryException.internalError("Could not build Brevo mail payload", excep);
    }
  }

  private String brevoMessageId(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode messageId = root.get("messageId");
      if (messageId != null && messageId.isTextual() && !messageId.asText().isBlank()) {
        return messageId.asText();
      }
    } catch (JsonProcessingException ignored) {
      // The Brevo body is only used as a transport reference when no messageId exists.
    }
    return textOrFallback(responseBody, "BREVO_ACCEPTED");
  }

  private static String requiredEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.trim().isBlank()) {
      throw MatchNotificationDeliveryException.preconditionFailed("Missing required environment variable: " + name);
    }
    return value.trim();
  }

  private static void validateBrevoSender(String senderEmail) {
    String normalized = textOrFallback(senderEmail, "").toLowerCase();
    if (!normalized.contains("@")
        || normalized.endsWith("@local.test")
        || normalized.endsWith("@example.test")
        || normalized.endsWith("@test")) {
      throw MatchNotificationDeliveryException.preconditionFailed("Brevo sender email is not a verified real sender");
    }
  }

  private static String envOrDefault(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.trim().isBlank() ? fallback : value.trim();
  }

  private static String compactText(String value, int maxLength) {
    String compacted = textOrFallback(value, "")
        .replace("\r", " ")
        .replace("\n", " ")
        .replaceAll("\\s+", " ")
        .trim();
    return compacted.length() <= maxLength ? compacted : compacted.substring(0, maxLength);
  }

  private static String textOrFallback(String value, String fallback) {
    return value == null || value.trim().isBlank() ? fallback : value.trim();
  }
}
