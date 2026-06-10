package mcp.server.foundation.logging;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.context.ObservCtxHolder;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Foundation-only structured logger.
 */
public final class ServerLogger {

  private static final String CATEGORY_AUDIT = "AUDIT";

  public enum Component {
    WS,
    RPC,
    MCP,
    RUNTIME
  }

  public enum Severity {
    INFO,
    WARN,
    ERROR
  }

  private static final String NOT_APPLICABLE = "N/A";
  public static final String WS_UNBOUND = "UNBOUND";

  private static final String DEFAULT_FROM = "SERVER";
  private static final String DEFAULT_TO = "CLIENT";
  private static final String DEFAULT_ACTION = "RUNNING";
  private static final ObservCtxFactory OBS_CONTEXT_FACTORY = new ObservCtxFactory();

  private final StructuredLogger structuredLogger;

  public ServerLogger() {
    this(new StructuredLogger(List.of(new HumanReadableConsoleLogSink(Severity.INFO))));
  }

  public ServerLogger(StructuredLogger structuredLogger) {
    this.structuredLogger = Objects.requireNonNull(structuredLogger, "structuredLogger");
  }

  public static String ServerLogNormalizeMcpSessId(String mcpSessId) {
    return normalizeMcp(mcpSessId);
  }

  public static String ServerLogNormalizeWsConnId(String wsConnId) {
    return normalizeWs(wsConnId);
  }

  public static String ServerLogNormalizeRPCCorrelaId(String rpcCorrelaId) {
    return normalizeCorr(rpcCorrelaId);
  }

  public void ServerLogInfoStructured(
      Component component,
      String mcpSessId,
      String wsConnId,
      String rpcCorrelaId,
      String message) {

    ServerLogStructured(
        Severity.INFO,
        component,
        null,
        mcpSessId,
        wsConnId,
        rpcCorrelaId,
        message,
        null);
  }

  public void ServerLogWarnStructured(
      Component component,
      String mcpSessId,
      String wsConnId,
      String rpcCorrelaId,
      String message) {

    ServerLogStructured(
        Severity.WARN,
        component,
        null,
        mcpSessId,
        wsConnId,
        rpcCorrelaId,
        message,
        null);
  }

  public void ServerLogErrorStructured(
      Component component,
      String mcpSessId,
      String wsConnId,
      String rpcCorrelaId,
      String message,
      Throwable ex) {

    ServerLogStructured(
        Severity.ERROR,
        component,
        null,
        mcpSessId,
        wsConnId,
        rpcCorrelaId,
        message,
        ex);
  }

  public void ServerLogInfoObserved(
      Component component,
      ObservCtx context,
      String action,
      String eventName,
      String message) {

    ServerLogObserved(Severity.INFO, component, null, context, action, eventName, message, null, null, null);
  }

  public void ServerLogInfoObserved(
      Component component,
      ObservCtx context,
      String action,
      String eventName,
      String message,
      Long durationMs) {

    ServerLogObserved(Severity.INFO, component, null, context, action, eventName, message, durationMs, null, null);
  }

  public void ServerLogWarnObserved(
      Component component,
      ObservCtx context,
      String action,
      String eventName,
      String message,
      String errorType) {

    ServerLogObserved(Severity.WARN, component, null, context, action, eventName, message, null, errorType, null);
  }

  public void ServerLogErrorObserved(
      Component component,
      ObservCtx context,
      String action,
      String eventName,
      String message,
      String errorType,
      Throwable throwable) {

    ServerLogObserved(Severity.ERROR, component, null, context, action, eventName, message, null, errorType, throwable);
  }

  public void ServerLogErrorObserved(
      Component component,
      ObservCtx context,
      String action,
      String eventName,
      String message,
      Long durationMs,
      String errorType,
      Throwable throwable) {

    ServerLogObserved(Severity.ERROR, component, null, context, action, eventName, message, durationMs, errorType, throwable);
  }

  public void ServerLogAuditInfoObserved(
      Component component,
      ObservCtx context,
      String action,
      String eventName,
      String message) {

    ServerLogObserved(Severity.INFO, component, CATEGORY_AUDIT, context, action, eventName, message, null, null, null);
  }

  public void ServerLogAuditWarnObserved(
      Component component,
      ObservCtx context,
      String action,
      String eventName,
      String message,
      String errorType) {

    ServerLogObserved(Severity.WARN, component, CATEGORY_AUDIT, context, action, eventName, message, null, errorType, null);
  }

  public void ServerLogAuditErrorObserved(
      Component component,
      ObservCtx context,
      String action,
      String eventName,
      String message,
      String errorType,
      Throwable throwable) {

    ServerLogObserved(Severity.ERROR, component, CATEGORY_AUDIT, context, action, eventName, message, null, errorType, throwable);
  }

