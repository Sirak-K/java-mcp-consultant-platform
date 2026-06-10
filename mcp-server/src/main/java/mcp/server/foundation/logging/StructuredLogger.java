package mcp.server.foundation.logging;

import java.util.List;
import java.util.Objects;

/**
 * Central structured logger.
 */
public final class StructuredLogger {

  private final List<StructuredLogSink> sinks;

  public StructuredLogger(List<StructuredLogSink> sinks) {
    this.sinks = List.copyOf(Objects.requireNonNull(sinks, "sinks"));

    if (this.sinks.isEmpty()) {
      throw new IllegalArgumentException("sinks must not be empty");
    }
  }

  public StructuredLogger(
      StructuredLogJsonCodec codec,
      StructuredLogRedactor redactor,
      List<StructuredLogSink> sinks) {

    this(Objects.requireNonNull(sinks, "sinks"));
    Objects.requireNonNull(codec, "codec");
    Objects.requireNonNull(redactor, "redactor");
  }

  public void StructuredLogLog(StructuredLogEvent event) {
    StructuredLogLog(event, null);
  }

  public void StructuredLogLog(
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(event, "event");

    for (StructuredLogSink sink : sinks) {
      sink.StructuredLogSinkWrite(event, throwable);
    }
  }
}
