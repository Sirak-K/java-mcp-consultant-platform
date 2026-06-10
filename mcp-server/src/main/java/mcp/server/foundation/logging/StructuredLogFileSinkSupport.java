package mcp.server.foundation.logging;

import java.nio.file.Path;
import java.util.Objects;

final class StructuredLogFileSinkSupport {

  private final RollingLogFileSupport rollingLogFileSupport;

  StructuredLogFileSinkSupport(LogRotationPolicy rotationPolicy) {
    this.rollingLogFileSupport = new RollingLogFileSupport(Objects.requireNonNull(rotationPolicy, "rotationPolicy"));
  }

  static StructuredLogFormatter StructuredLogFileSinkSupportDefaultJsonFormatter() {
    return new RedactedStructuredLogJsonFormatter(
        new StructuredLogJsonCodec(),
        new StructuredLogRedactor());
  }

  void StructuredLogFileSinkSupportPrepare(
      Path path,
      StructuredLogFormatter formatter,
      boolean resetOnStart) {

    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(formatter, "formatter");

    if (resetOnStart) {
      rollingLogFileSupport.RollingLogFileReset(path, formatter.StructuredLogHeader());
      return;
    }

    rollingLogFileSupport.RollingLogFileEnsureExists(path);
  }

  void StructuredLogFileSinkSupportAppend(
      Path path,
      StructuredLogFormatter formatter,
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(formatter, "formatter");
    Objects.requireNonNull(event, "event");

    rollingLogFileSupport.RollingLogFileAppendLine(
        path,
        formatter.StructuredLogFormat(event, throwable),
        formatter.StructuredLogHeader());
  }
}
