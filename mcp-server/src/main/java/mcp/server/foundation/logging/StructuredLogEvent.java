package mcp.server.foundation.logging;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable canonical structured log event.
 */
public final class StructuredLogEvent {

  private final Instant timestamp;
  private final String severity;
  private final String layer;
  private final String from;
  private final String to;
  private final String action;
  private final String message;

  private final String eventName;
  private final String category;
  private final String mcpSessId;
  private final String wsConnId;
  private final String rpcCorrelaId;
  private final String rpcMet;
  private final String toolName;
  private final Long durationMs;
  private final String errorType;
  private final String transportName;
  private final String sessionPhase;
  private final String traceId;
  private final String spanId;

  private StructuredLogEvent(Builder builder) {
    this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp");
    this.severity = requireNonBlank(builder.severity, "severity");
    this.layer = requireNonBlank(builder.layer, "layer");
    this.from = requireNonBlank(builder.from, "from");
    this.to = requireNonBlank(builder.to, "to");
    this.action = requireNonBlank(builder.action, "action");
    this.message = builder.message == null ? "" : builder.message;

    this.eventName = normalizeOptional(builder.eventName);
    this.category = normalizeOptional(builder.category);
    this.mcpSessId = normalizeOptional(builder.mcpSessId);
    this.wsConnId = normalizeOptional(builder.wsConnId);
    this.rpcCorrelaId = normalizeOptional(builder.rpcCorrelaId);
    this.rpcMet = normalizeOptional(builder.rpcMet);
    this.toolName = normalizeOptional(builder.toolName);
    this.durationMs = builder.durationMs;
    this.errorType = normalizeOptional(builder.errorType);
    this.transportName = normalizeOptional(builder.transportName);
    this.sessionPhase = normalizeOptional(builder.sessionPhase);
    this.traceId = normalizeOptional(builder.traceId);
    this.spanId = normalizeOptional(builder.spanId);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Instant StructuredLogEvtGetTimestamp() {
    return timestamp;
  }

  public String StructuredLogEvtGetSeverity() {
    return severity;
  }

  public String StructuredLogEvtGetLayer() {
    return layer;
  }

  public String StructuredLogEvtGetFrom() {
    return from;
  }

  public String StructuredLogEvtGetTo() {
    return to;
  }

  public String StructuredLogEvtGetAction() {
    return action;
  }

  public String StructuredLogEvtGetMessage() {
    return message;
  }

  public String StructuredLogEvtGetEventName() {
    return eventName;
  }

  public String StructuredLogEvtGetCategory() {
    return category;
  }

  public String StructuredLogEvtGetMcpSessId() {
    return mcpSessId;
  }

  public String StructuredLogEvtGetWsConnId() {
    return wsConnId;
  }

  public String StructuredLogEvtGetRPCCorrelaId() {
    return rpcCorrelaId;
  }

  public String StructuredLogEvtGetRPCMet() {
    return rpcMet;
  }

  public String StructuredLogEvtGetToolName() {
    return toolName;
  }

  public Long StructuredLogEvtGetDurationMs() {
    return durationMs;
  }

  public String StructuredLogEvtGetErrType() {
    return errorType;
  }

  public String StructuredLogEvtGetTranspName() {
    return transportName;
  }

  public String StructuredLogEvtGetSessPhase() {
    return sessionPhase;
  }

  public String StructuredLogEvtGetTraceId() {
    return traceId;
  }

  public String StructuredLogEvtGetSpanId() {
    return spanId;
  }

  private static String requireNonBlank(String value, String field) {
    Objects.requireNonNull(value, field);

    String normalized = value.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }

    return normalized;
  }

  private static String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  public static final class Builder {

    private Instant timestamp = Instant.now();
    private String severity;
    private String layer;
    private String from;
    private String to;
    private String action;
    private String message;
    private String eventName;
    private String category;
    private String mcpSessId;
    private String wsConnId;
    private String rpcCorrelaId;
    private String rpcMet;
    private String toolName;
    private Long durationMs;
    private String errorType;
    private String transportName;
    private String sessionPhase;
    private String traceId;
    private String spanId;

    private Builder() {
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder severity(String severity) {
      this.severity = severity;
      return this;
    }

    public Builder layer(String layer) {
      this.layer = layer;
      return this;
    }

    public Builder from(String from) {
      this.from = from;
      return this;
    }

    public Builder to(String to) {
      this.to = to;
      return this;
    }

    public Builder action(String action) {
      this.action = action;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder eventName(String eventName) {
      this.eventName = eventName;
      return this;
    }

    public Builder category(String category) {
      this.category = category;
      return this;
    }

    public Builder mcpSessId(String mcpSessId) {
      this.mcpSessId = mcpSessId;
      return this;
    }

    public Builder wsConnId(String wsConnId) {
      this.wsConnId = wsConnId;
      return this;
    }

    public Builder rpcCorrelaId(String rpcCorrelaId) {
      this.rpcCorrelaId = rpcCorrelaId;
      return this;
    }

    public Builder rpcMet(String rpcMet) {
      this.rpcMet = rpcMet;
      return this;
    }

    public Builder toolName(String toolName) {
      this.toolName = toolName;
      return this;
    }

    public Builder durationMs(Long durationMs) {
      this.durationMs = durationMs;
      return this;
    }

    public Builder errorType(String errorType) {
      this.errorType = errorType;
      return this;
    }

    public Builder transportName(String transportName) {
      this.transportName = transportName;
      return this;
    }

    public Builder sessionPhase(String sessionPhase) {
      this.sessionPhase = sessionPhase;
      return this;
    }

    public Builder traceId(String traceId) {
      this.traceId = traceId;
      return this;
    }

    public Builder spanId(String spanId) {
      this.spanId = spanId;
      return this;
    }

    public StructuredLogEvent build() {
      return new StructuredLogEvent(this);
    }
  }
}
