package mcp.server.foundation.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical JSON codec for structured log events.
 */
public final class StructuredLogJsonCodec {

  private static final String KEY_TS = "timestamp";
  private static final String KEY_SEVERITY = "severity";
  private static final String KEY_LAYER = "layer";
  private static final String KEY_FROM = "from";
  private static final String KEY_TO = "to";
  private static final String KEY_ACTION = "action";
  private static final String KEY_MSG = "message";
  private static final String KEY_EVENT = "event";
  private static final String KEY_CATEGORY = "category";
  private static final String KEY_MCP_SESSION_ID = "session";
  private static final String KEY_WS_CONNECTION_ID = "connection";
  private static final String KEY_RPC_CORRELATION_ID = "correlation";
  private static final String KEY_RPC_METHOD = "rpc";
  private static final String KEY_TOOL_NAME = "tool";
  private static final String KEY_DURATION_MS = "durationMs";
  private static final String KEY_ERROR_TYPE = "errorType";
  private static final String KEY_TRANSPORT_NAME = "transport";
  private static final String KEY_SESSION_PHASE = "sessionPhase";
  private static final String KEY_TRACE_ID = "trace";
  private static final String KEY_SPAN_ID = "span";
  private static final String KEY_STACK_TRACE = "stackTrace";

  private final ObjectMapper mapper;
  private final StructuredLogTimestampFormatter timestampFormatter;

  public StructuredLogJsonCodec() {
    this(new StructuredLogTimestampFormatter());
  }

  public StructuredLogJsonCodec(StructuredLogTimestampFormatter timestampFormatter) {
    this.mapper = new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.timestampFormatter = Objects.requireNonNull(timestampFormatter, "timestampFormatter");
  }

  public String StructuredLogJsonCodeSerialize(StructuredLogEvent event) {
    return StructuredLogJsonCodeSerialize(event, null);
  }

  public String StructuredLogJsonCodeSerialize(
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(event, "event");

    try {
      return mapper.writeValueAsString(StructuredLogJsonCodeToMap(event, throwable));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to serialize structured log event", ex);
    }
  }

  public Map<String, Object> StructuredLogJsonCodeToMap(StructuredLogEvent event) {
    return StructuredLogJsonCodeToMap(event, null);
  }

  public Map<String, Object> StructuredLogJsonCodeToMap(
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(event, "event");

    Map<String, Object> payload = new LinkedHashMap<>();

    payload.put(KEY_TS, timestampFormatter.StructuredLogTimestampFormat(event.StructuredLogEvtGetTimestamp()));
    payload.put(KEY_SEVERITY, event.StructuredLogEvtGetSeverity());
    payload.put(KEY_LAYER, event.StructuredLogEvtGetLayer());
    payload.put(KEY_FROM, event.StructuredLogEvtGetFrom());
    payload.put(KEY_TO, event.StructuredLogEvtGetTo());
    payload.put(KEY_ACTION, event.StructuredLogEvtGetAction());
    payload.put(KEY_MSG, event.StructuredLogEvtGetMessage());

    putIfPresent(payload, KEY_EVENT, event.StructuredLogEvtGetEventName());
    putIfPresent(payload, KEY_CATEGORY, event.StructuredLogEvtGetCategory());
    payload.put(KEY_MCP_SESSION_ID, event.StructuredLogEvtGetMcpSessId());
    payload.put(KEY_WS_CONNECTION_ID, event.StructuredLogEvtGetWsConnId());
    payload.put(KEY_RPC_CORRELATION_ID, event.StructuredLogEvtGetRPCCorrelaId());
    putIfPresent(payload, KEY_RPC_METHOD, event.StructuredLogEvtGetRPCMet());
    putIfPresent(payload, KEY_TOOL_NAME, event.StructuredLogEvtGetToolName());
    putIfPresent(payload, KEY_DURATION_MS, event.StructuredLogEvtGetDurationMs());
    putIfPresent(payload, KEY_ERROR_TYPE, event.StructuredLogEvtGetErrType());
    putIfPresent(payload, KEY_TRANSPORT_NAME, event.StructuredLogEvtGetTranspName());
    putIfPresent(payload, KEY_SESSION_PHASE, event.StructuredLogEvtGetSessPhase());
    putIfPresent(payload, KEY_TRACE_ID, event.StructuredLogEvtGetTraceId());
    putIfPresent(payload, KEY_SPAN_ID, event.StructuredLogEvtGetSpanId());
    putIfPresent(payload, KEY_STACK_TRACE, throwable == null ? null : stackTraceOf(throwable));

    return Collections.unmodifiableMap(payload);
  }

  private static void putIfPresent(Map<String, Object> payload, String key, Object value) {
    if (value != null) {
      payload.put(key, value);
    }
  }

  private static String stackTraceOf(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString().stripTrailing();
  }
}
