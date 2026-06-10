package mcp.server.foundation.logging;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Dedicated file sink for structured logging in the test profile.
 */
public final class TestStructuredLogSink implements StructuredLogSink {

  private final Path testLogPath;
  private final StructuredLogFormatter formatter;
  private final StructuredLogFileSinkSupport fileSinkSupport;

  public TestStructuredLogSink(
      Path testLogPath,
      LogRotationPolicy rotationPolicy) {
    this(
        testLogPath,
        rotationPolicy,
        StructuredLogFileSinkSupport.StructuredLogFileSinkSupportDefaultJsonFormatter());
  }

  public TestStructuredLogSink(
      Path testLogPath,
      LogRotationPolicy rotationPolicy,
      StructuredLogFormatter formatter) {

    this.testLogPath = Objects.requireNonNull(testLogPath, "testLogPath");
    this.formatter = Objects.requireNonNull(formatter, "formatter");
    this.fileSinkSupport = new StructuredLogFileSinkSupport(rotationPolicy);
    fileSinkSupport.StructuredLogFileSinkSupportPrepare(
        this.testLogPath.toAbsolutePath().normalize(),
        this.formatter,
        false);
  }

  @Override
  public synchronized void StructuredLogSinkWrite(
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(event, "event");
    fileSinkSupport.StructuredLogFileSinkSupportAppend(
        testLogPath,
        formatter,
        event,
        throwable);
  }
}
