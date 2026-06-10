package mcp.server.domain.candidate_presentation.application.artifacts;

public record CandidatePresentationGenerationFailureCommand(
    long artifactId,
    String failureMessage,
    String failureDetail,
    String failureStage,
    String runId,
    String modelAlias) {
}
