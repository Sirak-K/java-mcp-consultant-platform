package mcp.server.foundation.observability.diagnostics;

import mcp.server.foundation.observability.runtime.RTConcurrView;
import mcp.server.foundation.observability.runtime.RTStatusView;
import mcp.server.foundation.observability.runtime.RTVisibilityService;
import mcp.server.foundation.observability.runtime.ToolConcurrView;
import mcp.server.foundation.observability.runtime.ToolFailureView;
import mcp.server.foundation.server_process.client_context.session.McpSessBindingReg;
import mcp.server.foundation.server_process.client_context.session.McpSessReg;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessMetadata;
import mcp.server.foundation.tool_interface.ToolInvocEngine;
import mcp.server.foundation.tool_interface.ToolReg;

import java.util.List;
import java.util.Objects;

/**
 * Aggregates diagnostics data for operational drill-down.
 */
public final class RTDiagnService {

  private final RTVisibilityService runtimeVisibilityService;
  private final McpSessReg sessionRegistry;
  private final McpSessBindingReg bindingRegistry;
  private final ToolReg toolRegistry;
  private final ToolInvocEngine toolInvocationEngine;

  public RTDiagnService(
      RTVisibilityService runtimeVisibilityService,
      McpSessReg sessionRegistry,
      McpSessBindingReg bindingRegistry,
      ToolReg toolRegistry,
      ToolInvocEngine toolInvocationEngine) {

    this.runtimeVisibilityService = Objects.requireNonNull(runtimeVisibilityService, "runtimeVisibilityService");
    this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry");
    this.bindingRegistry = Objects.requireNonNull(bindingRegistry, "bindingRegistry");
    this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
    this.toolInvocationEngine = Objects.requireNonNull(toolInvocationEngine, "toolInvocationEngine");
  }

  public RTDiagnView RTDiagSvcGetView() {

    RTStatusView runtimeStatus = runtimeVisibilityService.RTVisibilitySvcGetRTStatus();
    RTConcurrView concurrency = runtimeStatus.coreSignals().concurrency();

    List<SessDiagnView> activeSessions = sessionRegistry.SessRegGetAllMetadata().stream()
        .map(RTDiagnService::RTDiagSess)
        .toList();

    List<BindingDiagnView> activeBindings = bindingRegistry.getSessIdsSnapshot().stream()
        .map(sessionId -> new BindingDiagnView(
            sessionId.asString(),
            bindingRegistry.getTranspName(sessionId),
            bindingRegistry.getTranspConnId(sessionId)))
        .sorted(java.util.Comparator.comparing(BindingDiagnView::mcpSessId))
        .toList();

    List<ActiveToolInvocView> activeTools = toolInvocationEngine.ToolInvocGetRunningInvocationsSnapshot().stream()
        .map(invocation -> new ActiveToolInvocView(
            invocation.requestId(),
            invocation.toolName(),
            invocation.timeoutMillis(),
            invocation.cancellable(),
            invocation.progressEnabled()))
        .toList();

    return new RTDiagnView(
        runtimeStatus.transport(),
        runtimeStatus.runtimeState(),
        runtimeStatus.activeTranspConnectionCount(),
        runtimeStatus.logicalSessionCount(),
        runtimeStatus.activeBindingCount(),
        runtimeStatus.sentinelSubscriberCount(),
        runtimeStatus.transportSignals(),
        runtimeStatus.capacityProfile(),
        toolRegistry.ToolRegSize(),
        activeSessions,
        activeBindings,
        activeTools,
        runtimeStatus.coreSignals().mostFailingTools().stream()
            .map(RTDiagnService::RTDiagToolFailure)
            .toList(),
        new RTConcurrView(
            concurrency.globalMaxConcurrency(),
            concurrency.globalAvailablePermits(),
            concurrency.tools().stream()
                .map(RTDiagnService::RTDiagToolConcurr)
                .toList()));
  }

  private static SessDiagnView RTDiagSess(McpSessMetadata metadata) {
    return new SessDiagnView(
        metadata.sessionId().asString(),
        metadata.state().name(),
        metadata.createdAt());
  }

  private static ToolFailureView RTDiagToolFailure(ToolFailureView toolFailureView) {
    return new ToolFailureView(
        toolFailureView.toolName(),
        toolFailureView.officialToolName(),
        toolFailureView.officialToolFamily(),
        toolFailureView.officialAction(),
        toolFailureView.officialToolSurface(),
        toolFailureView.totalFailures(),
        toolFailureView.errorCount(),
        toolFailureView.timeoutCount(),
        toolFailureView.rejectionCount(),
        toolFailureView.cancellationCount());
  }

  private static ToolConcurrView RTDiagToolConcurr(ToolConcurrView toolConcurrencyView) {
    return new ToolConcurrView(
        toolConcurrencyView.toolName(),
        toolConcurrencyView.officialToolName(),
        toolConcurrencyView.officialToolFamily(),
        toolConcurrencyView.officialAction(),
        toolConcurrencyView.officialToolSurface(),
        toolConcurrencyView.timeoutMillis(),
        toolConcurrencyView.maxConcurrency(),
        toolConcurrencyView.cancellable(),
        toolConcurrencyView.progressEnabled(),
        toolConcurrencyView.activeExecutions());
  }
}
