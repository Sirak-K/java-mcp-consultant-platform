package mcp.server.domain.match_notifications.web;

import java.util.List;

public final class MatchNotificationWebContract {

  private MatchNotificationWebContract() {
  }

  public record MatchNotificationPreviewView(
      long matchId,
      List<Long> matchIds,
      Long candidateProfileId,
      Long missionId,
      int groupedMatchCount,
      String subject,
      String evidenceBrief,
      String textBody,
      String htmlBody,
      String generatedAt) {

    public MatchNotificationPreviewView {
      matchIds = matchIds == null ? List.of() : List.copyOf(matchIds);
    }
  }

  public record MatchNotificationSendView(
      long matchId,
      List<Long> matchIds,
      Long candidateProfileId,
      Long missionId,
      int groupedMatchCount,
      String to,
      String from,
      String subject,
      String transport,
      String status,
      String transportRef,
      String sentAt,
      Long deliveryId,
      String deliveryStatus,
      String messageRfc822) {

    public MatchNotificationSendView {
      matchIds = matchIds == null ? List.of() : List.copyOf(matchIds);
    }
  }
}
