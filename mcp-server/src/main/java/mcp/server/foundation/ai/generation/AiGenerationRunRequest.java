package mcp.server.foundation.ai.generation;

public record AiGenerationRunRequest(
    String generationInputId,
    String generationArtifactId) {
}
