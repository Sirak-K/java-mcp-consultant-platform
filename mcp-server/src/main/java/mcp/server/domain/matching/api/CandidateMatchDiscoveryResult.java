package mcp.server.domain.matching.api;

import java.util.List;

public record CandidateMatchDiscoveryResult(
        int missionSlotNumber,
        String roleTitle,
        long candidateProfileId,
        String candidateName,
        int score,
        String scoreLabel,
        boolean roleMatched,
        boolean workModeMatched,
        int matchedSkillCount,
        int requiredSkillCount,
        List<String> matchedSkills) {

    public CandidateMatchDiscoveryResult {
        matchedSkills = matchedSkills == null ? List.of() : List.copyOf(matchedSkills);
    }
}
