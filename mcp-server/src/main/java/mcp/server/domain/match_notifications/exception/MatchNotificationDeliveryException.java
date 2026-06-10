package mcp.server.domain.match_notifications.exception;

public final class MatchNotificationDeliveryException extends MatchNotificationException {

  public enum Reason {
    PRECONDITION_FAILED,
    CONFLICT,
    BAD_GATEWAY,
    INTERNAL_ERROR
  }

  private final Reason reason;

  public MatchNotificationDeliveryException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public MatchNotificationDeliveryException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }

  public static MatchNotificationDeliveryException preconditionFailed(String message) {
    return new MatchNotificationDeliveryException(Reason.PRECONDITION_FAILED, message);
  }

  public static MatchNotificationDeliveryException conflict(String message) {
    return new MatchNotificationDeliveryException(Reason.CONFLICT, message);
  }

  public static MatchNotificationDeliveryException conflict(String message, Throwable cause) {
    return new MatchNotificationDeliveryException(Reason.CONFLICT, message, cause);
  }

  public static MatchNotificationDeliveryException badGateway(String message) {
    return new MatchNotificationDeliveryException(Reason.BAD_GATEWAY, message);
  }

  public static MatchNotificationDeliveryException badGateway(String message, Throwable cause) {
    return new MatchNotificationDeliveryException(Reason.BAD_GATEWAY, message, cause);
  }

  public static MatchNotificationDeliveryException internalError(String message, Throwable cause) {
    return new MatchNotificationDeliveryException(Reason.INTERNAL_ERROR, message, cause);
  }
}
