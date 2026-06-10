package mcp.server.foundation.observability.runtime;

import java.util.List;
import java.util.Objects;

/**
 * Canonical core signals mirrored from metrics, logs and traces.
 */
public record RTCoreSignalsView(
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
    List<ToolFailureView> mostFailingTools,
    RTConcurrView concurrency,
    RTCalibrView calibration) {

  public RTCoreSignalsView {
    mostFailingTools = List.copyOf(Objects.requireNonNull(mostFailingTools, "mostFailingTools"));
    Objects.requireNonNull(concurrency, "concurrency");
    Objects.requireNonNull(calibration, "calibration");
  }
}
