package mcp.server.domain.candidate_profiles.api;

import java.util.List;

public record CandidateProfileAssignmentRequirement(
        long candidateProfileId,
        long requiredRoleId,
        List<RequiredSkill> requiredSkills) {

    public CandidateProfileAssignmentRequirement {
        if (candidateProfileId <= 0) {
            throw new IllegalArgumentException("candidateProfileId must be positive");
        }
        if (requiredRoleId <= 0) {
            throw new IllegalArgumentException("requiredRoleId must be positive");
        }
        requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
    }

    public record RequiredSkill(long skillId, short minimumSkillLevelId) {

        public RequiredSkill {
            if (skillId <= 0) {
                throw new IllegalArgumentException("skillId must be positive");
            }
            if (minimumSkillLevelId <= 0) {
                throw new IllegalArgumentException("minimumSkillLevelId must be positive");
            }
        }
    }
}
