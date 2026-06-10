package mcp.server.domain.matching.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import mcp.server.domain.matching.application.viewer.MatchScoreBreakdownQueryService;
import mcp.server.domain.matching.application.viewer.MatchViewerQueryService;
import mcp.server.domain.matching.api.MatchScoreBreakdownView;
import mcp.server.domain.matching.api.MatchViewerReadModel;

import java.util.Objects;

@RestController
public final class MatchViewerController {

  private final MatchViewerQueryService matchViewerService;
  private final MatchScoreBreakdownQueryService scoreBreakdownService;

  public MatchViewerController(
      MatchViewerQueryService matchViewerService,
      MatchScoreBreakdownQueryService scoreBreakdownService) {
    this.matchViewerService = Objects.requireNonNull(matchViewerService, "matchViewerService");
    this.scoreBreakdownService = Objects.requireNonNull(scoreBreakdownService, "scoreBreakdownService");
  }

  @GetMapping("/api/ops/match-viewer")
  public MatchViewerReadModel.MatchViewerView matchViewer() {
    return matchViewerService.matchViewer();
  }

  @GetMapping("/api/ops/matches/{matchId}/score-breakdown")
  public MatchScoreBreakdownView inspectMatchScoreBreakdown(
      @PathVariable("matchId") long matchId) {
    return scoreBreakdownService.inspectMatchScoreBreakdown(matchId);
  }
}
