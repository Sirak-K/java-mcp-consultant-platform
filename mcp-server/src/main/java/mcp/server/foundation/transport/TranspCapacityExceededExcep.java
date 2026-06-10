package mcp.server.foundation.transport;

public final class TranspCapacityExceededExcep extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TranspCapacityExceededExcep(String message) {
    super(message);
  }
}
