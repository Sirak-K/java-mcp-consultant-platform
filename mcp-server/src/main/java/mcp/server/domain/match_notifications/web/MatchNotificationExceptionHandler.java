package mcp.server.domain.match_notifications.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import mcp.server.domain.match_notifications.exception.MatchNotificationDeliveryException;
import mcp.server.domain.match_notifications.exception.MatchNotificationNotFoundException;

@RestControllerAdvice(basePackages = "mcp.server.domain.match_notifications")
public final class MatchNotificationExceptionHandler {

  @ExceptionHandler(MatchNotificationNotFoundException.class)
  ProblemDetail handleNotFound(MatchNotificationNotFoundException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(MatchNotificationDeliveryException.class)
  ProblemDetail handleDeliveryFailure(MatchNotificationDeliveryException exception) {
    return ProblemDetail.forStatusAndDetail(statusFor(exception.reason()), exception.getMessage());
  }

  private static HttpStatus statusFor(MatchNotificationDeliveryException.Reason reason) {
    return switch (reason) {
      case PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED;
      case CONFLICT -> HttpStatus.CONFLICT;
      case BAD_GATEWAY -> HttpStatus.BAD_GATEWAY;
      case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
