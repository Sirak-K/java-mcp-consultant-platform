package mcp.server.domain.match_notifications.application.delivery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.springframework.stereotype.Component;

import mcp.server.domain.match_notifications.exception.MatchNotificationDeliveryException;

@Component
public final class MatchNotificationMockOutboxWriter {

  public String write(long matchId, Instant sentAt, String messageRfc822) {
    Path outboxDir = Path.of("target", "match-notification-outbox");
    String fileName = "match-notification-" + matchId + "-" + sentAt.toEpochMilli() + ".eml";
    Path outboxFile = outboxDir.resolve(fileName);
    try {
      Files.createDirectories(outboxDir);
      Files.writeString(outboxFile, messageRfc822, StandardCharsets.UTF_8);
    } catch (IOException excep) {
      throw MatchNotificationDeliveryException.internalError("Could not write match notification mock mail", excep);
    }
    return outboxFile.toString().replace('\\', '/');
  }
}
