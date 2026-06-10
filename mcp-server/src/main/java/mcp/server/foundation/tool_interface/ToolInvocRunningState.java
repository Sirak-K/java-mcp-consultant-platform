package mcp.server.foundation.tool_interface;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

record ToolInvocRunningState(
    String requestId,
    String toolName,
    ToolExecPolicy policy,
    AtomicBoolean cancellationRequested,
    ToolExecCtx executionContext,
    Future<ToolResponse> future) {

  ToolInvocRunningState withFuture(Future<ToolResponse> future) {
    return new ToolInvocRunningState(
        requestId,
        toolName,
        policy,
        cancellationRequested,
        executionContext,
        future);
  }

  void requestCancellation() {
    cancellationRequested.set(true);
    executionContext.ToolExecCtxMarkCancellationRequested();
  }
}
