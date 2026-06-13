package mcp.server.foundation.server_process.orchestration;

import java.util.List;

/**
 * Canonical lifecycle contract catalog for foundation runtime-session families.
 */
public final class RuntimeMcpSessionLifecycleCatalog {

  private RuntimeMcpSessionLifecycleCatalog() {
  }

  public static List<RTMcpSessLifecyContract> defaultContracts(
      long inactivityTtlSeconds) {

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
            RuntimeContractDescriptions.runtimeSessionLifecycleSummary(RTMcpSessType.MCP_SESSION)),
        new RTMcpSessLifecyContract(
            RTMcpSessType.APP_PRE_SESSION,
            RTMcpSessPhase.PRE_SESSION,
            true,
            true,
            false,
            true,
            false,
            inactivityTtlSeconds,
            RuntimeContractDescriptions.runtimeSessionLifecycleSummary(RTMcpSessType.APP_PRE_SESSION)),
        new RTMcpSessLifecyContract(
            RTMcpSessType.BOUND_BUSINESS_SESSION,
            RTMcpSessPhase.ACTIVE,
            true,
            true,
            true,
            false,
            true,
            inactivityTtlSeconds,
            RuntimeContractDescriptions.runtimeSessionLifecycleSummary(RTMcpSessType.BOUND_BUSINESS_SESSION)),
        new RTMcpSessLifecyContract(
            RTMcpSessType.PLATFORM_OPS_SESSION,
            RTMcpSessPhase.ACTIVE,
            true,
            true,
            false,
            false,
            true,
            inactivityTtlSeconds,
            RuntimeContractDescriptions.runtimeSessionLifecycleSummary(RTMcpSessType.PLATFORM_OPS_SESSION)));
  }
}
