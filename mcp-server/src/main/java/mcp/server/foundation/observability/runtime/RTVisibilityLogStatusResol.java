package mcp.server.foundation.observability.runtime;

import mcp.server.foundation.logging.CanonicalLogPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class RTVisibilityLogStatusResol {

  private static final String CANONICAL_SERVER_LOG = "logs/mcp-server.log";
  private static final String CANONICAL_ERROR_LOG = "logs/mcp-server-errors.log";
  private static final String CANONICAL_AUDIT_LOG = "logs/mcp-server-audit.log";
  private static final String CANONICAL_TEST_LOG = "logs/mcp-server.log";

  private final CanonicalLogPaths canonicalLogPaths;
  private final boolean fileSinkEnabled;
  private final String serverLogPathConfig;
  private final String errorLogPathConfig;
  private final boolean fileSinkRequired;
  private final boolean auditSinkEnabled;
  private final String auditLogPathConfig;
  private final boolean auditSinkRequired;
  private final boolean testSinkEnabled;
  private final String testLogPathConfig;
  private final boolean testSinkRequired;

  RTVisibilityLogStatusResol(
      CanonicalLogPaths canonicalLogPaths,
      boolean fileSinkEnabled,
      String serverLogPathConfig,
      String errorLogPathConfig,
      boolean auditSinkEnabled,
      String auditLogPathConfig,
      boolean testSinkEnabled,
      String testLogPathConfig,
      boolean fileSinkRequired,
      boolean auditSinkRequired,
      boolean testSinkRequired) {

    this.canonicalLogPaths = Objects.requireNonNull(canonicalLogPaths, "canonicalLogPaths");
    this.fileSinkEnabled = fileSinkEnabled;
    this.serverLogPathConfig = Objects.requireNonNull(serverLogPathConfig, "serverLogPathConfig");
    this.errorLogPathConfig = Objects.requireNonNull(errorLogPathConfig, "errorLogPathConfig");
    this.fileSinkRequired = fileSinkRequired;
    this.auditSinkEnabled = auditSinkEnabled;
    this.auditLogPathConfig = Objects.requireNonNull(auditLogPathConfig, "auditLogPathConfig");
    this.auditSinkRequired = auditSinkRequired;
    this.testSinkEnabled = testSinkEnabled;
    this.testLogPathConfig = Objects.requireNonNull(testLogPathConfig, "testLogPathConfig");
    this.testSinkRequired = testSinkRequired;
  }

  LogStatusView resolve() {

    Path logsDirectory = canonicalLogPaths.CanonicalLogPathsGetLogsDirectory();
    Path serverLog = canonicalLogPaths.CanonicalLogPathsResolve(serverLogPathConfig, CANONICAL_SERVER_LOG);
    Path errorLog = canonicalLogPaths.CanonicalLogPathsResolve(errorLogPathConfig, CANONICAL_ERROR_LOG);
    Path auditLog = canonicalLogPaths.CanonicalLogPathsResolve(auditLogPathConfig, CANONICAL_AUDIT_LOG);
    Path testLog = canonicalLogPaths.CanonicalLogPathsResolve(testLogPathConfig, CANONICAL_TEST_LOG);

    return new LogStatusView(
        Files.isDirectory(logsDirectory),
        fileSinkEnabled,
        auditSinkEnabled,
        testSinkEnabled,
        fileSinkEnabled && Files.exists(serverLog),
        fileSinkEnabled && Files.exists(errorLog),
        auditSinkEnabled && Files.exists(auditLog),
        testSinkEnabled && Files.exists(testLog),
        fileSinkRequired,
        fileSinkRequired,
        auditSinkRequired,
        testSinkRequired);
  }

  String loggingDestinationDetail(LogStatusView logStatus) {
    if (!logStatus.LogStatusViewIsAnyFileSinkEnabled()) {
      return "Ingen filbaserad logging ar aktiverad i aktuell profil";
    }
    return "Canonical loggmapp redo=" + logStatus.directoryExists()
        + ", fileSinkEnabled=" + logStatus.fileSinkEnabled()
        + ", auditSinkEnabled=" + logStatus.auditSinkEnabled()
        + ", testSinkEnabled=" + logStatus.testSinkEnabled();
  }

  String operationalLogsDetail(LogStatusView logStatus) {
    if (logStatus.testSinkEnabled()) {
      return CANONICAL_TEST_LOG + "=" + logStatus.testLogExists()
          + ", required=" + logStatus.testLogRequired();
    }
    if (!logStatus.fileSinkEnabled()) {
      return "file sink ar avstangd i aktuell profil";
    }
    return CANONICAL_SERVER_LOG + "=" + logStatus.serverLogExists() + ", "
        + CANONICAL_ERROR_LOG + "=" + logStatus.errorLogExists()
        + ", required=" + logStatus.serverLogRequired();
  }

  String auditLogDetail(LogStatusView logStatus) {
    if (logStatus.testSinkEnabled()) {
      return CANONICAL_TEST_LOG + "=" + logStatus.testLogExists()
          + ", required=" + logStatus.testLogRequired();
    }
    if (!logStatus.auditSinkEnabled()) {
      return "audit sink ar avstangd i aktuell profil";
    }
    return CANONICAL_AUDIT_LOG + "=" + logStatus.auditLogExists()
        + ", required=" + logStatus.auditLogRequired();
  }
}
