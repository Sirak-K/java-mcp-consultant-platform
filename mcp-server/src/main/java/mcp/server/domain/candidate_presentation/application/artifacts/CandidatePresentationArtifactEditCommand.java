package mcp.server.domain.candidate_presentation.application.artifacts;

public record CandidatePresentationArtifactEditCommand(
    String customerFacingContentJson,
    String opsReviewContentJson,
    String evidenceTraceJson) {
}
