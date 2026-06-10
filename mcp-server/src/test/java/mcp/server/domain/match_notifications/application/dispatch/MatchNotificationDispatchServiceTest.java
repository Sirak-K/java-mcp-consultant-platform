package mcp.server.domain.match_notifications.application.dispatch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import mcp.server.domain.match_notifications.application.preview.MatchNotificationPreviewService;
import mcp.server.domain.match_notifications.model.MatchNotificationSendSource;

class MatchNotificationDispatchServiceTest {

  private final MatchNotificationPreviewService previewService = mock(MatchNotificationPreviewService.class);
  private final MatchNotificationDispatchService dispatchService =
      new MatchNotificationDispatchService(previewService);

  @Test
  void dispatchRecordedMatchesFiltersInvalidIdsAndSendsEachMatchOnceAsAutoSend() {
    dispatchService.dispatchRecordedMatches(Arrays.asList(null, -1L, 0L, 42L, 42L, 43L));

    verify(previewService).sendMatchNotificationEmail(42L, MatchNotificationSendSource.AUTO_SEND);
    verify(previewService).sendMatchNotificationEmail(43L, MatchNotificationSendSource.AUTO_SEND);
    verify(previewService, never()).sendMatchNotificationEmail(0L, MatchNotificationSendSource.AUTO_SEND);
    verify(previewService, never()).sendMatchNotificationEmail(-1L, MatchNotificationSendSource.AUTO_SEND);
  }

  @Test
  void dispatchRecordedMatchesContinuesAfterSingleSendFailure() {
    when(previewService.sendMatchNotificationEmail(42L, MatchNotificationSendSource.AUTO_SEND))
        .thenThrow(new IllegalStateException("send failed"));

    dispatchService.dispatchRecordedMatches(List.of(42L, 43L));

    verify(previewService).sendMatchNotificationEmail(42L, MatchNotificationSendSource.AUTO_SEND);
    verify(previewService).sendMatchNotificationEmail(43L, MatchNotificationSendSource.AUTO_SEND);
  }
}
