package mcp.server.foundation.logging;

/**
 * Sink contract for canonical structured log events.
 */
public interface StructuredLogSink {

  void StructuredLogSinkWrite(
      StructuredLogEvent event,
      Throwable throwable);
}
