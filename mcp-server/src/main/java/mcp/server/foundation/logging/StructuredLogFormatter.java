package mcp.server.foundation.logging;

/**
 * Presentation formatter for a canonical structured log event.
 */
public interface StructuredLogFormatter {

  default String StructuredLogHeader() {
    return "";
  }

  String StructuredLogFormat(
      StructuredLogEvent event,
      Throwable throwable);
}
