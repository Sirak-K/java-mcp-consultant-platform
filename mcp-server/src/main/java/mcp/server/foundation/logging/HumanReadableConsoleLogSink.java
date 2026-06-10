package mcp.server.foundation.logging;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Human-readable console sink with per-environment severity filtering.
 */
public final class HumanReadableConsoleLogSink implements StructuredLogSink {

  private static final String DEFAULT_LAYER_RUNTIME = "RUNTIME";

  private final ServerLogger.Severity minimumSeverity;
  private final StructuredLogTimestampFormatter timestampFormatter;
  private final StructuredLogRedactor redactor;
  private final PrintStream standardStream;
  private final PrintStream errorStream;

  public HumanReadableConsoleLogSink(ServerLogger.Severity minimumSeverity) {
    this(
        minimumSeverity,
        new StructuredLogTimestampFormatter(),
        new StructuredLogRedactor());
  }

  public HumanReadableConsoleLogSink(
      ServerLogger.Severity minimumSeverity,
      StructuredLogTimestampFormatter timestampFormatter) {

    this(
        minimumSeverity,
        timestampFormatter,
        new StructuredLogRedactor());
  }

  public HumanReadableConsoleLogSink(
      ServerLogger.Severity minimumSeverity,
      StructuredLogTimestampFormatter timestampFormatter,
      StructuredLogRedactor redactor) {

    this(
        minimumSeverity,
        timestampFormatter,
        redactor,
        System.out,
        System.err);
  }

  public HumanReadableConsoleLogSink(
      ServerLogger.Severity minimumSeverity,
      StructuredLogTimestampFormatter timestampFormatter,
      StructuredLogRedactor redactor,
      PrintStream standardStream,
      PrintStream errorStream) {

    this.minimumSeverity = Objects.requireNonNull(minimumSeverity, "minimumSeverity");
    this.timestampFormatter = Objects.requireNonNull(timestampFormatter, "timestampFormatter");
    this.redactor = Objects.requireNonNull(redactor, "redactor");
    this.standardStream = Objects.requireNonNull(standardStream, "standardStream");
    this.errorStream = Objects.requireNonNull(errorStream, "errorStream");
  }

  @Override
  public void StructuredLogSinkWrite(
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(event, "event");

    ServerLogger.Severity eventSeverity = ServerLogger.Severity.valueOf(
        event.StructuredLogEvtGetSeverity().toUpperCase());

    if (eventSeverity.ordinal() < minimumSeverity.ordinal()) {
      return;
    }

    PrintStream stream = eventSeverity == ServerLogger.Severity.ERROR
        ? errorStream
        : standardStream;

    stream.println(redactor.StructuredLogRedact(buildHumanLine(event, eventSeverity)));
  }

  private String buildHumanLine(StructuredLogEvent event, ServerLogger.Severity eventSeverity) {
    String date = timestampFormatter.StructuredLogTimestampFormatDate(event.StructuredLogEvtGetTimestamp());
    String time = timestampFormatter.StructuredLogTimestampFormatTime(event.StructuredLogEvtGetTimestamp());
    String layer = normalizeLayer(event.StructuredLogEvtGetLayer());
    String eventName = event.StructuredLogEvtGetEventName();
    String msg = event.StructuredLogEvtGetMessage() == null ? "" : event.StructuredLogEvtGetMessage();

    return new StringBuilder()
        .append(date)
        .append(" | ")
        .append(time)
        .append(" | ")
        .append(eventSeverity.name())
        .append(" | ")
        .append(layer)
        .append(" | ")
        .append(eventName == null ? "" : eventName)
        .append(" | ")
        .append(msg)
        .toString();
  }

  private static String normalizeLayer(String layer) {
    if (layer == null || layer.isBlank()) {
      return DEFAULT_LAYER_RUNTIME;
    }
    return layer;
  }
}
