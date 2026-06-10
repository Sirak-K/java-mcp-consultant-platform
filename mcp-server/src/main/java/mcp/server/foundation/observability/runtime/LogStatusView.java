package mcp.server.foundation.observability.runtime;

/**
 * Log file visibility for operational runtime status.
 */
public record LogStatusView(
    boolean directoryExists,
    boolean fileSinkEnabled,
    boolean auditSinkEnabled,
    boolean testSinkEnabled,
    boolean serverLogExists,
    boolean errorLogExists,
    boolean auditLogExists,
    boolean testLogExists,
    boolean serverLogRequired,
    boolean errorLogRequired,
    boolean auditLogRequired,
    boolean testLogRequired) {

  public boolean LogStatusViewIsAnyFileSinkEnabled() {
    return fileSinkEnabled || auditSinkEnabled || testSinkEnabled;
  }

  public boolean LogStatusViewIsLoggingDestinationReady() {
    return !LogStatusViewIsAnyFileSinkEnabled() || directoryExists;
  }

  public boolean LogStatusViewHasOperLogsReady() {
    boolean canonicalOperationalReady = !fileSinkEnabled || (serverLogExists && errorLogExists);
    boolean testLoggingReady = !testLogRequired || testLogExists;
    return canonicalOperationalReady && testLoggingReady;
  }

  public boolean LogStatusViewHasRequiredLogsReady() {
    boolean operationalReady = !serverLogRequired || (serverLogExists && errorLogExists);
    boolean auditReady = !auditLogRequired || auditLogExists;
    boolean testReady = !testLogRequired || testLogExists;
    return LogStatusViewIsLoggingDestinationReady() && operationalReady && auditReady && testReady;
  }
}
