package mcp.server.foundation.tool_interface;

import io.micrometer.observation.Observation;
import mcp.server.foundation.control_plane.PlatformControlPlaneStore;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxHolder;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.tracing.McpObservationSupport;
import mcp.server.foundation.rpc.RPCErr;
import mcp.server.foundation.rpc.RPCMappedExcep;
import mcp.server.foundation.rpc.error.ErrClassifier;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.security.request_binding.ReqsBindingComplianceDecision;
import mcp.server.foundation.security.request_binding.ReqsBindingComplianceGuard;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ToolInvocEngine
 *
 * Runtime contract:
 * - POST_INIT använder delad ExecutorService (ingen executor-per-call).
 * - POST_INIT bounded concurrency via shared + per-tool semaphores.
 * - Semaphore rejection är fail-fast backpressure i execution plane, inte
 *   klientstyrd throttling eller rate limiting.
 * - POST_INIT policies hämtas från registrerade ToolDefinition.
 */
public final class ToolInvocEngine {

  private static final String METRIC_TOOL_TOTAL_DURATION = "runtime.tool.total.duration";
  private static final String METRIC_TOOL_EXECUTION_DURATION = "runtime.tool.execution.duration";
  private static final String METRIC_TOOL_QUEUE_WAIT_DURATION = "runtime.tool.queue.wait.duration";
  private static final String METRIC_TOOL_REJECTIONS_TOTAL = "runtime.tool.rejections.total";
  private static final String METRIC_TOOL_TIMEOUTS_TOTAL = "runtime.tool.timeouts.total";
  private static final String METRIC_TOOL_CANCELLATIONS_TOTAL = "runtime.tool.cancellations.total";

  public enum ExecuteMode {
    PRE_INIT,
    POST_INIT
  }

  private final ToolReg registry;
  private final ToolInvocLogger invocationLogger;
  private final ToolProgrPubl progressPublisher;
  private final McpObservationSupport observationSupport;
  private final McpTelemMetrics telemetryMetrics;
  private final RTMetrics runtimeMetrics;
  private final ToolInvocOutcomeRecorder outcomeRecorder;
  private final ReqsBindingComplianceGuard requestBindingComplianceGuard;

  private final ExecutorService postInitExecutor;
  private final ToolInvocConcurrencyState concurrencyState;

  public ToolInvocEngine(ToolReg registry) {
    this(
        registry,
        null,
        32,
        null,
        null,
        ToolInvocDefaultWiring.progressPublisher(),
        ToolInvocDefaultWiring.observationSupport(),
        ToolInvocDefaultWiring.telemetryMetrics(),
        ToolInvocDefaultWiring.runtimeMetrics(),
        ToolInvocDefaultWiring.controlPlaneStore());
  }

  public ToolInvocEngine(
      ToolReg registry,
      ExecutorService postInitExecutor,
      int postInitMaxConcurrency) {
    this(
        registry,
        postInitExecutor,
        postInitMaxConcurrency,
        null,
        null,
        ToolInvocDefaultWiring.progressPublisher(),
        ToolInvocDefaultWiring.observationSupport(),
        ToolInvocDefaultWiring.telemetryMetrics(),
        ToolInvocDefaultWiring.runtimeMetrics(),
        ToolInvocDefaultWiring.controlPlaneStore());
  }

  public ToolInvocEngine(
      ToolReg registry,
      ExecutorService postInitExecutor,
      int postInitMaxConcurrency,
      ErrClassifier errorClassifier,
      ServerLogger logger) {
    this(
        registry,
        postInitExecutor,
        postInitMaxConcurrency,
        errorClassifier,
        logger,
        ToolInvocDefaultWiring.progressPublisher(),
        ToolInvocDefaultWiring.observationSupport(),
        ToolInvocDefaultWiring.telemetryMetrics(),
        ToolInvocDefaultWiring.runtimeMetrics(),
        ToolInvocDefaultWiring.controlPlaneStore());
  }

