package mcp.server.foundation.observability.runtime;

public record RTVisibilityLogRequirementPolicy(
    boolean fileSinkRequired,
    boolean auditSinkRequired,
    boolean testSinkRequired) {

  public static RTVisibilityLogRequirementPolicy RTVisibilityLogRequirementPolicyResolve(
      boolean devProfileActive,
      boolean prodProfileActive,
      boolean testProfileActive,
      boolean auditSinkEnabled,
      boolean testSinkEnabled) {

    return new RTVisibilityLogRequirementPolicy(
        devProfileActive || prodProfileActive,
        devProfileActive || (prodProfileActive && auditSinkEnabled),
        testProfileActive && testSinkEnabled);
  }
}
