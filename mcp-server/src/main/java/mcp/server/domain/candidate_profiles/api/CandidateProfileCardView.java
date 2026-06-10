package mcp.server.domain.candidate_profiles.api;

import java.util.List;

public record CandidateProfileCardView(
        long candidateProfileId,
        String displayName,
        String primaryRoleTitle,
        String primaryRoleExperienceLevel,
        Integer primaryRoleExperienceYears,
        String workStatus,
        String country,
        String locationFlexibility,
        String workMode,
        List<CandidateProfileRoleView> roles,
        List<CandidateProfileSkillView> primarySkills,
        List<CandidateProfileSkillView> secondarySkills) {

    public CandidateProfileCardView {
        roles = roles == null ? List.of() : List.copyOf(roles);
        primarySkills = primarySkills == null ? List.of() : List.copyOf(primarySkills);
        secondarySkills = secondarySkills == null ? List.of() : List.copyOf(secondarySkills);
    }
}
