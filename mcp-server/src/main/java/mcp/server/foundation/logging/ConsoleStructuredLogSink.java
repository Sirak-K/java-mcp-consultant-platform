package mcp.server.foundation.logging;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;

/**
 * Console sink that preserves the current stdout/stderr based workflow.
 */
public final class ConsoleStructuredLogSink implements StructuredLogSink {

  private static final String ERROR = "ERROR";
  private final StructuredLogFormatter formatter;
  private final PrintStream standardStream;
  private final PrintStream errorStream;

  public ConsoleStructuredLogSink() {
    this(new RedactedStructuredLogJsonFormatter(
        new StructuredLogJsonCodec(),
        new StructuredLogRedactor()));
  }

  public ConsoleStructuredLogSink(StructuredLogFormatter formatter) {
    this(formatter, System.out, System.err);
  }

  public ConsoleStructuredLogSink(
      StructuredLogFormatter formatter,
      PrintStream standardStream,
      PrintStream errorStream) {

    this.formatter = Objects.requireNonNull(formatter, "formatter");
    this.standardStream = Objects.requireNonNull(standardStream, "standardStream");
    this.errorStream = Objects.requireNonNull(errorStream, "errorStream");
  }

  @Override
  public void StructuredLogSinkWrite(
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(event, "event");

    PrintStream stream = ERROR.equals(event.StructuredLogEvtGetSeverity().toUpperCase(Locale.ROOT))
        ? errorStream
        : standardStream;

    stream.println(formatter.StructuredLogFormat(event, throwable));
  }
}
