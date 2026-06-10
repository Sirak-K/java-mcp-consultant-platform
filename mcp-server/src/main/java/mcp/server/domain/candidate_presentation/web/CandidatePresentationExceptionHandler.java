package mcp.server.domain.candidate_presentation.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import mcp.server.domain.candidate_presentation.exception.CandidatePresentationException;

@RestControllerAdvice(assignableTypes = CandidatePresentationArtifactController.class)
public final class CandidatePresentationExceptionHandler {

  @ExceptionHandler(CandidatePresentationException.class)
  public ResponseEntity<Map<String, String>> handle(CandidatePresentationException exception) {
    return ResponseEntity.status(httpStatus(exception.kind()))
        .body(Map.of(
            "error", exception.kind().name(),
            "message", safeMessage(exception)));
  }

  private static HttpStatus httpStatus(CandidatePresentationException.Kind kind) {
    return switch (kind) {
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
      case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  private static String safeMessage(CandidatePresentationException exception) {
    if (exception.getMessage() == null || exception.getMessage().isBlank()) {
      return "Candidate Presentation request failed.";
    }
    return exception.getMessage();
  }
}
