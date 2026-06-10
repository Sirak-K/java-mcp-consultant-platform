package mcp.server.domain.candidate_presentation.web;

public record CandidatePresentationArtifactEditRequest(
    String customerFacingContentJson,
    String opsReviewContentJson,
    String evidenceTraceJson) {
}
