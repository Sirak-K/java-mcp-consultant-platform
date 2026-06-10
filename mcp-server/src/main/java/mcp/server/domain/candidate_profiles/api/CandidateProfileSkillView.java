package mcp.server.domain.candidate_profiles.api;

public record CandidateProfileSkillView(
        String skillCategory,
        long skillId,
        String skillTitle,
        short skillLevelId,
        String skillLevelName) {
}
