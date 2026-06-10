package mcp.server.foundation.observability.runtime;

import mcp.server.foundation.observability.health.HealthCheckView;
import mcp.server.foundation.observability.health.RTHealthService;
import mcp.server.foundation.server_process.status.RTStatus;
import mcp.server.foundation.transport.TranspAdap;

import java.util.List;
import java.util.Objects;

final class RTOperChecksAssembler {

  private final RTStatus runtimeStatus;
  private final TranspAdap transportAdapter;
  private final RTHealthService runtimeHealthService;
  private final RTVisibilityLogStatusResol logStatusResolver;

  RTOperChecksAssembler(
      RTStatus runtimeStatus,
      TranspAdap transportAdapter,
      RTHealthService runtimeHealthService,
      RTVisibilityLogStatusResol logStatusResolver) {

    this.runtimeStatus = Objects.requireNonNull(runtimeStatus, "runtimeStatus");
    this.transportAdapter = Objects.requireNonNull(transportAdapter, "transportAdapter");
    this.runtimeHealthService = Objects.requireNonNull(runtimeHealthService, "runtimeHealthService");
    this.logStatusResolver = Objects.requireNonNull(logStatusResolver, "logStatusResolver");
  }

  OperChecksView assemble() {

    HealthCheckView databaseCheck = runtimeHealthService.RTHealthSvcGetReadiness().checks().get("database");
    LogStatusView logStatus = logStatusResolver.resolve();

    List<OperCheckView> preStart = List.of(
        new OperCheckView(
            "transport_selected",
            transportAdapter.TranspAdapGetTranspName() != null && !transportAdapter.TranspAdapGetTranspName().isBlank(),
            "Aktiv transport: " + transportAdapter.TranspAdapGetTranspName()),
        new OperCheckView(
            "logging_destination_ready",
            logStatus.LogStatusViewIsLoggingDestinationReady(),
            logStatusResolver.loggingDestinationDetail(logStatus)),
        new OperCheckView(
            "database_reachable",
            "UP".equals(databaseCheck.status()),
            databaseCheck.detail()));

    List<OperCheckView> postStart = List.of(
        new OperCheckView(
            "liveness_up",
            runtimeHealthService.RTHealthSvcGetLiveness().live(),
            "Kontrollera /ops/health/live"),
        new OperCheckView(
            "readiness_up",
            runtimeHealthService.RTHealthSvcIsReady(),
            "Kontrollera /ops/health/ready"),
        new OperCheckView(
            "runtime_running",
            runtimeStatus.RTStatusGet() == RTStatus.ServerState.RUNNING,
            "Aktuellt runtime state: " + runtimeStatus.RTStatusGet()),
        new OperCheckView(
            "canonical_logs_present",
            logStatus.LogStatusViewHasOperLogsReady(),
            logStatusResolver.operationalLogsDetail(logStatus)),
        new OperCheckView(
            "audit_log_present_when_required",
            !logStatus.auditLogRequired() || logStatus.auditLogExists(),
            logStatusResolver.auditLogDetail(logStatus)));

    return new OperChecksView(preStart, postStart);
  }
}
