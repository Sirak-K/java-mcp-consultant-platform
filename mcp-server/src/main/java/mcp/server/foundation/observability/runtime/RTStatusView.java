package mcp.server.foundation.observability.runtime;

import mcp.server.foundation.observability.transport.TranspDiagnSignalView;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical runtime status view for operational troubleshooting.
 */
public record RTStatusView(
    String runtimeState,
    String transport,
    int logicalSessionCount,
    int activeBindingCount,
    int activeTranspConnectionCount,
    int sentinelSubscriberCount,
    long transportErrorCount,
    long outboundMessageCount,
    List<TranspDiagnSignalView> transportSignals,
    Map<String, Long> capacityProfile,
    LogStatusView logs,
    RTCoreSignalsView coreSignals) {

  public RTStatusView {
    Objects.requireNonNull(runtimeState, "runtimeState");
    Objects.requireNonNull(transport, "transport");
    transportSignals = List.copyOf(Objects.requireNonNull(transportSignals, "transportSignals"));
    capacityProfile = Map.copyOf(Objects.requireNonNull(capacityProfile, "capacityProfile"));
    Objects.requireNonNull(logs, "logs");
    Objects.requireNonNull(coreSignals, "coreSignals");
  }
}
