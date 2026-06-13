package mcp.server.foundation.tool_interface;

import io.micrometer.observation.ObservationRegistry;
import mcp.server.foundation.control_plane.PlatformControlPlaneStore;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.tracing.McpObservationSupport;
import mcp.server.foundation.security.request_binding.ReqsBindingComplianceGuard;
import mcp.server.foundation.security.request_binding.ReqsBindingStage;
import mcp.server.foundation.security.request_binding.ReqsLifecyContract;
import mcp.server.foundation.security.request_binding.ReqsLifecyReg;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.orchestration.RuntimeContractDescriptions;

import java.util.List;

final class ToolInvocDefaultWiring {

  private ToolInvocDefaultWiring() {
  }

  static ToolProgrPubl progressPublisher() {
    return ToolProgrPubl.ToolProgrPublNoOp();
  }

  static McpObservationSupport observationSupport() {
    return new McpObservationSupport(ObservationRegistry.NOOP);
  }

  static McpTelemMetrics telemetryMetrics() {
    return McpTelemMetrics.McpTelemNoOp();
  }

  static RTMetrics runtimeMetrics() {
    return new RTMetrics();
  }

  static PlatformControlPlaneStore controlPlaneStore() {
    return PlatformControlPlaneStore.PlatformControlPlaneStoreNoOp();
  }

  static ReqsBindingComplianceGuard complianceGuard() {
    return new ReqsBindingComplianceGuard(new ReqsLifecyReg(List.of(
        new ReqsLifecyContract(
            OperatingSurface.MCP_DIRECT,
            true,
            false,
            false,
            ReqsBindingStage.PLATFORM_BOUND,
            RuntimeContractDescriptions.requestLifecycleSummary(OperatingSurface.MCP_DIRECT)),
        new ReqsLifecyContract(
            OperatingSurface.APP_ADAPTER,
            true,
            true,
            false,
            ReqsBindingStage.PRE_SESSION,
            RuntimeContractDescriptions.requestLifecycleSummary(OperatingSurface.APP_ADAPTER)),
        new ReqsLifecyContract(
            OperatingSurface.PLATFORM_OPS,
            true,
            false,
            true,
            ReqsBindingStage.PLATFORM_BOUND,
            RuntimeContractDescriptions.requestLifecycleSummary(OperatingSurface.PLATFORM_OPS)))));
  }
}
