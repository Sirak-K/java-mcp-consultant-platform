package mcp.server.foundation.logging;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Optional file sink dedicated to audit-classified structured events.
 */
public final class AuditStructuredLogSink implements StructuredLogSink {

  private final Path auditLogPath;
  private final StructuredLogFormatter formatter;
  private final StructuredLogFileSinkSupport fileSinkSupport;

  public AuditStructuredLogSink(
      Path auditLogPath,
      LogRotationPolicy rotationPolicy) {
    this(
        auditLogPath,
        rotationPolicy,
        StructuredLogFileSinkSupport.StructuredLogFileSinkSupportDefaultJsonFormatter());
  }

  public AuditStructuredLogSink(
      Path auditLogPath,
      LogRotationPolicy rotationPolicy,
      StructuredLogFormatter formatter) {

    this.auditLogPath = Objects.requireNonNull(auditLogPath, "auditLogPath");
    this.formatter = Objects.requireNonNull(formatter, "formatter");
    this.fileSinkSupport = new StructuredLogFileSinkSupport(rotationPolicy);
    fileSinkSupport.StructuredLogFileSinkSupportPrepare(this.auditLogPath, this.formatter, false);
  }

  @Override
  public synchronized void StructuredLogSinkWrite(
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(event, "event");

    if (!"AUDIT".equalsIgnoreCase(event.StructuredLogEvtGetCategory())) {
      return;
    }

    fileSinkSupport.StructuredLogFileSinkSupportAppend(
        auditLogPath,
        formatter,
        event,
        throwable);
  }
}
