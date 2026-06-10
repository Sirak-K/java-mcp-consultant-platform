package mcp.server.foundation.observability.runtime;

import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspDiagnSignalView;
import mcp.server.foundation.observability.transport.TranspSignalModel;
import mcp.server.foundation.server_process.client_context.session.McpSessBindingReg;
import mcp.server.foundation.server_process.client_context.session.McpSessReg;
import mcp.server.foundation.server_process.status.RTStatus;
import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.http.streamable.StreamableHTTPTranspAdap;
import mcp.server.foundation.transport.websocket.WsTranspAdap;

import java.util.List;
import java.util.Objects;

final class RTStatusViewAssembler {

  private final RTMetrics runtimeMetrics;
  private final RTStatus runtimeStatus;
  private final TranspAdap transportAdapter;
  private final McpSessReg sessionRegistry;
  private final McpSessBindingReg bindingRegistry;
  private final RTVisibilityLogStatusResol logStatusResolver;
  private final RTCoreSignalsAssembler coreSignalsAssembler;

  RTStatusViewAssembler(
      RTMetrics runtimeMetrics,
      RTStatus runtimeStatus,
      TranspAdap transportAdapter,
      McpSessReg sessionRegistry,
      McpSessBindingReg bindingRegistry,
      RTVisibilityLogStatusResol logStatusResolver,
      RTCoreSignalsAssembler coreSignalsAssembler) {

    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.runtimeStatus = Objects.requireNonNull(runtimeStatus, "runtimeStatus");
    this.transportAdapter = Objects.requireNonNull(transportAdapter, "transportAdapter");
    this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry");
    this.bindingRegistry = Objects.requireNonNull(bindingRegistry, "bindingRegistry");
    this.logStatusResolver = Objects.requireNonNull(logStatusResolver, "logStatusResolver");
    this.coreSignalsAssembler = Objects.requireNonNull(coreSignalsAssembler, "coreSignalsAssembler");
  }

  RTStatusView assemble() {

    String transportName = transportAdapter.TranspAdapGetTranspName();

    return new RTStatusView(
        runtimeStatus.RTStatusGet().name(),
        transportName,
        sessionRegistry.SessRegGetActiveSessCount(),
        bindingRegistry.getActiveBindingCount(),
        activeTranspConnections(),
        sessionRegistry.SessRegGetSentinelMcpSessIds().size(),
        runtimeMetrics.RTMetricsGetCounter(TranspSignalModel.TransSigTranspErrorsMetricName(transportName)),
        runtimeMetrics.RTMetricsGetCounter(TranspSignalModel.TransSigOutbMessagesMetricName(transportName)),
        transportSignals(transportName),
        transportAdapter.TranspAdapDescribeCapacityProfile(),
        logStatusResolver.resolve(),
        coreSignalsAssembler.assemble());
  }

  private int activeTranspConnections() {
    if (transportAdapter instanceof WsTranspAdap wsTranspAdap) {
      return wsTranspAdap.WSTrGetActiveWsSessCount();
    }
    if (transportAdapter instanceof StreamableHTTPTranspAdap streamableHttpTranspAdap) {
      return streamableHttpTranspAdap.StrHTTPTrGetActiveSessCount();
    }
    return bindingRegistry.getActiveBindingCount();
  }

  private List<TranspDiagnSignalView> transportSignals(String transportName) {
    return TranspSignalModel.TransSigBuildCanonicalSignals(transportName, runtimeMetrics);
  }
}
