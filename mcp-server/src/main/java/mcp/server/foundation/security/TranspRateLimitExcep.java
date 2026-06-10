package mcp.server.foundation.security;

public final class TranspRateLimitExcep extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TranspRateLimitExcep(String message) {
    super(message);
  }
}
