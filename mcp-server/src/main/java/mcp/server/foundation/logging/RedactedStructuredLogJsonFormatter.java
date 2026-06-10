package mcp.server.foundation.logging;

import java.util.Objects;

/**
 * Canonical JSON formatter with redaction applied before sink output.
 */
public final class RedactedStructuredLogJsonFormatter implements StructuredLogFormatter {

  private final StructuredLogJsonCodec codec;
  private final StructuredLogRedactor redactor;

  public RedactedStructuredLogJsonFormatter(
      StructuredLogJsonCodec codec,
      StructuredLogRedactor redactor) {

    this.codec = Objects.requireNonNull(codec, "codec");
    this.redactor = Objects.requireNonNull(redactor, "redactor");
  }

  @Override
  public String StructuredLogFormat(
      StructuredLogEvent event,
      Throwable throwable) {

    return redactor.StructuredLogRedact(codec.StructuredLogJsonCodeSerialize(event, throwable));
  }
}
