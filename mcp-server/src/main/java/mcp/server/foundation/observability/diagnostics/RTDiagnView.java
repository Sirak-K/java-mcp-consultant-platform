package mcp.server.foundation.observability.diagnostics;

import mcp.server.foundation.observability.transport.TranspDiagnSignalView;
import mcp.server.foundation.observability.runtime.RTConcurrView;
import mcp.server.foundation.observability.runtime.ToolFailureView;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregated diagnostics view for incidents and runtime drill-down.
 */
public record RTDiagnView(
    String transport,
    String runtimeState,
    int activeTranspConnectionCount,
    int activeSessionCount,
    int activeBindingCount,
    int sentinelSubscriberCount,
    List<TranspDiagnSignalView> transportSignals,
    Map<String, Long> capacityProfile,
    int registeredToolCount,
    List<SessDiagnView> activeSessions,
    List<BindingDiagnView> activeBindings,
    List<ActiveToolInvocView> activeTools,
    List<ToolFailureView> mostFailingTools,
    RTConcurrView concurrency) {

  public RTDiagnView {
    Objects.requireNonNull(transport, "transport");
    Objects.requireNonNull(runtimeState, "runtimeState");
    transportSignals = List.copyOf(Objects.requireNonNull(transportSignals, "transportSignals"));
    capacityProfile = Map.copyOf(Objects.requireNonNull(capacityProfile, "capacityProfile"));
    activeSessions = List.copyOf(Objects.requireNonNull(activeSessions, "activeSessions"));
    activeBindings = List.copyOf(Objects.requireNonNull(activeBindings, "activeBindings"));
    activeTools = List.copyOf(Objects.requireNonNull(activeTools, "activeTools"));
    mostFailingTools = List.copyOf(Objects.requireNonNull(mostFailingTools, "mostFailingTools"));
    Objects.requireNonNull(concurrency, "concurrency");
  }
}
