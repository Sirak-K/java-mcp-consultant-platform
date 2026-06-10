package mcp.server.domain.candidate_profiles.api;

import java.util.List;

public record CandidateProfileAssignmentEligibility(
        long candidateProfileId,
        boolean candidateProfileFound,
        boolean assignmentAvailable,
        boolean roleMatched,
        boolean requiredSkillMatched,
        boolean eligible,
        List<String> rejectionReasons) {

    public CandidateProfileAssignmentEligibility {
        rejectionReasons = rejectionReasons == null ? List.of() : List.copyOf(rejectionReasons);
    }
}
