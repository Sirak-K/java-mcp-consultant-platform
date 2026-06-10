package mcp.server.foundation.tool_interface;

import mcp.server.foundation.control_plane.PlatformControlPlaneStore;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.rpc.RPCMappedExcep;
import mcp.server.foundation.rpc.error.ErrClassifier;

import java.util.Objects;

final class ToolInvocOutcomeRecorder {

  private final PlatformControlPlaneStore controlPlaneStore;
  private final ErrClassifier errorClassifier;

  ToolInvocOutcomeRecorder(
      PlatformControlPlaneStore controlPlaneStore,
      ErrClassifier errorClassifier) {
    this.controlPlaneStore = Objects.requireNonNull(controlPlaneStore, "controlPlaneStore");
    this.errorClassifier = errorClassifier;
  }

  void record(
      ObservCtx context,
      String toolName,
      String outcome,
      String errorType,
      Long durationMs) {

    controlPlaneStore.PlatformControlPlaneStoreRecordToolOutcome(
        context,
        toolName,
        outcome,
        errorType,
        durationMs);
  }

  void recordSuccess(ObservCtx context, String toolName) {
    record(context, toolName, "success", null, ToolInvocRTSupport.durationFrom(context));
  }

  void recordError(ObservCtx context, String toolName, Throwable throwable) {
    record(context, toolName, "error", classify(throwable), ToolInvocRTSupport.durationFrom(context));
  }

  void recordError(ObservCtx context, String toolName, String errorType) {
    record(context, toolName, "error", errorType, ToolInvocRTSupport.durationFrom(context));
  }

  void recordMappedCompletion(ObservCtx context, String toolName, RPCMappedExcep mapped) {
    record(
        context,
        toolName,
        ToolInvocRTSupport.looksCancelled(mapped) ? "cancelled" : "error",
        classify(mapped),
        ToolInvocRTSupport.durationFrom(context));
  }

  void recordCancelled(ObservCtx context, String toolName, String errorType) {
    record(context, toolName, "cancelled", errorType, ToolInvocRTSupport.durationFrom(context));
  }

  void recordTimedOut(ObservCtx context, String toolName) {
    record(context, toolName, "timed_out", "TIMEOUT_EXCEEDED", ToolInvocRTSupport.durationFrom(context));
  }

  void recordRejected(ObservCtx context, String toolName, String rejectionOutcome, long queueWaitMillis) {
    record(context, toolName, rejectionOutcome, "BACKPRESSURE_REJECTED", queueWaitMillis);
  }

  String classify(Throwable throwable) {
    if (errorClassifier == null || throwable == null) {
      return null;
    }
    return errorClassifier.ErrClassifierClassify(throwable).name();
  }
}
