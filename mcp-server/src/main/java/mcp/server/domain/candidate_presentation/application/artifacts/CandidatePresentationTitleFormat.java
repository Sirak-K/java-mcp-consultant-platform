package mcp.server.domain.candidate_presentation.application.artifacts;

public final class CandidatePresentationTitleFormat {

  private CandidatePresentationTitleFormat() {
  }

  public static String forMatchId(long matchId) {
    return "Presentation för Matchnings-ID: " + matchId;
  }
}
