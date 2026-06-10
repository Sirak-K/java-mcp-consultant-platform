package mcp.server.domain.matching.api;

import java.util.List;

public record MatchScoreBreakdownFactorView(
        String factor,
        boolean matched,
        int matchedCount,
        int requiredCount,
        int scorePerInstance,
        int points,
        List<String> evidence,
        String note) {

    public MatchScoreBreakdownFactorView {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
