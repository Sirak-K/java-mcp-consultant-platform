package mcp.server.domain.candidate_presentation.exception;

public final class CandidatePresentationException extends RuntimeException {

  public enum Kind {
    NOT_FOUND,
    CONFLICT,
    INVALID_REQUEST,
    INTERNAL_ERROR
  }

  private final Kind kind;

  private CandidatePresentationException(Kind kind, String message, Throwable cause) {
    super(message, cause);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }

  public static CandidatePresentationException notFound(String message) {
    return new CandidatePresentationException(Kind.NOT_FOUND, message, null);
  }

  public static CandidatePresentationException conflict(String message) {
    return new CandidatePresentationException(Kind.CONFLICT, message, null);
  }

  public static CandidatePresentationException invalidRequest(String message) {
    return new CandidatePresentationException(Kind.INVALID_REQUEST, message, null);
  }

  public static CandidatePresentationException invalidRequest(String message, Throwable cause) {
    return new CandidatePresentationException(Kind.INVALID_REQUEST, message, cause);
  }

  public static CandidatePresentationException internalError(String message) {
    return new CandidatePresentationException(Kind.INTERNAL_ERROR, message, null);
  }

  public static CandidatePresentationException internalError(String message, Throwable cause) {
    return new CandidatePresentationException(Kind.INTERNAL_ERROR, message, cause);
  }
}
