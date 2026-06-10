package mcp.server.domain.candidate_presentation.application.generation;

import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactView;

public record CandidatePresentationGenerationStartView(
    String generationStartStatus,
    long matchId,
    Long artifactId,
    String artifactStatus,
    String runId,
    String message,
    CandidatePresentationArtifactView artifact) {
}
