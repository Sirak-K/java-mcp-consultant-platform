package mcp.server.foundation.observability.health;

import mcp.server.foundation.server_process.status.RTStatus;
import mcp.server.foundation.transport.TranspAdap;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

final class RTHealthChecksAssembler {

  private static final String STATUS_UP = "UP";
  private static final String STATUS_DOWN = "DOWN";

  private final RTStatus runtimeStatus;
  private final TranspAdap transportAdapter;
  private final DataSource dataSource;

  RTHealthChecksAssembler(
      RTStatus runtimeStatus,
      TranspAdap transportAdapter,
      DataSource dataSource) {

    this.runtimeStatus = Objects.requireNonNull(runtimeStatus, "runtimeStatus");
    this.transportAdapter = Objects.requireNonNull(transportAdapter, "transportAdapter");
    this.dataSource = dataSource;
  }

  RTHealthView buildLiveness() {
    return new RTHealthView(
        STATUS_UP,
        true,
        isReady(),
        runtimeStatus.RTStatusGet().name(),
        transportAdapter.TranspAdapGetTranspName(),
        Map.of("process", up("HTTP process responds to health probe")));
  }

  RTHealthView buildReadiness() {

    LinkedHashMap<String, HealthCheckView> checks = readinessChecks();
    boolean ready = allChecksUp(checks);

    return new RTHealthView(
        ready ? STATUS_UP : STATUS_DOWN,
        true,
        ready,
        runtimeStatus.RTStatusGet().name(),
        transportAdapter.TranspAdapGetTranspName(),
        checks);
  }

  RTHealthView buildSummary() {

    LinkedHashMap<String, HealthCheckView> readinessChecks = readinessChecks();
    LinkedHashMap<String, HealthCheckView> checks = new LinkedHashMap<>();
    checks.put("process", up("HTTP process responds to health probe"));
    checks.putAll(readinessChecks);

    boolean ready = allChecksUp(readinessChecks);

    return new RTHealthView(
        ready ? STATUS_UP : STATUS_DOWN,
        true,
        ready,
        runtimeStatus.RTStatusGet().name(),
        transportAdapter.TranspAdapGetTranspName(),
        checks);
  }

  boolean isReady() {
    return allChecksUp(readinessChecks());
  }

  private LinkedHashMap<String, HealthCheckView> readinessChecks() {

    LinkedHashMap<String, HealthCheckView> checks = new LinkedHashMap<>();
    checks.put("runtime", runtimeCheck());
    checks.put("transport", transportCheck());
    checks.put("database", databaseCheck());
    return checks;
  }

  private HealthCheckView runtimeCheck() {

    RTStatus.ServerState state = runtimeStatus.RTStatusGet();
    if (state == RTStatus.ServerState.RUNNING) {
      return up("Runtime state is RUNNING");
    }
    return down("Runtime state is " + state);
  }

  private HealthCheckView transportCheck() {

    String transportName = transportAdapter.TranspAdapGetTranspName();
    if (transportName == null || transportName.isBlank()) {
      return down("No active transport adapter name is available");
    }
    return up("Active transport is " + transportName);
  }

  private HealthCheckView databaseCheck() {

    if (dataSource == null) {
      return up("No DataSource bean is configured");
    }

    try (Connection connection = dataSource.getConnection()) {
      if (!connection.isValid(1)) {
        return down("Database connection validation returned false");
      }
      return up("Database connection validated");
    } catch (Exception ex) {
      return down("Database check failed: " + ex.getClass().getSimpleName());
    }
  }

  private static boolean allChecksUp(Map<String, HealthCheckView> checks) {
    return checks.values().stream().allMatch(check -> STATUS_UP.equals(check.status()));
  }

  private static HealthCheckView up(String detail) {
    return new HealthCheckView(STATUS_UP, detail);
  }

  private static HealthCheckView down(String detail) {
    return new HealthCheckView(STATUS_DOWN, detail);
  }
}
