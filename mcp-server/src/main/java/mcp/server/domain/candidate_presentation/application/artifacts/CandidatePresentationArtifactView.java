package mcp.server.domain.candidate_presentation.application.artifacts;

public record CandidatePresentationArtifactView(
    long id,
    long sourceCandidateToSlotMatchId,
    long candProfileId,
    long missionId,
    long missionSlotId,
    String artifactStatus,
    String presentationTitle,
    String customerFacingContentJson,
    String opsReviewContentJson,
    String evidenceTraceJson,
    String createdAt,
    String updatedAt) {
}