  public ToolInvocEngine(
      ToolReg registry,
      ExecutorService postInitExecutor,
      int postInitMaxConcurrency,
      ErrClassifier errorClassifier,
      ServerLogger logger,
      ToolProgrPubl progressPublisher) {
    this(
        registry,
        postInitExecutor,
        postInitMaxConcurrency,
        errorClassifier,
        logger,
        progressPublisher,
        ToolInvocDefaultWiring.observationSupport(),
        ToolInvocDefaultWiring.telemetryMetrics(),
        ToolInvocDefaultWiring.runtimeMetrics(),
        ToolInvocDefaultWiring.controlPlaneStore());
  }

  public ToolInvocEngine(
      ToolReg registry,
      ExecutorService postInitExecutor,
      int postInitMaxConcurrency,
      ErrClassifier errorClassifier,
      ServerLogger logger,
      ToolProgrPubl progressPublisher,
      McpObservationSupport observationSupport) {
    this(
        registry,
        postInitExecutor,
        postInitMaxConcurrency,
        errorClassifier,
        logger,
        progressPublisher,
        observationSupport,
        ToolInvocDefaultWiring.telemetryMetrics(),
        ToolInvocDefaultWiring.runtimeMetrics(),
        ToolInvocDefaultWiring.controlPlaneStore());
  }

  public ToolInvocEngine(
      ToolReg registry,
      ExecutorService postInitExecutor,
      int postInitMaxConcurrency,
      ErrClassifier errorClassifier,
      ServerLogger logger,
      ToolProgrPubl progressPublisher,
      McpObservationSupport observationSupport,
      McpTelemMetrics telemetryMetrics) {
    this(
        registry,
        postInitExecutor,
        postInitMaxConcurrency,
        errorClassifier,
        logger,
        progressPublisher,
        observationSupport,
        telemetryMetrics,
        ToolInvocDefaultWiring.runtimeMetrics(),
        ToolInvocDefaultWiring.controlPlaneStore());
  }

  public ToolInvocEngine(
      ToolReg registry,
      ExecutorService postInitExecutor,
      int postInitMaxConcurrency,
      ErrClassifier errorClassifier,
      ServerLogger logger,
      ToolProgrPubl progressPublisher,
      McpObservationSupport observationSupport,
      McpTelemMetrics telemetryMetrics,
      RTMetrics runtimeMetrics) {
    this(
        registry,
        postInitExecutor,
        postInitMaxConcurrency,
        errorClassifier,
        logger,
        progressPublisher,
        observationSupport,
        telemetryMetrics,
        runtimeMetrics,
        ToolInvocDefaultWiring.controlPlaneStore());
  }

  public ToolInvocEngine(
      ToolReg registry,
      ExecutorService postInitExecutor,
      int postInitMaxConcurrency,
      ErrClassifier errorClassifier,
      ServerLogger logger,
      ToolProgrPubl progressPublisher,
      McpObservationSupport observationSupport,
      McpTelemMetrics telemetryMetrics,
      RTMetrics runtimeMetrics,
      PlatformControlPlaneStore controlPlaneStore) {

    this(
        registry,
        postInitExecutor,
        postInitMaxConcurrency,
        errorClassifier,
        logger,
        progressPublisher,
        observationSupport,
        telemetryMetrics,
        runtimeMetrics,
        controlPlaneStore,
        ToolInvocDefaultWiring.complianceGuard());
  }

