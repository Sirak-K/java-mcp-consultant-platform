package mcp.server.foundation.observability.runtime;

import mcp.server.foundation.logging.CanonicalLogPaths;
import mcp.server.foundation.observability.health.RTHealthService;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.server_process.client_context.session.McpSessBindingReg;
import mcp.server.foundation.server_process.client_context.session.McpSessReg;
import mcp.server.foundation.server_process.status.RTStatus;
import mcp.server.foundation.tool_interface.ToolInvocEngine;
import mcp.server.foundation.transport.TranspAdap;

import java.util.Objects;

/**
 * Exposes runtime metrics, status and operator checks from existing instrumentation.
 */
public final class RTVisibilityService {

  private final RTMetricsViewAssembler metricsViewAssembler;
  private final RTStatusViewAssembler statusViewAssembler;
  private final RTOperChecksAssembler operationalChecksAssembler;

  public RTVisibilityService(
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      RTStatus runtimeStatus,
      TranspAdap transportAdapter,
      McpSessReg sessionRegistry,
      McpSessBindingReg bindingRegistry,
      RTHealthService runtimeHealthService,
      ToolInvocEngine toolInvocationEngine,
      CanonicalLogPaths canonicalLogPaths,
      boolean fileSinkEnabled,
      String serverLogPathConfig,
      String errorLogPathConfig,
      boolean auditSinkEnabled,
      String auditLogPathConfig,
      boolean testSinkEnabled,
      String testLogPathConfig,
      boolean fileSinkRequired,
      boolean auditSinkRequired,
      boolean testSinkRequired) {
    this(
        runtimeMetrics,
        telemetryMetrics,
        runtimeStatus,
        transportAdapter,
        sessionRegistry,
        bindingRegistry,
        runtimeHealthService,
        toolInvocationEngine,
        canonicalLogPaths,
        new RTTailLatencyAdvisor(),
        fileSinkEnabled,
        serverLogPathConfig,
        errorLogPathConfig,
        auditSinkEnabled,
        auditLogPathConfig,
        testSinkEnabled,
        testLogPathConfig,
        fileSinkRequired,
        auditSinkRequired,
        testSinkRequired);
  }

  RTVisibilityService(
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      RTStatus runtimeStatus,
      TranspAdap transportAdapter,
      McpSessReg sessionRegistry,
      McpSessBindingReg bindingRegistry,
      RTHealthService runtimeHealthService,
      ToolInvocEngine toolInvocationEngine,
      CanonicalLogPaths canonicalLogPaths,
      RTTailLatencyAdvisor tailLatencyAdvisor,
      boolean fileSinkEnabled,
      String serverLogPathConfig,
      String errorLogPathConfig,
      boolean auditSinkEnabled,
      String auditLogPathConfig,
      boolean testSinkEnabled,
      String testLogPathConfig,
      boolean fileSinkRequired,
      boolean auditSinkRequired,
      boolean testSinkRequired) {

    RTVisibilityLogStatusResol logStatusResolver = new RTVisibilityLogStatusResol(
        Objects.requireNonNull(canonicalLogPaths, "canonicalLogPaths"),
        fileSinkEnabled,
        serverLogPathConfig,
        errorLogPathConfig,
        auditSinkEnabled,
        auditLogPathConfig,
        testSinkEnabled,
        testLogPathConfig,
        fileSinkRequired,
        auditSinkRequired,
        testSinkRequired);

    this.metricsViewAssembler = new RTMetricsViewAssembler(
        Objects.requireNonNull(runtimeMetrics, "runtimeMetrics"));
    RTCoreSignalsAssembler coreSignalsAssembler = new RTCoreSignalsAssembler(
        Objects.requireNonNull(telemetryMetrics, "telemetryMetrics"),
        Objects.requireNonNull(toolInvocationEngine, "toolInvocationEngine"),
        Objects.requireNonNull(tailLatencyAdvisor, "tailLatencyAdvisor"));
    this.statusViewAssembler = new RTStatusViewAssembler(
        runtimeMetrics,
        Objects.requireNonNull(runtimeStatus, "runtimeStatus"),
        Objects.requireNonNull(transportAdapter, "transportAdapter"),
        Objects.requireNonNull(sessionRegistry, "sessionRegistry"),
        Objects.requireNonNull(bindingRegistry, "bindingRegistry"),
        logStatusResolver,
        coreSignalsAssembler);
    this.operationalChecksAssembler = new RTOperChecksAssembler(
        runtimeStatus,
        transportAdapter,
        Objects.requireNonNull(runtimeHealthService, "runtimeHealthService"),
        logStatusResolver);
  }

  public RTMetricsView RTVisibilitySvcGetMetrics() {
    return metricsViewAssembler.assemble();
  }

  public RTStatusView RTVisibilitySvcGetRTStatus() {
    return statusViewAssembler.assemble();
  }

  public OperChecksView RTVisibilitySvcGetOperChecks() {
    return operationalChecksAssembler.assemble();
  }
}
