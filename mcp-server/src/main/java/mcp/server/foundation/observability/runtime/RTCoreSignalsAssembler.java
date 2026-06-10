package mcp.server.foundation.observability.runtime;

import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.tool_interface.ToolInvocEngine;

import java.util.Objects;

final class RTCoreSignalsAssembler {

  private final McpTelemMetrics telemetryMetrics;
  private final ToolInvocEngine toolInvocationEngine;
  private final RTTailLatencyAdvisor tailLatencyAdvisor;

  RTCoreSignalsAssembler(
      McpTelemMetrics telemetryMetrics,
      ToolInvocEngine toolInvocationEngine,
      RTTailLatencyAdvisor tailLatencyAdvisor) {

    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
    this.toolInvocationEngine = Objects.requireNonNull(toolInvocationEngine, "toolInvocationEngine");
    this.tailLatencyAdvisor = Objects.requireNonNull(tailLatencyAdvisor, "tailLatencyAdvisor");
  }

  RTCoreSignalsView assemble() {

    McpTelemMetrics.TelemSnapshot snapshot = telemetryMetrics.McpTelemGetSnapshot();
    ToolInvocEngine.ConcurrSnapshot concurrencySnapshot = toolInvocationEngine.ToolInvocGetConcurrSnapshot();
    RTCalibrView calibration = tailLatencyAdvisor.RuntimeTailLatencyAdvise(snapshot, concurrencySnapshot);

    return new RTCoreSignalsView(
        snapshot.rpcRequestsTotal(),
        snapshot.transportErrorsTotal(),
        snapshot.authDenialsTotal(),
        snapshot.toolInvocationsTotal(),
        snapshot.toolExecutionsActive(),
        snapshot.toolTimeoutsTotal(),
        snapshot.toolRejectionsTotal(),
        snapshot.toolCancellationsTotal(),
        snapshot.toolFailuresTotal(),
        snapshot.queuePressureEventCount(),
        snapshot.queueWaitMaxMillis(),
        snapshot.queueWaitP95Millis(),
        snapshot.queueWaitP99Millis(),
        snapshot.persistenceCallsTotal(),
        snapshot.persistenceFailuresTotal(),
        snapshot.mostFailingTools().stream()
            .map(tool -> new ToolFailureView(
                tool.toolName(),
                tool.toolName(),
                extractFamily(tool.toolName()),
                extractAction(tool.toolName()),
                true,
                tool.totalFailures(),
                tool.errorCount(),
                tool.timeoutCount(),
                tool.rejectionCount(),
                tool.cancellationCount()))
            .toList(),
        new RTConcurrView(
            concurrencySnapshot.globalMaxConcurrency(),
            concurrencySnapshot.globalAvailablePermits(),
            concurrencySnapshot.tools().stream()
                .map(tool -> new ToolConcurrView(
                    tool.toolName(),
                    tool.toolName(),
                    extractFamily(tool.toolName()),
                    extractAction(tool.toolName()),
                    true,
                    tool.timeoutMillis(),
                    tool.maxConcurrency(),
                    tool.cancellable(),
                    tool.progressEnabled(),
                    tool.activeExecutions()))
                .toList()),
        calibration);
  }

  private static String extractFamily(String toolName) {
    int dot = toolName.indexOf('.');
    return dot > 0 ? toolName.substring(0, dot) : toolName;
  }

  private static String extractAction(String toolName) {
    int dot = toolName.indexOf('.');
    return dot >= 0 && dot < toolName.length() - 1 ? toolName.substring(dot + 1) : toolName;
  }
}
