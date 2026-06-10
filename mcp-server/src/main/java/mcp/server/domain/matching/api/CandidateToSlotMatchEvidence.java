package mcp.server.domain.matching.api;

import java.time.Instant;
import java.util.List;

public record CandidateToSlotMatchEvidence(
        long matchId,
        long candidateProfileId,
        long missionSlotId,
        int score,
        String scoreLabel,
        boolean roleMatched,
        boolean workModeMatched,
        int matchedSkillCount,
        List<Long> matchedSkillIds,
        List<String> matchedSkillTitles,
        Instant matchedAt) {

    public CandidateToSlotMatchEvidence {
        matchedSkillIds = matchedSkillIds == null ? List.of() : List.copyOf(matchedSkillIds);
        matchedSkillTitles = matchedSkillTitles == null ? List.of() : List.copyOf(matchedSkillTitles);
    }
}
