package mcp.server.domain.match_notifications.application.dispatch;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import mcp.server.domain.match_notifications.application.preview.MatchNotificationPreviewService;
import mcp.server.domain.match_notifications.model.MatchNotificationSendSource;

@Service
public class MatchNotificationDispatchService {

  private static final Logger log = LoggerFactory.getLogger(MatchNotificationDispatchService.class);

  private final MatchNotificationPreviewService matchPreviewService;

  public MatchNotificationDispatchService(MatchNotificationPreviewService matchPreviewService) {
    this.matchPreviewService = matchPreviewService;
  }

  public void dispatchRecordedMatches(Collection<Long> matchIds) {
    List<Long> dispatchableMatchIds = matchIds == null
        ? List.of()
        : matchIds.stream()
            .filter(matchId -> matchId != null && matchId > 0)
            .distinct()
            .toList();
    dispatchableMatchIds.forEach(this::sendSafely);
  }

  private void sendSafely(Long matchId) {
    try {
      matchPreviewService.sendMatchNotificationEmail(matchId, MatchNotificationSendSource.AUTO_SEND);
    } catch (RuntimeException exception) {
      log.warn("Match notification auto-send skipped or failed for matchId={}: {}", matchId, exception.getMessage());
    }
  }
}
