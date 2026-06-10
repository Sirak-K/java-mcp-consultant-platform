package mcp.server.domain.candidate_presentation.api;

public interface CandidatePresentationArtifactCleanup {

  int deleteArtifactsForCandidateProfile(long candidateProfileId);
}
