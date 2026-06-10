package mcp.server.foundation.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Formats log timestamps in the local server timezone with millisecond precision.
 */
public final class StructuredLogTimestampFormatter {

  private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd", Locale.ROOT);
  private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("HH:mm:ss.SSS", Locale.ROOT);
  private static final DateTimeFormatter LOG_TIMESTAMP_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd | HH:mm:ss.SSS", Locale.ROOT);

  private final ZoneId zoneId;

  public StructuredLogTimestampFormatter() {
    this(ZoneId.systemDefault());
  }

  public StructuredLogTimestampFormatter(ZoneId zoneId) {
    this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
  }

  public String StructuredLogTimestampFormat(Instant timestamp) {
    Objects.requireNonNull(timestamp, "timestamp");
    return LOG_TIMESTAMP_FORMATTER.format(timestamp.atZone(zoneId));
  }

  public String StructuredLogTimestampFormatDate(Instant timestamp) {
    Objects.requireNonNull(timestamp, "timestamp");
    return LOG_DATE_FORMATTER.format(timestamp.atZone(zoneId));
  }

  public String StructuredLogTimestampFormatTime(Instant timestamp) {
    Objects.requireNonNull(timestamp, "timestamp");
    return LOG_TIME_FORMATTER.format(timestamp.atZone(zoneId));
  }
}
