package mcp.server.foundation.server_process.orchestration;

import java.util.List;

/**
 * Canonical lifecycle contract catalog for foundation runtime-session families.
 */
public final class RuntimeMcpSessionLifecycleCatalog {

  private RuntimeMcpSessionLifecycleCatalog() {
  }

  public static List<RTMcpSessLifecyContract> defaultContracts(
      long inactivityTtlSeconds,
      RuntimeContractDescriptionCatalogService descriptionCatalog) {

    return List.of(
        new RTMcpSessLifecyContract(
            RTMcpSessType.MCP_SESSION,
            RTMcpSessPhase.PRE_INIT,
            true,
            true,
            false,
            false,
            false,
            inactivityTtlSeconds,
            descriptionCatalog.runtimeSessionLifecycleSummary(RTMcpSessType.MCP_SESSION)),
        new RTMcpSessLifecyContract(
            RTMcpSessType.APP_PRE_SESSION,
            RTMcpSessPhase.PRE_SESSION,
            true,
            true,
            false,
            true,
            false,
            inactivityTtlSeconds,
            descriptionCatalog.runtimeSessionLifecycleSummary(RTMcpSessType.APP_PRE_SESSION)),
        new RTMcpSessLifecyContract(
            RTMcpSessType.BOUND_BUSINESS_SESSION,
            RTMcpSessPhase.ACTIVE,
            true,
            true,
            true,
            false,
            true,
            inactivityTtlSeconds,
            descriptionCatalog.runtimeSessionLifecycleSummary(RTMcpSessType.BOUND_BUSINESS_SESSION)),
        new RTMcpSessLifecyContract(
            RTMcpSessType.PLATFORM_OPS_SESSION,
            RTMcpSessPhase.ACTIVE,
            true,
            true,
            false,
            false,
            true,
            inactivityTtlSeconds,
            descriptionCatalog.runtimeSessionLifecycleSummary(RTMcpSessType.PLATFORM_OPS_SESSION)));
  }
}
