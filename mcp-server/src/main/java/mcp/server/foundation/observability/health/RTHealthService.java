package mcp.server.foundation.observability.health;

import mcp.server.foundation.server_process.status.RTStatus;
import mcp.server.foundation.transport.TranspAdap;

import java.util.Objects;

import javax.sql.DataSource;

/**
 * Computes operational health views for runtime liveness and readiness.
 */
public final class RTHealthService {

  private final RTHealthChecksAssembler healthChecksAssembler;

  public RTHealthService(
      RTStatus runtimeStatus,
      TranspAdap transportAdapter,
      DataSource dataSource) {

    this.healthChecksAssembler = new RTHealthChecksAssembler(
        Objects.requireNonNull(runtimeStatus, "runtimeStatus"),
        Objects.requireNonNull(transportAdapter, "transportAdapter"),
        dataSource);
  }

  public RTHealthView RTHealthSvcGetLiveness() {
    return healthChecksAssembler.buildLiveness();
  }

  public RTHealthView RTHealthSvcGetReadiness() {
    return healthChecksAssembler.buildReadiness();
  }

  public RTHealthView RTHealthSvcGetSummary() {
    return healthChecksAssembler.buildSummary();
  }

  public boolean RTHealthSvcIsReady() {
    return healthChecksAssembler.isReady();
  }
}
