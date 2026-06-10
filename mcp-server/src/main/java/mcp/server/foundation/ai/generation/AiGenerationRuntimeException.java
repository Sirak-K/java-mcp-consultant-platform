package mcp.server.foundation.ai.generation;

public final class AiGenerationRuntimeException extends RuntimeException {

  public AiGenerationRuntimeException(String message) {
    super(message);
  }

  public AiGenerationRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
