package mcp.server.foundation.spring_integration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.Objects;

import mcp.server.domain.shared_kernel.exception.DomainInvariantViolationException;
import mcp.server.domain.shared_kernel.validation.InvalidApplicationInputException;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtxFactory;

@RestControllerAdvice(basePackages = {
    "mcp.server.domain",
    "mcp.server.foundation.logging",
    "mcp.server.foundation.security"
})
public final class SpringApiExceptionHandler {

  private final ServerLogger serverLogger;
  private final ObservCtxFactory obsCtxFactory;

  public SpringApiExceptionHandler(
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory) {

    this.serverLogger = Objects.requireNonNull(serverLogger, "serverLogger");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorMessage> handleBadCredentials(BadCredentialsException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorMessage("Invalid email or password"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorMessage> handleAccessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorMessage("Access denied"));
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorMessage> handleNotFound(NoSuchElementException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorMessage(exception.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorMessage> handleBadRequest(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorMessage(exception.getMessage()));
  }

  @ExceptionHandler(InvalidApplicationInputException.class)
  public ResponseEntity<ErrorMessage> handleInvalidApplicationInput(
      InvalidApplicationInputException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorMessage(exception.getMessage()));
  }

  @ExceptionHandler(DomainInvariantViolationException.class)
  public ResponseEntity<ErrorMessage> handleDomainInvariantViolation(
      DomainInvariantViolationException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorMessage(exception.getMessage()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorMessage> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
    return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
        .body(new ErrorMessage("Uploaded file or multipart request is too large. Reduce the PDF size and try again."));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorMessage> handleConflict(IllegalStateException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorMessage(exception.getMessage()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorMessage> handleResponseStatus(ResponseStatusException exception) {
    HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return ResponseEntity.status(status)
        .body(new ErrorMessage(exception.getReason()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorMessage> handleFallback(Exception exception) {
    serverLogger.ServerLogErrorObserved(
        ServerLogger.Component.RUNTIME,
        obsCtxFactory.ObservCtxFactoryCurrentOrEmpty(),
        "HANDLE",
        "SPRING_API_UNEXPECTED_EXCEPTION",
        "Unhandled Spring API exception: " + exception.getClass().getSimpleName(),
        exception.getClass().getSimpleName(),
        exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorMessage("Internal server error"));
  }
}

record ErrorMessage(String message) {
}
