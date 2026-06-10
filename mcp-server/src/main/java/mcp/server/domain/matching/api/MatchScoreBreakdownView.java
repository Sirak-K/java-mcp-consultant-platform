package mcp.server.domain.matching.api;

import java.util.List;

public record MatchScoreBreakdownView(
        long matchId,
        int score,
        String scoreLabel,
        int discoveryThreshold,
        boolean passedDiscoveryThreshold,
        String decision,
        List<MatchScoreBreakdownFactorView> factors,
        List<String> matchedSkills,
        List<String> missingOrWeakFactors,
        String matchedAt) {

    public MatchScoreBreakdownView {
        factors = factors == null ? List.of() : List.copyOf(factors);
        matchedSkills = matchedSkills == null ? List.of() : List.copyOf(matchedSkills);
        missingOrWeakFactors = missingOrWeakFactors == null ? List.of() : List.copyOf(missingOrWeakFactors);
    }
}
