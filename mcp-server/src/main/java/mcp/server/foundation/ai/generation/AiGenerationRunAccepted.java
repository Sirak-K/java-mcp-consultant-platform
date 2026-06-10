package mcp.server.foundation.ai.generation;

public record AiGenerationRunAccepted(
    String runId,
    String runState,
    String generationInputId,
    String generationArtifactId,
    Boolean backgroundStarted) {
}
