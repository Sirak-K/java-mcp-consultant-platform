package mcp.server.domain.candidate_presentation.application.artifacts;

public record CandidatePresentationGeneratedContentCommand(
    long artifactId,
    String presentationTitle,
    String customerFacingContentJson,
    String opsReviewContentJson,
    String evidenceTraceJson) {
}