  public ToolInvocEngine(
      ToolReg registry,
      ExecutorService postInitExecutor,
      int postInitMaxConcurrency,
      ErrClassifier errorClassifier,
      ServerLogger logger,
      ToolProgrPubl progressPublisher,
      McpObservationSupport observationSupport,
      McpTelemMetrics telemetryMetrics,
      RTMetrics runtimeMetrics,
      PlatformControlPlaneStore controlPlaneStore,
      ReqsBindingComplianceGuard requestBindingComplianceGuard) {

    this.registry = Objects.requireNonNull(registry, "registry");
    this.invocationLogger = new ToolInvocLogger(logger);
    this.progressPublisher = Objects.requireNonNull(progressPublisher, "progressPublisher");
    this.observationSupport = Objects.requireNonNull(observationSupport, "observationSupport");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.outcomeRecorder = new ToolInvocOutcomeRecorder(controlPlaneStore, errorClassifier);
    this.requestBindingComplianceGuard = Objects.requireNonNull(
        requestBindingComplianceGuard,
        "requestBindingComplianceGuard");
    this.postInitExecutor = postInitExecutor;

    if (postInitMaxConcurrency <= 0) {
      throw new IllegalArgumentException("postInitMaxConcurrency must be > 0");
    }

    this.concurrencyState = new ToolInvocConcurrencyState(postInitMaxConcurrency);
    ToolInvocPublishConcurrTelem();
  }

  public ToolReg ToolInvocGetReg() {
    return registry;
  }

  public ToolResponse ToolInvocExecute(
      String method,
      ToolReqs request) {
    return ToolInvocExecute(method, request, (ObservCtx) null, OperatingSurface.MCP_DIRECT);
  }

  public ToolResponse ToolInvocExecute(
      String method,
      ToolReqs request,
      ObservCtx context) {

    return ToolInvocExecute(method, request, context, OperatingSurface.MCP_DIRECT);
  }

  public ToolResponse ToolInvocExecute(
      String method,
      ToolReqs request,
      ObservCtx context,
      OperatingSurface operatingSurface) {

    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(operatingSurface, "operatingSurface");
    ToolDefinition definition = ToolInvocResolveDefin(method, context);
    long startedAt = System.nanoTime();

    try {
      return ToolInvocExecuteDirect(definition, method, request, context, operatingSurface);
    } finally {
      runtimeMetrics.RTMetricsRecordTimerMillis(
          METRIC_TOOL_TOTAL_DURATION,
          ToolInvocRTSupport.elapsedMillis(startedAt));
    }
  }

  public ToolResponse ToolInvocExecute(
      String method,
      ToolReqs request,
      ExecuteMode mode) {
    return ToolInvocExecute(method, request, mode, (ObservCtx) null, OperatingSurface.MCP_DIRECT);
  }

  public ToolResponse ToolInvocExecute(
      String method,
      ToolReqs request,
      ExecuteMode mode,
      ObservCtx context) {

    return ToolInvocExecute(method, request, mode, context, OperatingSurface.MCP_DIRECT);
  }

  public ToolResponse ToolInvocExecute(
      String method,
      ToolReqs request,
      ExecuteMode mode,
      ObservCtx context,
      OperatingSurface operatingSurface) {

    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(operatingSurface, "operatingSurface");
    ToolDefinition definition = ToolInvocResolveDefin(method, context);
    long startedAt = System.nanoTime();

    try {
      if (mode == ExecuteMode.PRE_INIT) {
        return ToolInvocExecuteDirect(definition, method, request, context, operatingSurface);
      }

      return ToolInvocExecuteWithTimeoutResolved(
          definition,
          method,
          request,
          definition.ToolDefGetExecPolicy().timeoutMillis(),
          context,
          operatingSurface);
    } finally {
      runtimeMetrics.RTMetricsRecordTimerMillis(
          METRIC_TOOL_TOTAL_DURATION,
          ToolInvocRTSupport.elapsedMillis(startedAt));
    }
  }

  public ToolResponse ToolInvocExecuteWithTimeout(
      String method,
      ToolReqs request,
      long timeoutMillis) {
    return ToolInvocExecuteWithTimeout(method, request, timeoutMillis, (ObservCtx) null, OperatingSurface.MCP_DIRECT);
  }

  public ToolResponse ToolInvocExecuteWithTimeout(
      String method,
      ToolReqs request,
      long timeoutMillis,
      ObservCtx context) {

    return ToolInvocExecuteWithTimeout(method, request, timeoutMillis, context, OperatingSurface.MCP_DIRECT);
  }

