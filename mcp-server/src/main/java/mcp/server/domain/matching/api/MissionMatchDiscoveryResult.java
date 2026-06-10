package mcp.server.domain.matching.api;

import java.util.List;

public record MissionMatchDiscoveryResult(
        long missionSlotId,
        String missionTitle,
        int missionSlotNumber,
        String roleTitle,
        int requiredRoleExperienceYears,
        int score,
        String scoreLabel,
        boolean roleMatched,
        boolean workModeMatched,
        int matchedSkillCount,
        int requiredSkillCount,
        List<String> matchedSkills,
        String readiness) {

    public MissionMatchDiscoveryResult {
        matchedSkills = matchedSkills == null ? List.of() : List.copyOf(matchedSkills);
    }
}
