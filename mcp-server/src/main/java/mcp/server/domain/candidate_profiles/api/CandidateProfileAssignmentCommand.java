package mcp.server.domain.candidate_profiles.api;

public interface CandidateProfileAssignmentCommand {

    void markUnavailableWhenAssignmentLimitExceeded(long candidateProfileId, int activeAssignmentCount);
}
