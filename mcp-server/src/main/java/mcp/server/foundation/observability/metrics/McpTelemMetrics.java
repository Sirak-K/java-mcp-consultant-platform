package mcp.server.foundation.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import mcp.server.foundation.observability.context.ObservCtx;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class McpTelemMetrics {

  private static final double[] LATENCY_PERCENTILES = new double[] {0.95d, 0.99d};
  private static final String FALLBACK = "unknown";
  private static final String NONE = "none";

  private final MeterRegistry meterRegistry;
  private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, SessionMetricState> sessionMetrics = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, ToolConcurrMetricState> toolConcurrMetrics = new ConcurrentHashMap<>();
  private final AtomicInteger activeToolInvocations = new AtomicInteger();
  private final AtomicInteger globalToolConcurrActive = new AtomicInteger();
  private final AtomicInteger globalToolConcurrLimit = new AtomicInteger();
  private final AtomicLong rpcRequestsTotal = new AtomicLong();
  private final AtomicLong transportErrorsTotal = new AtomicLong();
  private final AtomicLong authDenialsTotal = new AtomicLong();
  private final AtomicLong toolInvocationsTotal = new AtomicLong();
  private final AtomicLong toolTimeoutsTotal = new AtomicLong();
  private final AtomicLong toolRejectionsTotal = new AtomicLong();
  private final AtomicLong toolCancellationsTotal = new AtomicLong();
  private final AtomicLong toolFailuresTotal = new AtomicLong();
  private final AtomicLong queuePressureEventCount = new AtomicLong();
  private final AtomicLong queueWaitMaxMillis = new AtomicLong();
  private final TimerAccumulator queueWaitDurations = new TimerAccumulator();
  private final AtomicLong persistenceCallsTotal = new AtomicLong();
  private final AtomicLong persistenceFailuresTotal = new AtomicLong();
  private final ConcurrentMap<String, ToolFailureAccumulator> toolFailures = new ConcurrentHashMap<>();

  public McpTelemMetrics() {
    this(null);
  }

  public McpTelemMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;

    if (meterRegistry != null) {
      Gauge.builder(McpMetricCatal.MCP_TOOL_EXECUTIONS_ACTIVE, activeToolInvocations, AtomicInteger::get)
          .description("Active MCP tool executions")
          .register(meterRegistry);
      Gauge.builder(McpMetricCatal.MCP_TOOL_GLOBAL_CONCURRENCY_ACTIVE, globalToolConcurrActive, AtomicInteger::get)
          .description("Active MCP global tool concurrency slots")
          .register(meterRegistry);
      Gauge.builder(McpMetricCatal.MCP_TOOL_GLOBAL_CONCURRENCY_LIMIT, globalToolConcurrLimit, AtomicInteger::get)
          .description("Configured MCP global tool concurrency limit")
          .register(meterRegistry);
      Gauge.builder(
          McpMetricCatal.MCP_TOOL_GLOBAL_CONCURRENCY_UTILIZATION,
          this,
          metrics -> metrics.McpTelemConcurrRatio(
              metrics.globalToolConcurrActive,
              metrics.globalToolConcurrLimit))
          .description("Utilization ratio of the global MCP tool concurrency limit")
          .register(meterRegistry);
    }
  }

  public static McpTelemMetrics McpTelemNoOp() {
    return new McpTelemMetrics();
  }

  public void McpTelemSessionOpened(
      String sessionId,
      String transportName,
      String sessionPhase) {

    if (meterRegistry == null) {
      return;
    }

    String normalizedSessionId = McpTelemSessId(sessionId);
    if (normalizedSessionId == null) {
      return;
    }

    String normalizedTransp = McpTelemTag(transportName);
    String normalizedPhase = McpTelemTag(sessionPhase);
    long createdTimestampSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

    sessionMetrics.compute(normalizedSessionId, (ignored, existing) -> {
      if (existing != null) {
        existing.unregister(meterRegistry);
      }
      return SessionMetricState.register(
          meterRegistry,
          normalizedSessionId,
          normalizedTransp,
          normalizedPhase,
          createdTimestampSeconds);
    });
  }

  public void McpTelemSessionPhaseUpdated(
      String sessionId,
      String transportName,
      String sessionPhase) {

    if (meterRegistry == null) {
      return;
    }

    String normalizedSessionId = McpTelemSessId(sessionId);
    if (normalizedSessionId == null) {
      return;
    }

    SessionMetricState state = sessionMetrics.get(normalizedSessionId);
    if (state == null) {
      return;
    }

    state.updateActiveSeries(
        meterRegistry,
        McpTelemTag(transportName),
        McpTelemTag(sessionPhase));
    state.markActivityNow();
  }

  public void McpTelemSessClosed(String sessionId) {
    McpTelemSessClosed(sessionId, null, "normal_close");
  }

  public void McpTelemSessClosed(
      String sessionId,
      String transportName,
      String closeReason) {

    if (meterRegistry == null) {
      return;
    }

    String normalizedSessionId = McpTelemSessId(sessionId);
    if (normalizedSessionId == null) {
      return;
    }

    SessionMetricState state = sessionMetrics.remove(normalizedSessionId);
    if (state == null) {
      return;
    }

    state.recordPhaseDurationNow();
    McpTelemIncrementCounter(
        McpMetricCatal.MCP_SESSION_CLOSES_TOTAL,
        Tags.of(
            "transport", McpTelemTag(transportName),
            "close_reason", McpTelemTag(closeReason)));
    state.unregister(meterRegistry);
  }

  public void McpTelemIncrementSessReqs(ObservCtx context) {
    McpTelemIncrementSessionMetric(context, SessionMetricState::requestCountIncrement);
  }

  public void McpTelemIncrementSessResp(ObservCtx context) {
    McpTelemIncrementSessionMetric(context, SessionMetricState::responseCountIncrement);
  }

  public void McpTelemIncrementSessSuccessfulToolInvoc(ObservCtx context) {
    McpTelemIncrementSessionMetric(context, SessionMetricState::successfulToolInvocIncrement);
  }

  public void McpTelemRecordRpc(
      ObservCtx context,
      String outcome,
      String errorType,
      long durationMillis) {

    rpcRequestsTotal.incrementAndGet();

    Tags tags = Tags.of(
        "transport", McpTelemTag(context == null ? null : context.ObservCtxGetTranspName()),
        "rpc_method", McpTelemTag(context == null ? null : context.ObservCtxGetRPCMet()),
        "outcome", McpTelemTag(outcome),
        "error_type", McpTelemErrTag(errorType));

    McpTelemIncrementCounter(McpMetricCatal.MCP_RPC_REQUESTS_TOTAL, tags);
    McpTelemRecordTimer(McpMetricCatal.MCP_RPC_DURATION, tags, durationMillis);
  }

  public void McpTelemRecordToolQueueWait(
      ObservCtx context,
      String toolName,
      String result,
      long durationMillis) {

    queueWaitDurations.record(durationMillis);
    if (durationMillis > 0L || !"acquired".equalsIgnoreCase(result)) {
      queuePressureEventCount.incrementAndGet();
    }
    queueWaitMaxMillis.accumulateAndGet(McpTelemDuration(durationMillis), Math::max);

    Tags tags = Tags.of(
        "transport", McpTelemTag(context == null ? null : context.ObservCtxGetTranspName()),
        "tool_name", McpTelemTag(toolName),
        "result", McpTelemTag(result));

    McpTelemRecordTimer(McpMetricCatal.MCP_TOOL_QUEUE_WAIT_DURATION, tags, durationMillis);
  }

  public void McpTelemRecordToolInvocation(
      ObservCtx context,
      String toolName,
      String outcome,
      String errorType,
      long durationMillis) {

    toolInvocationsTotal.incrementAndGet();
    if ("error".equalsIgnoreCase(outcome)) {
      toolFailuresTotal.incrementAndGet();
      McpTelemToolFailure(toolName).errorCount().incrementAndGet();
    }

    Tags tags = Tags.of(
        "transport", McpTelemTag(context == null ? null : context.ObservCtxGetTranspName()),
        "tool_name", McpTelemTag(toolName),
        "outcome", McpTelemTag(outcome),
        "error_type", McpTelemErrTag(errorType));

    McpTelemIncrementCounter(McpMetricCatal.MCP_TOOL_INVOCATIONS_TOTAL, tags);
    McpTelemRecordTimer(McpMetricCatal.MCP_TOOL_DURATION, tags, durationMillis);
  }

  public void McpTelemIncrementToolRejection(
      ObservCtx context,
      String toolName,
      String reason) {

    toolRejectionsTotal.incrementAndGet();
    McpTelemToolFailure(toolName).rejectionCount().incrementAndGet();

    Tags tags = Tags.of(
        "transport", McpTelemTag(context == null ? null : context.ObservCtxGetTranspName()),
        "tool_name", McpTelemTag(toolName),
        "reason", McpTelemTag(reason));

    McpTelemIncrementCounter(McpMetricCatal.MCP_TOOL_REJECTIONS_TOTAL, tags);
  }

  public void McpTelemIncrementToolCancellation(
      ObservCtx context,
      String toolName,
      String reason) {

    toolCancellationsTotal.incrementAndGet();
    McpTelemToolFailure(toolName).cancellationCount().incrementAndGet();

    Tags tags = Tags.of(
        "transport", McpTelemTag(context == null ? null : context.ObservCtxGetTranspName()),
        "tool_name", McpTelemTag(toolName),
        "reason", McpTelemTag(reason));

    McpTelemIncrementCounter(McpMetricCatal.MCP_TOOL_CANCELLATIONS_TOTAL, tags);
  }

  public void McpTelemIncrementToolTimeout(
      ObservCtx context,
      String toolName,
      String reason) {

    toolTimeoutsTotal.incrementAndGet();
    McpTelemToolFailure(toolName).timeoutCount().incrementAndGet();

    Tags tags = Tags.of(
        "transport", McpTelemTag(context == null ? null : context.ObservCtxGetTranspName()),
        "tool_name", McpTelemTag(toolName),
        "reason", McpTelemTag(reason));

    McpTelemIncrementCounter(McpMetricCatal.MCP_TOOL_TIMEOUTS_TOTAL, tags);
  }

  public void McpTelemIncrementAuthDenied(
      String transportName,
      String reason) {

    authDenialsTotal.incrementAndGet();

    Tags tags = Tags.of(
        "transport", McpTelemTag(transportName),
        "reason", McpTelemTag(reason));

    McpTelemIncrementCounter(McpMetricCatal.MCP_AUTH_DENIALS_TOTAL, tags);
  }

  public void McpTelemIncrementSecurityDenied(
      String transportName,
      String eventName) {

    Tags tags = Tags.of(
        "transport", McpTelemTag(transportName),
        "event", McpTelemSecurityEvent(eventName));

    McpTelemIncrementCounter(McpMetricCatal.MCP_SECURITY_DENIALS_TOTAL, tags);
  }

  public void McpTelemRecordPersistence(
      ObservCtx context,
      String repository,
      String operation,
      String outcome,
      String errorType,
      long durationMillis) {

    persistenceCallsTotal.incrementAndGet();
    if (!"success".equalsIgnoreCase(outcome)) {
      persistenceFailuresTotal.incrementAndGet();
    }

    Tags tags = Tags.of(
        "transport", McpTelemTag(context == null ? null : context.ObservCtxGetTranspName()),
        "rpc_method", McpTelemTag(context == null ? null : context.ObservCtxGetRPCMet()),
        "tool_name", McpTelemTag(context == null ? null : context.ObservCtxGetToolName()),
        "repository", McpTelemTag(repository),
        "operation", McpTelemTag(operation),
        "outcome", McpTelemTag(outcome),
        "error_type", McpTelemErrTag(errorType));

    McpTelemIncrementCounter(McpMetricCatal.MCP_PERSISTENCE_CALLS_TOTAL, tags);
    McpTelemRecordTimer(McpMetricCatal.MCP_PERSISTENCE_DURATION, tags, durationMillis);
  }

  public void McpTelemIncrementTranspError(
      ObservCtx context,
      String direction,
      String errorType) {

    transportErrorsTotal.incrementAndGet();

    Tags tags = Tags.of(
        "transport", McpTelemTag(context == null ? null : context.ObservCtxGetTranspName()),
        "direction", McpTelemTag(direction),
        "error_type", McpTelemErrTag(errorType));

    McpTelemIncrementCounter(McpMetricCatal.MCP_TRANSPORT_ERRORS_TOTAL, tags);
  }

  public void McpTelemToolActiveIncrement() {
    activeToolInvocations.incrementAndGet();
  }

  public void McpTelemToolActiveDecrement() {
    activeToolInvocations.updateAndGet(current -> Math.max(0, current - 1));
  }

  public void McpTelemUpdateToolConcurr(
      int globalLimit,
      int globalAvailablePermits,
      List<mcp.server.foundation.tool_interface.ToolInvocEngine.ToolConcurrSnapshot> tools) {

    if (meterRegistry == null) {
      return;
    }

    int normalizedLimit = Math.max(0, globalLimit);
    int normalizedActive = Math.max(0, normalizedLimit - Math.max(0, globalAvailablePermits));
    globalToolConcurrLimit.set(normalizedLimit);
    globalToolConcurrActive.set(normalizedActive);

    if (tools == null) {
      return;
    }

    tools.forEach(tool -> toolConcurrMetrics.compute(
        McpTelemTag(tool.toolName()),
        (ignored, existing) -> {
          if (existing == null) {
            return ToolConcurrMetricState.register(
                meterRegistry,
                McpTelemTag(tool.toolName()),
                tool.maxConcurrency(),
                tool.activeExecutions());
          }

          existing.update(tool.maxConcurrency(), tool.activeExecutions());
          return existing;
        }));
  }

  public TelemSnapshot McpTelemGetSnapshot() {
    TimerSnapshot queueWaitSnapshot = queueWaitDurations.snapshot();

    List<ToolFailureSnapshot> topFailingTools = toolFailures.entrySet().stream()
        .map(entry -> entry.getValue().toSnapshot(entry.getKey()))
        .filter(snapshot -> snapshot.totalFailures() > 0L)
        .sorted(Comparator
            .comparingLong(ToolFailureSnapshot::totalFailures)
            .reversed()
            .thenComparing(ToolFailureSnapshot::toolName))
        .limit(5)
        .toList();

    return new TelemSnapshot(
        rpcRequestsTotal.get(),
        transportErrorsTotal.get(),
        authDenialsTotal.get(),
        toolInvocationsTotal.get(),
        activeToolInvocations.get(),
        toolTimeoutsTotal.get(),
        toolRejectionsTotal.get(),
        toolCancellationsTotal.get(),
        toolFailuresTotal.get(),
        queuePressureEventCount.get(),
        queueWaitMaxMillis.get(),
        queueWaitSnapshot.p95Millis(),
        queueWaitSnapshot.p99Millis(),
        persistenceCallsTotal.get(),
        persistenceFailuresTotal.get(),
        topFailingTools);
  }

  private void McpTelemIncrementCounter(String metricName, Tags tags) {
    McpTelemIncrementCounter(metricName, tags, 1.0d);
  }

  private void McpTelemIncrementCounter(String metricName, Tags tags, double amount) {

    if (meterRegistry == null) {
      return;
    }

    String key = McpTelemMetricKey(metricName, tags);
    counters.computeIfAbsent(
        key,
        ignored -> Counter.builder(metricName)
            .tags(tags)
            .register(meterRegistry))
        .increment(amount);
  }

  private void McpTelemRecordTimer(String metricName, Tags tags, long durationMillis) {

    if (meterRegistry == null) {
      return;
    }

    String key = McpTelemMetricKey(metricName, tags);
    timers.computeIfAbsent(
        key,
        ignored -> Timer.builder(metricName)
            .publishPercentileHistogram()
            .publishPercentiles(LATENCY_PERCENTILES)
            .tags(tags)
            .register(meterRegistry))
        .record(McpTelemDuration(durationMillis), TimeUnit.MILLISECONDS);
  }

  private void McpTelemIncrementSessionMetric(
      ObservCtx context,
      SessionMetricIncrementer incrementer) {

    if (meterRegistry == null) {
      return;
    }

    String sessionId = McpTelemSessId(context == null ? null : context.ObservCtxGetMcpSessId());
    if (sessionId == null) {
      return;
    }

    SessionMetricState state = sessionMetrics.get(sessionId);
    if (state == null) {
      return;
    }

    incrementer.increment(state);
  }

  private static String McpTelemMetricKey(String metricName, Tags tags) {
    Objects.requireNonNull(metricName, "metricName");
    StringBuilder builder = new StringBuilder(metricName);
    tags.forEach(tag -> builder.append('|').append(tag.getKey()).append('=').append(tag.getValue()));
    return builder.toString();
  }

  private static long McpTelemDuration(long durationMillis) {
    return Math.max(0L, durationMillis);
  }

  private static String McpTelemTag(String value) {
    if (value == null || value.isBlank()) {
      return FALLBACK;
    }
    return value.trim();
  }

  private static String McpTelemErrTag(String errorType) {
    if (errorType == null || errorType.isBlank()) {
      return NONE;
    }
    return errorType.trim();
  }

  private static String McpTelemSecurityEvent(String eventName) {
    Objects.requireNonNull(eventName, "eventName");

    String normalized = eventName.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("eventName must not be blank");
    }

    return normalized.toUpperCase(Locale.ROOT);
  }

  private static String McpTelemSessId(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return null;
    }
    return sessionId.trim();
  }

  private ToolFailureAccumulator McpTelemToolFailure(String toolName) {
    return toolFailures.computeIfAbsent(McpTelemTag(toolName), ignored -> new ToolFailureAccumulator());
  }

  private double McpTelemConcurrRatio(AtomicInteger active, AtomicInteger limit) {
    int limitValue = Math.max(0, limit.get());
    if (limitValue == 0) {
      return 0.0d;
    }
    return ((double) Math.max(0, active.get())) / limitValue;
  }

  public record TelemSnapshot(
      long rpcRequestsTotal,
      long transportErrorsTotal,
      long authDenialsTotal,
      long toolInvocationsTotal,
      long toolExecutionsActive,
      long toolTimeoutsTotal,
      long toolRejectionsTotal,
      long toolCancellationsTotal,
      long toolFailuresTotal,
      long queuePressureEventCount,
      long queueWaitMaxMillis,
      long queueWaitP95Millis,
      long queueWaitP99Millis,
      long persistenceCallsTotal,
      long persistenceFailuresTotal,
      List<ToolFailureSnapshot> mostFailingTools) {
  }

  public record ToolFailureSnapshot(
      String toolName,
      long totalFailures,
      long errorCount,
      long timeoutCount,
      long rejectionCount,
      long cancellationCount) {
  }

  @FunctionalInterface
  private interface SessionMetricIncrementer {
    void increment(SessionMetricState state);
  }

  private static final class SessionMetricState {

    private final MeterRegistry meterRegistry;
    private final String sessionId;
    private final AtomicInteger activeValue;
    private final AtomicLong createdTimestampSeconds;
    private final AtomicLong lastActivityTimestampSeconds;
    private final AtomicLong currentPhaseStartedTimestampSeconds;
    private final Counter requestsCounter;
    private final Counter responsesCounter;
    private final Counter successfulToolInvocationsCounter;
    private final Gauge createdTimestampGauge;
    private final Gauge lastActivityTimestampGauge;
    private final Gauge currentPhaseStartedTimestampGauge;
    private String transportName;
    private String sessionPhase;
    private Meter activeMeter;

    private SessionMetricState(
        MeterRegistry meterRegistry,
        String sessionId,
        AtomicInteger activeValue,
        AtomicLong createdTimestampSeconds,
        AtomicLong lastActivityTimestampSeconds,
        AtomicLong currentPhaseStartedTimestampSeconds,
        Counter requestsCounter,
        Counter responsesCounter,
        Counter successfulToolInvocationsCounter,
        Gauge createdTimestampGauge,
        Gauge lastActivityTimestampGauge,
        Gauge currentPhaseStartedTimestampGauge,
        String transportName,
        String sessionPhase,
        Meter activeMeter) {

      this.meterRegistry = meterRegistry;
      this.sessionId = sessionId;
      this.activeValue = activeValue;
      this.createdTimestampSeconds = createdTimestampSeconds;
      this.lastActivityTimestampSeconds = lastActivityTimestampSeconds;
      this.currentPhaseStartedTimestampSeconds = currentPhaseStartedTimestampSeconds;
      this.requestsCounter = requestsCounter;
      this.responsesCounter = responsesCounter;
      this.successfulToolInvocationsCounter = successfulToolInvocationsCounter;
      this.createdTimestampGauge = createdTimestampGauge;
      this.lastActivityTimestampGauge = lastActivityTimestampGauge;
      this.currentPhaseStartedTimestampGauge = currentPhaseStartedTimestampGauge;
      this.transportName = transportName;
      this.sessionPhase = sessionPhase;
      this.activeMeter = activeMeter;
    }

    static SessionMetricState register(
        MeterRegistry meterRegistry,
        String sessionId,
        String transportName,
        String sessionPhase,
        long createdTimestampSeconds) {

      AtomicInteger activeValue = new AtomicInteger(1);
      AtomicLong createdTimestampHolder = new AtomicLong(createdTimestampSeconds);
      AtomicLong lastActivityTimestampHolder = new AtomicLong(createdTimestampSeconds);
      AtomicLong currentPhaseStartedTimestampHolder = new AtomicLong(createdTimestampSeconds);
      Counter requestsCounter = Counter.builder(McpMetricCatal.MCP_SESSION_REQUESTS_TOTAL)
          .tag("session_id", sessionId)
          .register(meterRegistry);
      Counter responsesCounter = Counter.builder(McpMetricCatal.MCP_SESSION_RESPONSES_TOTAL)
          .tag("session_id", sessionId)
          .register(meterRegistry);
      Counter successfulToolInvocationsCounter = Counter
          .builder(McpMetricCatal.MCP_SESSION_TOOL_INVOCATIONS_SUCCESS_TOTAL)
          .tag("session_id", sessionId)
          .register(meterRegistry);
      Gauge createdTimestampGauge = Gauge.builder(
          McpMetricCatal.MCP_SESSION_CREATED_TIMESTAMP_SECONDS,
          createdTimestampHolder,
          AtomicLong::doubleValue)
          .tag("session_id", sessionId)
          .register(meterRegistry);
      Gauge lastActivityTimestampGauge = Gauge.builder(
          McpMetricCatal.MCP_SESSION_LAST_ACTIVITY_TIMESTAMP_SECONDS,
          lastActivityTimestampHolder,
          AtomicLong::doubleValue)
          .tag("session_id", sessionId)
          .register(meterRegistry);
      Gauge currentPhaseStartedTimestampGauge = Gauge.builder(
          McpMetricCatal.MCP_SESSION_CURRENT_PHASE_STARTED_TIMESTAMP_SECONDS,
          currentPhaseStartedTimestampHolder,
          AtomicLong::doubleValue)
          .tag("session_id", sessionId)
          .register(meterRegistry);
      Meter activeMeter = registerActiveMeter(
          meterRegistry,
          sessionId,
          activeValue,
          transportName,
          sessionPhase);

      return new SessionMetricState(
          meterRegistry,
          sessionId,
          activeValue,
          createdTimestampHolder,
          lastActivityTimestampHolder,
          currentPhaseStartedTimestampHolder,
          requestsCounter,
          responsesCounter,
          successfulToolInvocationsCounter,
          createdTimestampGauge,
          lastActivityTimestampGauge,
          currentPhaseStartedTimestampGauge,
          transportName,
          sessionPhase,
          activeMeter);
    }

    void updateActiveSeries(
        MeterRegistry meterRegistry,
        String transportName,
        String sessionPhase) {

      if (!Objects.equals(this.sessionPhase, sessionPhase)) {
        recordPhaseDurationNow();
        currentPhaseStartedTimestampSeconds.set(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
      }

      if (Objects.equals(this.transportName, transportName)
          && Objects.equals(this.sessionPhase, sessionPhase)) {
        markActivityNow();
        return;
      }

      if (activeMeter != null) {
        meterRegistry.remove(activeMeter);
      }

      this.transportName = transportName;
      this.sessionPhase = sessionPhase;
      this.activeMeter = registerActiveMeter(
          meterRegistry,
          sessionId,
          activeValue,
          transportName,
          sessionPhase);
      markActivityNow();
    }

    void unregister(MeterRegistry meterRegistry) {
      activeValue.set(0);
      createdTimestampSeconds.set(0L);
      lastActivityTimestampSeconds.set(0L);
      currentPhaseStartedTimestampSeconds.set(0L);
      meterRegistry.remove(requestsCounter);
      meterRegistry.remove(responsesCounter);
      meterRegistry.remove(successfulToolInvocationsCounter);
      meterRegistry.remove(createdTimestampGauge);
      meterRegistry.remove(lastActivityTimestampGauge);
      meterRegistry.remove(currentPhaseStartedTimestampGauge);
      if (activeMeter != null) {
        meterRegistry.remove(activeMeter);
      }
    }

    void requestCountIncrement() {
      markActivityNow();
      requestsCounter.increment();
    }

    void responseCountIncrement() {
      markActivityNow();
      responsesCounter.increment();
    }

    void successfulToolInvocIncrement() {
      markActivityNow();
      successfulToolInvocationsCounter.increment();
    }

    void markActivityNow() {
      lastActivityTimestampSeconds.set(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    }

    void recordPhaseDurationNow() {
      long nowSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
      long enteredAtSeconds = currentPhaseStartedTimestampSeconds.get();
      long elapsedSeconds = Math.max(0L, nowSeconds - enteredAtSeconds);
      if (elapsedSeconds <= 0L) {
        return;
      }

      Counter.builder(McpMetricCatal.MCP_SESSION_PHASE_DURATION_SECONDS_TOTAL)
          .tags(
              "session_id", sessionId,
              "phase", McpTelemTag(sessionPhase))
          .register(meterRegistry)
          .increment(elapsedSeconds);
    }

    private static Meter registerActiveMeter(
        MeterRegistry meterRegistry,
        String sessionId,
        AtomicInteger activeValue,
        String transportName,
        String sessionPhase) {

      return Gauge.builder(
          McpMetricCatal.MCP_SESSION_ACTIVE,
          activeValue,
          AtomicInteger::doubleValue)
          .tags(
              "session_id", sessionId,
              "transport", transportName,
              "session_phase", sessionPhase)
          .register(meterRegistry);
    }
  }

  private static final class ToolFailureAccumulator {

    private final AtomicLong errorCount = new AtomicLong();
    private final AtomicLong timeoutCount = new AtomicLong();
    private final AtomicLong rejectionCount = new AtomicLong();
    private final AtomicLong cancellationCount = new AtomicLong();

    AtomicLong errorCount() {
      return errorCount;
    }

    AtomicLong timeoutCount() {
      return timeoutCount;
    }

    AtomicLong rejectionCount() {
      return rejectionCount;
    }

    AtomicLong cancellationCount() {
      return cancellationCount;
    }

    ToolFailureSnapshot toSnapshot(String toolName) {
      long errors = errorCount.get();
      long timeouts = timeoutCount.get();
      long rejections = rejectionCount.get();
      long cancellations = cancellationCount.get();
      return new ToolFailureSnapshot(
          toolName,
          errors + timeouts + rejections + cancellations,
          errors,
          timeouts,
          rejections,
          cancellations);
    }
  }

  private static final class ToolConcurrMetricState {

    private final AtomicInteger activeExecutions;
    private final AtomicInteger maxConcurrency;

    private ToolConcurrMetricState(
        AtomicInteger activeExecutions,
        AtomicInteger maxConcurrency) {
      this.activeExecutions = activeExecutions;
      this.maxConcurrency = maxConcurrency;
    }

    static ToolConcurrMetricState register(
        MeterRegistry meterRegistry,
        String toolName,
        int maxConcurrency,
        int activeExecutions) {

      AtomicInteger activeHolder = new AtomicInteger(Math.max(0, activeExecutions));
      AtomicInteger limitHolder = new AtomicInteger(Math.max(0, maxConcurrency));

      Gauge.builder(McpMetricCatal.MCP_TOOL_CONCURRENCY_ACTIVE, activeHolder, AtomicInteger::get)
          .tag("tool_name", toolName)
          .register(meterRegistry);
      Gauge.builder(McpMetricCatal.MCP_TOOL_CONCURRENCY_LIMIT, limitHolder, AtomicInteger::get)
          .tag("tool_name", toolName)
          .register(meterRegistry);
      Gauge.builder(
          McpMetricCatal.MCP_TOOL_CONCURRENCY_UTILIZATION,
          new ToolConcurrMetricState(activeHolder, limitHolder),
          state -> state.ratio())
          .tag("tool_name", toolName)
          .register(meterRegistry);

      return new ToolConcurrMetricState(activeHolder, limitHolder);
    }

    void update(int maxConcurrency, int activeExecutions) {
      this.maxConcurrency.set(Math.max(0, maxConcurrency));
      this.activeExecutions.set(Math.max(0, activeExecutions));
    }

    double ratio() {
      int limitValue = Math.max(0, maxConcurrency.get());
      if (limitValue == 0) {
        return 0.0d;
      }
      return ((double) Math.max(0, activeExecutions.get())) / limitValue;
    }
  }

  private record TimerSnapshot(long p95Millis, long p99Millis) {
  }

  private static final class TimerAccumulator {

    private final TimerSampleReservoir reservoir = new TimerSampleReservoir();

    void record(long durationMillis) {
      reservoir.record(McpTelemDuration(durationMillis));
    }

    TimerSnapshot snapshot() {
      TimerSampleReservoir.PercentileSnapshot percentiles = reservoir.percentileSnapshot();
      return new TimerSnapshot(
          percentiles.p95Millis(),
          percentiles.p99Millis());
    }
  }
}
