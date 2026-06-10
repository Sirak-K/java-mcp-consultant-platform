package mcp.server.domain.matching.api;

import java.util.List;

public record CandidateToSlotMatchesRecorded(List<Long> matchIds) {

  public CandidateToSlotMatchesRecorded {
    matchIds = matchIds == null
        ? List.of()
        : matchIds.stream()
            .filter(matchId -> matchId != null && matchId > 0)
            .distinct()
            .toList();
  }

  public boolean hasMatches() {
    return !matchIds.isEmpty();
  }
}
