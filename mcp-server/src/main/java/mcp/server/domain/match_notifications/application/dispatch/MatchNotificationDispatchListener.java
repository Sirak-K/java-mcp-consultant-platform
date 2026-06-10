package mcp.server.domain.match_notifications.application.dispatch;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import mcp.server.domain.matching.api.CandidateToSlotMatchesRecorded;

@Component
public class MatchNotificationDispatchListener {

  private final MatchNotificationDispatchService dispatchService;

  public MatchNotificationDispatchListener(MatchNotificationDispatchService dispatchService) {
    this.dispatchService = dispatchService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onCandidateToSlotMatchesRecorded(CandidateToSlotMatchesRecorded event) {
    if (event != null && event.hasMatches()) {
      dispatchService.dispatchRecordedMatches(event.matchIds());
    }
  }
}
