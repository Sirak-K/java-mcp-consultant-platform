package mcp.server.foundation.logging;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import mcp.server.foundation.observability.context.McpCorrelaFieldCatal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Exports canonical structured log events as OTLP logs for collector/Loki
 * pipelines, while local file sinks remain the forensic source of truth.
 */
public final class OtlpStructuredLogSink implements StructuredLogSink {

  private static final AttributeKey<String> ATTR_LAYER = AttributeKey.stringKey("mcp.log.layer");
  private static final AttributeKey<String> ATTR_FROM = AttributeKey.stringKey("mcp.log.from");
  private static final AttributeKey<String> ATTR_TO = AttributeKey.stringKey("mcp.log.to");
  private static final AttributeKey<String> ATTR_ACTION = AttributeKey.stringKey("mcp.log.action");
  private static final AttributeKey<String> ATTR_EVENT = AttributeKey.stringKey("mcp.log.event");
  private static final AttributeKey<String> ATTR_CATEGORY = AttributeKey.stringKey("mcp.log.category");
  private static final AttributeKey<String> ATTR_MCP_SESSION_ID = AttributeKey.stringKey(McpCorrelaFieldCatal.MCP_SESSION_ID);
  private static final AttributeKey<String> ATTR_WS_CONNECTION_ID = AttributeKey.stringKey(McpCorrelaFieldCatal.MCP_WS_CONNECTION_ID);
  private static final AttributeKey<String> ATTR_RPC_CORRELATION_ID = AttributeKey.stringKey(McpCorrelaFieldCatal.MCP_RPC_CORRELATION_ID);
  private static final AttributeKey<String> ATTR_RPC_METHOD = AttributeKey.stringKey(McpCorrelaFieldCatal.MCP_RPC_METHOD);
  private static final AttributeKey<String> ATTR_TOOL_NAME = AttributeKey.stringKey(McpCorrelaFieldCatal.MCP_TOOL_NAME);
  private static final AttributeKey<String> ATTR_ERROR_TYPE = AttributeKey.stringKey(McpCorrelaFieldCatal.ERROR_TYPE);
  private static final AttributeKey<String> ATTR_TRANSPORT_NAME = AttributeKey.stringKey(McpCorrelaFieldCatal.MCP_TRANSPORT_NAME);
  private static final AttributeKey<String> ATTR_SESSION_PHASE = AttributeKey.stringKey(McpCorrelaFieldCatal.MCP_SESSION_PHASE);
  private static final AttributeKey<String> ATTR_TRACE_ID = AttributeKey.stringKey(McpCorrelaFieldCatal.TRACE_ID);
  private static final AttributeKey<String> ATTR_SPAN_ID = AttributeKey.stringKey(McpCorrelaFieldCatal.SPAN_ID);
  private static final AttributeKey<String> ATTR_EXCEPTION_TYPE = AttributeKey.stringKey("exception.type");
  private static final AttributeKey<String> ATTR_EXCEPTION_MESSAGE = AttributeKey.stringKey("exception.message");
  private static final AttributeKey<String> ATTR_EXCEPTION_STACKTRACE = AttributeKey.stringKey("exception.stacktrace");
  private static final AttributeKey<Long> ATTR_DURATION_MS = AttributeKey.longKey("mcp.duration.ms");

  private final Logger logger;
  private final StructuredLogRedactor redactor;

  public OtlpStructuredLogSink(
      Logger logger,
      StructuredLogRedactor redactor) {

    this.logger = Objects.requireNonNull(logger, "logger");
    this.redactor = Objects.requireNonNull(redactor, "redactor");
  }

  @Override
  public void StructuredLogSinkWrite(StructuredLogEvent event, Throwable throwable) {

    Objects.requireNonNull(event, "event");

    AttributesBuilder attributes = Attributes.builder()
        .put(ATTR_LAYER, event.StructuredLogEvtGetLayer())
        .put(ATTR_FROM, event.StructuredLogEvtGetFrom())
        .put(ATTR_TO, event.StructuredLogEvtGetTo())
        .put(ATTR_ACTION, event.StructuredLogEvtGetAction());

    OtlpStructuredLogSinkPut(attributes, ATTR_EVENT, event.StructuredLogEvtGetEventName());
    OtlpStructuredLogSinkPut(attributes, ATTR_CATEGORY, event.StructuredLogEvtGetCategory());
    OtlpStructuredLogSinkPut(attributes, ATTR_MCP_SESSION_ID, event.StructuredLogEvtGetMcpSessId());
    OtlpStructuredLogSinkPut(attributes, ATTR_WS_CONNECTION_ID, event.StructuredLogEvtGetWsConnId());
    OtlpStructuredLogSinkPut(attributes, ATTR_RPC_CORRELATION_ID, event.StructuredLogEvtGetRPCCorrelaId());
    OtlpStructuredLogSinkPut(attributes, ATTR_RPC_METHOD, event.StructuredLogEvtGetRPCMet());
    OtlpStructuredLogSinkPut(attributes, ATTR_TOOL_NAME, event.StructuredLogEvtGetToolName());
    OtlpStructuredLogSinkPut(attributes, ATTR_ERROR_TYPE, event.StructuredLogEvtGetErrType());
    OtlpStructuredLogSinkPut(attributes, ATTR_TRANSPORT_NAME, event.StructuredLogEvtGetTranspName());
    OtlpStructuredLogSinkPut(attributes, ATTR_SESSION_PHASE, event.StructuredLogEvtGetSessPhase());
    OtlpStructuredLogSinkPut(attributes, ATTR_TRACE_ID, event.StructuredLogEvtGetTraceId());
    OtlpStructuredLogSinkPut(attributes, ATTR_SPAN_ID, event.StructuredLogEvtGetSpanId());

    if (event.StructuredLogEvtGetDurationMs() != null) {
      attributes.put(ATTR_DURATION_MS, event.StructuredLogEvtGetDurationMs());
    }

    if (throwable != null) {
      attributes.put(ATTR_EXCEPTION_TYPE, throwable.getClass().getName());
      OtlpStructuredLogSinkPut(attributes, ATTR_EXCEPTION_MESSAGE, throwable.getMessage());
      OtlpStructuredLogSinkPut(
          attributes,
          ATTR_EXCEPTION_STACKTRACE,
          redactor.StructuredLogRedact(OtlpStructuredLogSinkStackTrace(throwable)));
    }

    logger.logRecordBuilder()
        .setContext(Context.current())
        .setTimestamp(event.StructuredLogEvtGetTimestamp().toEpochMilli(), TimeUnit.MILLISECONDS)
        .setSeverity(OtlpStructuredLogSinkSeverity(event.StructuredLogEvtGetSeverity()))
        .setSeverityText(event.StructuredLogEvtGetSeverity())
        .setBody(redactor.StructuredLogRedact(event.StructuredLogEvtGetMessage()))
        .setAllAttributes(attributes.build())
        .emit();
  }

  private static void OtlpStructuredLogSinkPut(
      AttributesBuilder attributes,
      AttributeKey<String> key,
      String value) {

    if (value != null && !value.isBlank()) {
      attributes.put(key, value);
    }
  }

  private static Severity OtlpStructuredLogSinkSeverity(String severity) {
    if (severity == null) {
      return Severity.INFO;
    }

    return switch (severity.trim().toUpperCase()) {
      case "WARN" -> Severity.WARN;
      case "ERROR" -> Severity.ERROR;
      default -> Severity.INFO;
    };
  }

  private static String OtlpStructuredLogSinkStackTrace(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }
}
