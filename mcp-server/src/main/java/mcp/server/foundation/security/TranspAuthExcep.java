package mcp.server.foundation.security;

public final class TranspAuthExcep extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final TranspAuthFailureReason reason;

  public TranspAuthExcep(String message, TranspAuthFailureReason reason) {
    super(message);
    this.reason = reason;
  }

  public TranspAuthFailureReason TransAuthExcepGetReason() {
    return reason;
  }
}
