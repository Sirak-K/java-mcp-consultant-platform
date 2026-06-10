package mcp.server.domain.match_notifications.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import mcp.server.domain.match_notifications.application.preview.MatchNotificationPreviewService;
import mcp.server.domain.match_notifications.model.MatchNotificationSendSource;

import java.util.List;
import java.util.Objects;

@RestController
public final class MatchNotificationController {

  private static final String BASE_PATH = "/api/ops/match-notifications";

  private final MatchNotificationPreviewService matchPreviewService;

  public MatchNotificationController(MatchNotificationPreviewService matchPreviewService) {
    this.matchPreviewService = Objects.requireNonNull(matchPreviewService, "matchPreviewService");
  }

  @GetMapping(BASE_PATH + "/matches/{matchId}/preview")
  public MatchNotificationWebContract.MatchNotificationPreviewView previewMatch(
      @PathVariable("matchId") long matchId) {
    return matchPreviewService.previewMatch(matchId);
  }

  @GetMapping(BASE_PATH + "/previews")
  public List<MatchNotificationWebContract.MatchNotificationPreviewView> previewMatches() {
    return matchPreviewService.previewMatches();
  }

  @PostMapping(BASE_PATH + "/matches/{matchId}/mock-send")
  public MatchNotificationWebContract.MatchNotificationSendView sendMockMatchNotificationEmail(
      @PathVariable("matchId") long matchId) {
    return matchPreviewService.sendMatchNotificationMockEmail(matchId);
  }

  @PostMapping(BASE_PATH + "/matches/{matchId}/email-send")
  public MatchNotificationWebContract.MatchNotificationSendView sendMatchNotificationEmail(
      @PathVariable("matchId") long matchId) {
    return matchPreviewService.sendMatchNotificationEmail(matchId, MatchNotificationSendSource.OPS_REST);
  }
}
