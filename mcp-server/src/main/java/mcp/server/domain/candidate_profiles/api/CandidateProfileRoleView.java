package mcp.server.domain.candidate_profiles.api;

public record CandidateProfileRoleView(
        int roleOrder,
        long roleId,
        String roleTitle,
        int roleExperienceYears,
        short competencyLevelId) {
}
