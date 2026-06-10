package mcp.server.foundation.tool_interface;

import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMeta;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ToolExecCtx {

  private final String requestId;
  private final String toolName;
  private final ObservCtx obsContext;
  private final ReqsAuthBinding requestAuthBinding;
  private final McpSessRTMeta runtimeMeta;
  private final OperatingSurface operatingSurface;
  private final Instant deadline;
  private final boolean cancellable;
  private final boolean progressEnabled;
  private final AtomicBoolean cancellationRequested;
  private final ToolProgrPubl progressPublisher;

  ToolExecCtx(
      String requestId,
      String toolName,
      ObservCtx obsContext,
      OperatingSurface operatingSurface,
      Instant deadline,
      boolean cancellable,
      boolean progressEnabled,
      AtomicBoolean cancellationRequested,
      ToolProgrPubl progressPublisher) {

    this.requestId = requireText(requestId, "requestId");
    this.toolName = requireText(toolName, "toolName");
    this.obsContext = Objects.requireNonNull(obsContext, "obsContext");
    this.requestAuthBinding = obsContext.ObservCtxGetReqsAuthBinding();
    this.runtimeMeta = obsContext.ObservCtxGetRuntimeMeta();
    this.operatingSurface = Objects.requireNonNull(operatingSurface, "operatingSurface");
    this.deadline = deadline;
    this.cancellable = cancellable;
    this.progressEnabled = progressEnabled;
    this.cancellationRequested = Objects.requireNonNull(cancellationRequested, "cancellationRequested");
    this.progressPublisher = Objects.requireNonNull(progressPublisher, "progressPublisher");
  }

  public String ToolExecCtxGetReqsId() {
    return requestId;
  }

  public String ToolExecCtxGetToolName() {
    return toolName;
  }

  public ObservCtx ToolExecCtxGetObservCtx() {
    return obsContext;
  }

  public ReqsAuthBinding ToolExecCtxGetReqsAuthBinding() {
    return requestAuthBinding;
  }

  public McpSessRTMeta ToolExecCtxGetRuntimeMeta() {
    return runtimeMeta;
  }

  public String ToolExecCtxGetRTMcpSessTypeId() {
    return runtimeMeta == null ? null : runtimeMeta.McpSessRTMetaGetSessionType().RTMcpSessTypeGetId();
  }

  public String ToolExecCtxGetRTMcpSessPhaseId() {
    return runtimeMeta == null ? null : runtimeMeta.McpSessRTMetaGetSessionPhase().RTMcpSessPhaseGetId();
  }

  public long ToolExecCtxGetRuntimeSessionVersion() {
    return runtimeMeta == null ? 0L : runtimeMeta.McpSessRTMetaGetSessionVersion();
  }

  public long ToolExecCtxGetRuntimeInactivityTtlSeconds() {
    return runtimeMeta == null ? 0L : runtimeMeta.McpSessRTMetaGetInactivityTtlSeconds();
  }

  public String ToolExecCtxGetRuntimeResumeCapabilityId() {
    return runtimeMeta == null ? null : runtimeMeta.resumeCapabilityId();
  }

  public String ToolExecCtxGetRuntimeActiveTenantId() {
    return runtimeMeta == null ? null : runtimeMeta.activeTenantId();
  }

  public OperatingSurface ToolExecCtxGetOperatingSurface() {
    return operatingSurface;
  }

  public String ToolExecCtxGetOperatingSurfaceId() {
    return operatingSurface.OperatingSurfaceGetId();
  }

  public Instant ToolExecCtxGetDeadline() {
    return deadline;
  }

  public boolean ToolExecCtxIsCancellable() {
    return cancellable;
  }

  public boolean ToolExecCtxIsProgrEnabled() {
    return progressEnabled;
  }

  public boolean ToolExecCtxIsCancellationRequested() {
    return cancellationRequested.get() || Thread.currentThread().isInterrupted();
  }

  public void ToolExecCtxThrowIfCancelled() {
    if (ToolExecCtxIsCancellationRequested()) {
      throw new ToolExecCancelledExcep("Tool execution cancelled: " + toolName);
    }
  }

  public void ToolExecCtxReportProgr(int progressPercent, String message) {
    ToolExecCtxReportProgr(ToolProgrUpd.State.RUNNING, progressPercent, message);
  }

  public void ToolExecCtxReportProgr(String message) {
    ToolExecCtxReportProgr(ToolProgrUpd.State.RUNNING, null, message);
  }

  void ToolExecCtxMarkCancellationRequested() {
    cancellationRequested.set(true);
  }

  void ToolExecCtxReportProgr(
      ToolProgrUpd.State state,
      Integer progressPercent,
      String message) {

    if (!progressEnabled) {
      return;
    }

    progressPublisher.ToolProgrPublPublish(
        new ToolProgrUpd(
            requestId,
            toolName,
            state,
            progressPercent,
            message,
            cancellable,
            Instant.now()),
        obsContext);
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName);
    String normalized = value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