  public void ServerLogSecurityAuditWarnDeniedObserved(
      Component component,
      ObservCtx context,
      String eventName,
      String message) {

    String normalizedEventName = ServerLogNormalizeSecurityEventName(eventName);
    ServerLogObserved(
        Severity.WARN,
        component,
        CATEGORY_AUDIT,
        context,
        "DENY",
        normalizedEventName,
        message,
        null,
        normalizedEventName,
        null);
  }

  public void ServerLogSecurityAuditErrorDeniedObserved(
      Component component,
      ObservCtx context,
      String eventName,
      String message,
      Throwable throwable) {

    String normalizedEventName = ServerLogNormalizeSecurityEventName(eventName);
    ServerLogObserved(
        Severity.ERROR,
        component,
        CATEGORY_AUDIT,
        context,
        "DENY",
        normalizedEventName,
        message,
        null,
        normalizedEventName,
        throwable);
  }

  private void ServerLogStructured(
      Severity severity,
      Component component,
      String category,
      String mcpSessId,
      String wsConnId,
      String rpcCorrelaId,
      String message,
      Throwable ex) {

    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(component, "component");

    StructuredLogEvent event = StructuredLogEvent.builder()
        .severity(severity.name())
        .layer(componentToLayer(component))
        .from(DEFAULT_FROM)
        .to(DEFAULT_TO)
        .action(DEFAULT_ACTION)
        .message(message)
        .category(category)
        .mcpSessId(normalizeOptionalForEvent(normalizeMcp(mcpSessId), NOT_APPLICABLE))
        .wsConnId(normalizeOptionalForEvent(normalizeWs(wsConnId), WS_UNBOUND))
        .rpcCorrelaId(normalizeOptionalForEvent(normalizeCorr(rpcCorrelaId), NOT_APPLICABLE))
        .build();

    structuredLogger.StructuredLogLog(event, ex);
  }

  private void ServerLogObserved(
      Severity severity,
      Component component,
      String category,
      ObservCtx context,
      String action,
      String eventName,
      String message,
      Long durationMs,
      String errorType,
      Throwable throwable) {

    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(component, "component");

    ObservCtx safeContext = ServerLogEffectiveCtx(context);

    StructuredLogEvent event = StructuredLogEvent.builder()
        .severity(severity.name())
        .layer(componentToLayer(component))
        .from(DEFAULT_FROM)
        .to(DEFAULT_TO)
        .action(action == null || action.isBlank() ? DEFAULT_ACTION : action)
        .eventName(eventName)
        .category(category)
        .message(message)
        .mcpSessId(normalizeOptionalForEvent(normalizeMcp(safeContext.ObservCtxGetMcpSessId()), NOT_APPLICABLE))
        .wsConnId(normalizeOptionalForEvent(normalizeWs(safeContext.ObservCtxGetWsConnId()), WS_UNBOUND))
        .rpcCorrelaId(normalizeOptionalForEvent(normalizeCorr(safeContext.ObservCtxGetRPCCorrelaId()), NOT_APPLICABLE))
        .rpcMet(safeContext.ObservCtxGetRPCMet())
        .toolName(safeContext.ObservCtxGetToolName())
        .durationMs(durationMs)
        .errorType(errorType == null ? safeContext.ObservCtxGetErrType() : errorType)
        .transportName(safeContext.ObservCtxGetTranspName())
        .sessionPhase(safeContext.ObservCtxGetSessPhase())
        .traceId(ServerLogCurrentTraceId())
        .spanId(ServerLogCurrentSpanId())
        .build();

    structuredLogger.StructuredLogLog(event, throwable);
  }

  private static ObservCtx ServerLogEffectiveCtx(ObservCtx primary) {
    return OBS_CONTEXT_FACTORY.ObservCtxFactoryMerge(primary, ObservCtxHolder.ObservCtxHolderGet());
  }

  private static String normalizeMcp(String id) {
    return (id == null || id.isBlank()) ? NOT_APPLICABLE : id;
  }

  private static String normalizeWs(String id) {
    return (id == null || id.isBlank()) ? WS_UNBOUND : id;
  }

  private static String normalizeCorr(String id) {
    return (id == null || id.isBlank()) ? NOT_APPLICABLE : id;
  }

  private static String componentToLayer(Component component) {
    return switch (component) {
      case WS -> "WS";
      case RPC -> "RPC";
      case MCP -> "MCP";
      case RUNTIME -> "RUNTIME";
    };
  }

  private static String normalizeOptionalForEvent(String value, String sentinel) {
    return sentinel.equals(value) ? null : value;
  }

  private static String ServerLogNormalizeSecurityEventName(String eventName) {
    Objects.requireNonNull(eventName, "eventName");

    String normalized = eventName.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("eventName must not be blank");
    }

    return normalized.toUpperCase(Locale.ROOT);
  }

  private static String ServerLogCurrentTraceId() {
    SpanContext spanContext = Span.current().getSpanContext();
    return spanContext.isValid() ? spanContext.getTraceId() : null;
  }

  private static String ServerLogCurrentSpanId() {
    SpanContext spanContext = Span.current().getSpanContext();
    return spanContext.isValid() ? spanContext.getSpanId() : null;
  }
}
