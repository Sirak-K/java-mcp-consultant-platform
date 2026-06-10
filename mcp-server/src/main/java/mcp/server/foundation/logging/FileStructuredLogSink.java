package mcp.server.foundation.logging;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Optional dual-write file sink for structured logs.
 */
public final class FileStructuredLogSink implements StructuredLogSink {

  private static final String ERROR = "ERROR";
  private static final String AUDIT = "AUDIT";

  private final Path allLogPath;
  private final Path errorLogPath;
  private final StructuredLogFormatter allFormatter;
  private final StructuredLogFormatter errorFormatter;
  private final StructuredLogFileSinkSupport fileSinkSupport;

  public FileStructuredLogSink(
      Path allLogPath,
      Path errorLogPath,
      LogRotationPolicy rotationPolicy) {
    this(
        allLogPath,
        errorLogPath,
        rotationPolicy,
        StructuredLogFileSinkSupport.StructuredLogFileSinkSupportDefaultJsonFormatter(),
        StructuredLogFileSinkSupport.StructuredLogFileSinkSupportDefaultJsonFormatter());
  }

  public FileStructuredLogSink(
      Path allLogPath,
      Path errorLogPath,
      LogRotationPolicy rotationPolicy,
      StructuredLogFormatter allFormatter,
      StructuredLogFormatter errorFormatter) {
    this(allLogPath, errorLogPath, rotationPolicy, allFormatter, errorFormatter, false);
  }

  public FileStructuredLogSink(
      Path allLogPath,
      Path errorLogPath,
      LogRotationPolicy rotationPolicy,
      StructuredLogFormatter allFormatter,
      StructuredLogFormatter errorFormatter,
      boolean resetAllLogOnStart) {

    this.allLogPath = Objects.requireNonNull(allLogPath, "allLogPath");
    this.errorLogPath = Objects.requireNonNull(errorLogPath, "errorLogPath");
    this.allFormatter = Objects.requireNonNull(allFormatter, "allFormatter");
    this.errorFormatter = Objects.requireNonNull(errorFormatter, "errorFormatter");
    this.fileSinkSupport = new StructuredLogFileSinkSupport(rotationPolicy);
    fileSinkSupport.StructuredLogFileSinkSupportPrepare(
        this.allLogPath,
        this.allFormatter,
        resetAllLogOnStart);
    fileSinkSupport.StructuredLogFileSinkSupportPrepare(this.errorLogPath, this.errorFormatter, false);
  }

  @Override
  public synchronized void StructuredLogSinkWrite(
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(event, "event");

    try {
      boolean auditEvent = AUDIT.equalsIgnoreCase(event.StructuredLogEvtGetCategory());
      boolean errorEvent = ERROR.equals(event.StructuredLogEvtGetSeverity().toUpperCase(Locale.ROOT));

      if (!auditEvent && !errorEvent) {
        fileSinkSupport.StructuredLogFileSinkSupportAppend(
            allLogPath,
            allFormatter,
            event,
            throwable);
      }

      if (!auditEvent && errorEvent) {
        fileSinkSupport.StructuredLogFileSinkSupportAppend(
            errorLogPath,
            errorFormatter,
            event,
            throwable);
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to write structured log file", ex);
    }
  }
}
