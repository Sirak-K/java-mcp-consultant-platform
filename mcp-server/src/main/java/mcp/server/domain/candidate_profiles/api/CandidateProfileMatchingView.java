package mcp.server.domain.candidate_profiles.api;

import java.util.List;

public record CandidateProfileMatchingView(
        long candidateProfileId,
        String displayName,
        String firstName,
        String lastName,
        String contactEmail,
        String workStatus,
        String workMode,
        List<CandidateProfileRoleView> roles,
        List<CandidateProfileSkillView> skills) {

    public CandidateProfileMatchingView {
        roles = roles == null ? List.of() : List.copyOf(roles);
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}