  public ToolResponse ToolInvocExecuteWithTimeout(
      String method,
      ToolReqs request,
      long timeoutMillis,
      ObservCtx context,
      OperatingSurface operatingSurface) {

    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(operatingSurface, "operatingSurface");

    if (timeoutMillis <= 0L) {
      throw new IllegalArgumentException("timeoutMillis must be > 0");
    }

    ToolDefinition definition = ToolInvocResolveDefin(method, context);
    long startedAt = System.nanoTime();

    try {
      return ToolInvocExecuteWithTimeoutResolved(
          definition,
          method,
          request,
          timeoutMillis,
          context,
          operatingSurface);
    } finally {
      runtimeMetrics.RTMetricsRecordTimerMillis(
          METRIC_TOOL_TOTAL_DURATION,
          ToolInvocRTSupport.elapsedMillis(startedAt));
    }
  }

  private ToolResponse ToolInvocExecuteDirect(
      ToolDefinition definition,
      String method,
      ToolReqs request,
      ObservCtx context,
      OperatingSurface operatingSurface) {

    ObservCtx toolContext = ToolInvocRTSupport.buildToolCtx(context, method);
    ToolExecPolicy executionPolicy = definition.ToolDefGetExecPolicy();
    ToolExecCtx executionContext = ToolInvocCreateExecutionContext(
        definition,
        toolContext,
        executionPolicy,
        executionPolicy.timeoutMillis(),
        new AtomicBoolean(false),
        operatingSurface);

    try {
      ToolResponse response = ToolInvocInvoke(
          definition,
          request,
          toolContext,
          executionContext);
      outcomeRecorder.recordSuccess(toolContext, definition.ToolDefGetName());
      return response;
    } catch (RPCMappedExcep mapped) {
      outcomeRecorder.recordMappedCompletion(toolContext, definition.ToolDefGetName(), mapped);
      throw mapped;
    } catch (Exception ex) {
      outcomeRecorder.recordError(toolContext, definition.ToolDefGetName(), ex);
      throw ToolInvocExcepMapper.map(ex);
    }
  }

  private ToolResponse ToolInvocExecuteWithTimeoutResolved(
      ToolDefinition definition,
      String method,
      ToolReqs request,
      long timeoutMillis,
      ObservCtx context,
      OperatingSurface operatingSurface) {

    ExecutorService exec = postInitExecutor;
    if (exec == null) {
      return ToolInvocExecuteDirect(definition, method, request, context, operatingSurface);
    }

    ToolExecPolicy policy = definition.ToolDefGetExecPolicy();
    ObservCtx toolContext = ToolInvocRTSupport.buildToolCtx(context, method);
    long queueStartedAt = System.nanoTime();

    boolean globalAcquired = concurrencyState.tryAcquireGlobal();
    if (!globalAcquired) {
      ToolInvocPublishConcurrTelem();
      long queueWaitMillis = ToolInvocRTSupport.elapsedMillis(queueStartedAt);
      telemetryMetrics.McpTelemRecordToolQueueWait(
          toolContext,
          definition.ToolDefGetName(),
          "rejected_global_concurrency",
          queueWaitMillis);
      telemetryMetrics.McpTelemIncrementToolRejection(
          toolContext,
          definition.ToolDefGetName(),
          "global_concurrency");
      runtimeMetrics.RTMetricsRecordTimerMillis(METRIC_TOOL_QUEUE_WAIT_DURATION, queueWaitMillis);
      runtimeMetrics.RTMetricsIncrementCounter(METRIC_TOOL_REJECTIONS_TOTAL);
      invocationLogger.warn(
          toolContext,
          "REJECT",
          "TOOL_INVOCATION_CONCURRENCY_REJECTED",
          "ToolInvocEngine: global concurrency limit reached for " + method,
          "BACKPRESSURE_REJECTED");
      outcomeRecorder.recordRejected(
          toolContext,
          definition.ToolDefGetName(),
          "rejected_global_concurrency",
          queueWaitMillis);

      throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "Tool execution concurrency limit reached: " + method);
    }

