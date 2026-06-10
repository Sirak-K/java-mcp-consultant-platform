package mcp.server.domain.match_notifications.application.delivery;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import org.springframework.stereotype.Component;

import mcp.server.domain.match_notifications.web.MatchNotificationWebContract;

@Component
public final class MatchNotificationMimeMessageBuilder {

  private static final DateTimeFormatter MAIL_DATE_FORMAT = new DateTimeFormatterBuilder()
      .appendPattern("EEE, dd MMM yyyy HH:mm:ss Z")
      .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
      .toFormatter(java.util.Locale.ENGLISH);

  public String build(
      MatchNotificationWebContract.MatchNotificationPreviewView preview,
      Instant sentAt,
      String sender,
      String recipient,
      String replyTo) {
    String boundary = "match-notification-" + preview.matchId() + "-" + sentAt.toEpochMilli();
    String mailDate = MAIL_DATE_FORMAT.format(ZonedDateTime.ofInstant(sentAt, ZoneId.systemDefault()));
    return String.join("\r\n",
        "From: " + sanitizeHeader(sender),
        "To: " + sanitizeHeader(recipient),
        "Reply-To: " + sanitizeHeader(replyTo),
        "Subject: " + sanitizeHeader(preview.subject()),
        "Date: " + mailDate,
        "MIME-Version: 1.0",
        "Content-Type: multipart/alternative; boundary=\"" + boundary + "\"",
        "",
        "--" + boundary,
        "Content-Type: text/plain; charset=UTF-8",
        "Content-Transfer-Encoding: 8bit",
        "",
        preview.textBody(),
        "",
        "--" + boundary,
        "Content-Type: text/html; charset=UTF-8",
        "Content-Transfer-Encoding: 8bit",
        "",
        preview.htmlBody(),
        "",
        "--" + boundary + "--",
        "");
  }

  private static String sanitizeHeader(String value) {
    return textOrFallback(value, "")
        .replace("\r", " ")
        .replace("\n", " ");
  }

  private static String textOrFallback(String value, String fallback) {
    return value == null || value.trim().isBlank() ? fallback : value.trim();
  }
}
