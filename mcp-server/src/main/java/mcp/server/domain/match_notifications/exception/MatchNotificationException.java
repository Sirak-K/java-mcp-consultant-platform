package mcp.server.domain.match_notifications.exception;

public class MatchNotificationException extends RuntimeException {

  public MatchNotificationException(String message) {
    super(message);
  }

  public MatchNotificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