    Semaphore toolSemaphore = concurrencyState.toolSemaphore(definition);
    boolean toolAcquired = toolSemaphore.tryAcquire();
    if (!toolAcquired) {
      concurrencyState.releaseGlobal();
      ToolInvocPublishConcurrTelem();
      long queueWaitMillis = ToolInvocRTSupport.elapsedMillis(queueStartedAt);
      telemetryMetrics.McpTelemRecordToolQueueWait(
          toolContext,
          definition.ToolDefGetName(),
          "rejected_per_tool_concurrency",
          queueWaitMillis);
      telemetryMetrics.McpTelemIncrementToolRejection(
          toolContext,
          definition.ToolDefGetName(),
          "per_tool_concurrency");
      runtimeMetrics.RTMetricsRecordTimerMillis(METRIC_TOOL_QUEUE_WAIT_DURATION, queueWaitMillis);
      runtimeMetrics.RTMetricsIncrementCounter(METRIC_TOOL_REJECTIONS_TOTAL);

      invocationLogger.warn(
          toolContext,
          "REJECT",
          "TOOL_INVOCATION_CONCURRENCY_REJECTED",
          "ToolInvocEngine: per-tool concurrency limit reached for " + method,
          "BACKPRESSURE_REJECTED");
      outcomeRecorder.recordRejected(
          toolContext,
          definition.ToolDefGetName(),
          "rejected_per_tool_concurrency",
          queueWaitMillis);

      throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "Tool execution concurrency limit reached: " + method);
    }

    String requestId = ToolInvocRTSupport.resolveReqsId(toolContext);
    AtomicBoolean cancellationRequested = new AtomicBoolean(false);
    ToolExecCtx executionContext = ToolInvocCreateExecutionContext(
        definition,
        toolContext,
        policy,
        timeoutMillis,
        cancellationRequested,
        operatingSurface);
    ToolInvocRunningState runningInvocation = new ToolInvocRunningState(
        requestId,
        method,
        policy,
        cancellationRequested,
        executionContext,
        null);
    concurrencyState.registerRunning(runningInvocation);
    ToolInvocPublishConcurrTelem();

    Future<ToolResponse> future = null;

    try {
      long queueWaitMillis = ToolInvocRTSupport.elapsedMillis(queueStartedAt);
      telemetryMetrics.McpTelemRecordToolQueueWait(
          toolContext,
          definition.ToolDefGetName(),
          "acquired",
          queueWaitMillis);
      runtimeMetrics.RTMetricsRecordTimerMillis(METRIC_TOOL_QUEUE_WAIT_DURATION, queueWaitMillis);
      executionContext.ToolExecCtxReportProgr(
          ToolProgrUpd.State.STARTED,
          0,
          "Tool execution started");

      future = exec.submit(() -> ToolInvocInvoke(
          definition,
          request,
          toolContext,
          executionContext));
      concurrencyState.registerFuture(requestId, future);
      ToolInvocPublishConcurrTelem();

      ToolResponse response = future.get(timeoutMillis, TimeUnit.MILLISECONDS);

      executionContext.ToolExecCtxReportProgr(
          ToolProgrUpd.State.SUCCEEDED,
          100,
          "Tool execution completed");
      outcomeRecorder.recordSuccess(toolContext, definition.ToolDefGetName());

      return response;
    } catch (TimeoutException timeoutException) {
      runningInvocation.requestCancellation();
      if (future != null) {
        future.cancel(true);
      }
      telemetryMetrics.McpTelemIncrementToolTimeout(
          toolContext,
          definition.ToolDefGetName(),
          "deadline_exceeded");
      runtimeMetrics.RTMetricsIncrementCounter(METRIC_TOOL_TIMEOUTS_TOTAL);

      executionContext.ToolExecCtxReportProgr(
          ToolProgrUpd.State.TIMED_OUT,
          null,
          "Tool execution timed out");

      invocationLogger.warn(
          toolContext,
          "TIMEOUT",
          "TOOL_INVOCATION_TIMED_OUT",
          "ToolInvocEngine: invocation timed out after " + timeoutMillis + " ms for " + method,
          "TIMEOUT_EXCEEDED");
      outcomeRecorder.recordTimedOut(toolContext, definition.ToolDefGetName());

      throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "Tool execution timed out after " + timeoutMillis + " ms: " + method);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      outcomeRecorder.recordError(toolContext, definition.ToolDefGetName(), "INTERRUPTED");
      throw new RPCMappedExcep(
          RPCErr.RPCErrInternalErr("Tool execution interrupted: " + method));
    } catch (CancellationException cancellationException) {
      telemetryMetrics.McpTelemIncrementToolCancellation(
          toolContext,
          definition.ToolDefGetName(),
          "future_cancelled");
      runtimeMetrics.RTMetricsIncrementCounter(METRIC_TOOL_CANCELLATIONS_TOTAL);
      executionContext.ToolExecCtxReportProgr(
          ToolProgrUpd.State.CANCELLED,
          null,
          "Tool execution cancelled");
      outcomeRecorder.recordCancelled(toolContext, definition.ToolDefGetName(), "CANCELLED");
      throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation("Tool execution cancelled: " + method);
    } catch (ExecutionException executionException) {
      Throwable cause = executionException.getCause();

      if (cause instanceof RPCMappedExcep mapped) {
        if (ToolInvocRTSupport.looksCancelled(mapped)) {
          telemetryMetrics.McpTelemIncrementToolCancellation(
              toolContext,
              definition.ToolDefGetName(),
              "mapped_exception");
          runtimeMetrics.RTMetricsIncrementCounter(METRIC_TOOL_CANCELLATIONS_TOTAL);
          executionContext.ToolExecCtxReportProgr(
              ToolProgrUpd.State.CANCELLED,
              null,
              "Tool execution cancelled");
          outcomeRecorder.recordCancelled(toolContext, definition.ToolDefGetName(), outcomeRecorder.classify(mapped));
        } else {
          executionContext.ToolExecCtxReportProgr(
              ToolProgrUpd.State.FAILED,
              null,
              "Tool execution failed");
          outcomeRecorder.recordError(toolContext, definition.ToolDefGetName(), mapped);
        }
        throw mapped;
      }

      if (cause instanceof ToolExecCancelledExcep) {
        telemetryMetrics.McpTelemIncrementToolCancellation(
            toolContext,
            definition.ToolDefGetName(),
            "tool_cancelled");
        runtimeMetrics.RTMetricsIncrementCounter(METRIC_TOOL_CANCELLATIONS_TOTAL);
        executionContext.ToolExecCtxReportProgr(
            ToolProgrUpd.State.CANCELLED,
            null,
            "Tool execution cancelled");
        outcomeRecorder.recordCancelled(toolContext, definition.ToolDefGetName(), "TOOL_CANCELLED");
        throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation("Tool execution cancelled: " + method);
      }

      executionContext.ToolExecCtxReportProgr(
          ToolProgrUpd.State.FAILED,
          null,
          "Tool execution failed");
      outcomeRecorder.recordError(
          toolContext,
          definition.ToolDefGetName(),
          cause == null ? executionException : cause);

      throw ToolInvocExcepMapper.map(cause == null ? executionException : cause);
    } finally {
      concurrencyState.removeRunning(requestId);
      toolSemaphore.release();
      concurrencyState.releaseGlobal();
      ToolInvocPublishConcurrTelem();
    }
  }

  public boolean ToolInvocCancel(String requestId) {

    Objects.requireNonNull(requestId, "requestId");

    ToolInvocRunningState invocation = concurrencyState.runningInvocation(requestId);
    if (invocation == null || !invocation.policy().cancellable()) {
      return false;
    }

    invocation.requestCancellation();
    invocation.executionContext().ToolExecCtxReportProgr(
        ToolProgrUpd.State.CANCELLED,
        null,
        "Tool execution cancellation requested");

    Future<ToolResponse> future = invocation.future();
    if (future != null) {
      future.cancel(true);
    }

    return true;
  }

  public List<ActiveInvocationSnapshot> ToolInvocGetRunningInvocationsSnapshot() {
    return concurrencyState.runningInvocationsSnapshot();
  }

  public ConcurrSnapshot ToolInvocGetConcurrSnapshot() {
    return concurrencyState.concurrencySnapshot(registry.ToolRegListDefinitions());
  }

  private void ToolInvocPublishConcurrTelem() {
    telemetryMetrics.McpTelemUpdateToolConcurr(
        concurrencyState.globalMaxConcurrency(),
        concurrencyState.globalAvailablePermits(),
        ToolInvocGetConcurrSnapshot().tools());
  }

  private ToolDefinition ToolInvocResolveDefin(String method, ObservCtx context) {
    ToolDefinition definition = registry.ToolRegGetDefin(method);
    if (definition != null) {
      return definition;
    }

    invocationLogger.warn(
        ToolInvocRTSupport.buildToolCtx(context, method),
        "VALIDATE",
        "TOOL_INVOCATION_FAILED",
        "ToolInvocEngine: unknown method " + method,
        "RPC_PROTOCOL_ERROR");

    throw new RPCMappedExcep(
        RPCErr.RPCErrMetNotFound("Unknown method: " + method));
  }

  private ToolExecCtx ToolInvocCreateExecutionContext(
      ToolDefinition definition,
      ObservCtx toolContext,
      ToolExecPolicy executionPolicy,
      long timeoutMillis,
      AtomicBoolean cancellationRequested,
      OperatingSurface operatingSurface) {

    Instant deadline = Instant.now().plusMillis(timeoutMillis);
    ToolInvocRequireBindingCompliance(toolContext, operatingSurface);

    return new ToolExecCtx(
        ToolInvocRTSupport.resolveReqsId(toolContext),
        definition.ToolDefGetName(),
        toolContext,
        operatingSurface,
        deadline,
        executionPolicy.cancellable(),
        executionPolicy.progressEnabled(),
        cancellationRequested,
        progressPublisher);
  }

  private ReqsAuthBinding ToolInvocRequireBindingCompliance(
      ObservCtx toolContext,
      OperatingSurface operatingSurface) {

    ReqsAuthBinding requestAuthBinding = toolContext == null
        ? null
        : toolContext.ObservCtxGetReqsAuthBinding();

    ReqsBindingComplianceDecision decision = requestBindingComplianceGuard
        .ReqsBindingComplianceGuardEvaluate(operatingSurface, requestAuthBinding);

    if (decision.ReqsBindingComplianceDecisionIsDenied()) {
      invocationLogger.warn(
          toolContext,
          "VALIDATE",
          "REQUEST_BINDING_DENIED",
          "ToolInvocEngine: request binding denied for "
              + operatingSurface.OperatingSurfaceGetId()
              + ": "
              + decision.ReqsBindingComplianceDecisionDescribe(),
          decision.decisionCode());
      throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "Request binding not compliant: " + decision.ReqsBindingComplianceDecisionDescribe());
    }

    if (decision.preSessionPath()) {
      invocationLogger.warn(
          toolContext,
          "VALIDATE",
          "REQUEST_BINDING_PRE_SESSION_BLOCKED",
          "ToolInvocEngine: pre-session binding cannot execute tools on "
              + operatingSurface.OperatingSurfaceGetId()
              + ": "
              + decision.ReqsBindingComplianceDecisionDescribe(),
          "PRE_SESSION_TOOL_EXECUTION_BLOCKED");
      throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "Pre-session binding cannot execute tools: " + decision.ReqsBindingComplianceDecisionDescribe());
    }

    String eventName = "REQUEST_BINDING_ALLOWED";
    if (decision.assumeTenantPath()) {
      eventName = "REQUEST_BINDING_ASSUME_TENANT_ALLOWED";
    }

    invocationLogger.info(
        toolContext,
        "VALIDATE",
        eventName,
        "ToolInvocEngine: request binding accepted for "
            + operatingSurface.OperatingSurfaceGetId()
            + ": "
            + decision.ReqsBindingComplianceDecisionDescribe());

    return requestAuthBinding;
  }

  private ToolResponse ToolInvocInvoke(
      ToolDefinition definition,
      ToolReqs request,
      ObservCtx toolContext,
      ToolExecCtx executionContext) throws Exception {

    Observation observation = observationSupport.McpObsStartToolObservation(
        toolContext,
        definition.ToolDefGetName());
    Observation.Scope scope = observation.openScope();
    ObservCtxHolder.Scope holderScope = ObservCtxHolder.ObservCtxHolderOpenScope(toolContext);
    telemetryMetrics.McpTelemToolActiveIncrement();
    long executionStartedAt = System.nanoTime();

    try {
      invocationLogger.info(
          toolContext,
          "START",
          "TOOL_INVOCATION_STARTED",
          "ToolInvocEngine: invocation started for " + definition.ToolDefGetName());

      ToolResponse response = definition.ToolDefGetImplementation()
          .execute(request, executionContext);

      invocationLogger.info(
          toolContext,
          "SUCCESS",
          "TOOL_INVOCATION_SUCCEEDED",
          "ToolInvocEngine: invocation succeeded for " + definition.ToolDefGetName(),
          ToolInvocRTSupport.durationFrom(toolContext));
      telemetryMetrics.McpTelemRecordToolInvocation(
          toolContext,
          definition.ToolDefGetName(),
          "success",
          null,
          ToolInvocRTSupport.durationFrom(toolContext));
      telemetryMetrics.McpTelemIncrementSessSuccessfulToolInvoc(toolContext);

      return response;
    } catch (RPCMappedExcep mapped) {
      String errorType = outcomeRecorder.classify(mapped);
      observationSupport.McpObsMarkErr(observation, mapped, errorType);
      invocationLogger.warn(
          toolContext,
          "FAIL",
          "TOOL_INVOCATION_FAILED",
          "ToolInvocEngine: mapped failure for " + definition.ToolDefGetName() + ": " + mapped.getMessage(),
          errorType);
      telemetryMetrics.McpTelemRecordToolInvocation(
          toolContext,
          definition.ToolDefGetName(),
          ToolInvocRTSupport.looksCancelled(mapped) ? "cancelled" : "error",
          errorType,
          ToolInvocRTSupport.durationFrom(toolContext));
      throw mapped;
    } catch (Exception ex) {
      String errorType = outcomeRecorder.classify(ex);
      observationSupport.McpObsMarkErr(observation, ex, errorType);
      invocationLogger.error(
          toolContext,
          "FAIL",
          "TOOL_INVOCATION_FAILED",
          "ToolInvocEngine: invocation failed for " + definition.ToolDefGetName() + ": " + ex.getMessage(),
          ToolInvocRTSupport.durationFrom(toolContext),
          errorType,
          ex);
      telemetryMetrics.McpTelemRecordToolInvocation(
          toolContext,
          definition.ToolDefGetName(),
          "error",
          errorType,
          ToolInvocRTSupport.durationFrom(toolContext));
      throw ex;
    } finally {
      runtimeMetrics.RTMetricsRecordTimerMillis(
          METRIC_TOOL_EXECUTION_DURATION,
          ToolInvocRTSupport.elapsedMillis(executionStartedAt));
      telemetryMetrics.McpTelemToolActiveDecrement();
      holderScope.close();
      scope.close();
      observation.stop();
    }
  }

  public record ActiveInvocationSnapshot(
      String requestId,
      String toolName,
      long timeoutMillis,
      boolean cancellable,
      boolean progressEnabled) {
  }

  public record ConcurrSnapshot(
      int globalMaxConcurrency,
      int globalAvailablePermits,
      List<ToolConcurrSnapshot> tools) {
  }

  public record ToolConcurrSnapshot(
      String toolName,
      long timeoutMillis,
      int maxConcurrency,
      boolean cancellable,
      boolean progressEnabled,
      int activeExecutions) {
  }
}
