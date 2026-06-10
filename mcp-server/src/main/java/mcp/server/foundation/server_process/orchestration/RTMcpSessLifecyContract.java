package mcp.server.foundation.server_process.orchestration;

import java.util.Objects;

/**
 * Compact runtime/session contract for one canonical session family.
 */
public final class RTMcpSessLifecyContract {

  private final RTMcpSessType sessionType;
  private final RTMcpSessPhase initialPhase;
  private final boolean durableTarget;
  private final boolean resumeSupported;
  private final boolean requiresActiveTenant;
  private final boolean preSessionAllowed;
  private final boolean tenantSwitchCreatesNewVersion;
  private final long inactivityTtlSeconds;
  private final String summary;

  public RTMcpSessLifecyContract(
      RTMcpSessType sessionType,
      RTMcpSessPhase initialPhase,
      boolean durableTarget,
      boolean resumeSupported,
      boolean requiresActiveTenant,
      boolean preSessionAllowed,
      boolean tenantSwitchCreatesNewVersion,
      long inactivityTtlSeconds,
      String summary) {

    this.sessionType = Objects.requireNonNull(sessionType, "sessionType");
    this.initialPhase = Objects.requireNonNull(initialPhase, "initialPhase");
    this.durableTarget = durableTarget;
    this.resumeSupported = resumeSupported;
    this.requiresActiveTenant = requiresActiveTenant;
    this.preSessionAllowed = preSessionAllowed;
    this.tenantSwitchCreatesNewVersion = tenantSwitchCreatesNewVersion;
    if (inactivityTtlSeconds <= 0L) {
      throw new IllegalArgumentException("inactivityTtlSeconds must be > 0");
    }
    this.inactivityTtlSeconds = inactivityTtlSeconds;
    this.summary = requireText(summary, "summary");
  }

  public RTMcpSessType RTMcpSessLifecyContractGetSessionType() {
    return sessionType;
  }

  public RTMcpSessPhase RTMcpSessLifecyContractGetInitialPhase() {
    return initialPhase;
  }

  public boolean RTMcpSessLifecyContractIsDurableTarget() {
    return durableTarget;
  }

  public boolean RTMcpSessLifecyContractIsResumeSupported() {
    return resumeSupported;
  }

  public boolean RTMcpSessLifecyContractRequiresActiveTenant() {
    return requiresActiveTenant;
  }

  public boolean RTMcpSessLifecyContractIsPreSessionAllowed() {
    return preSessionAllowed;
  }

  public boolean RTMcpSessLifecyContractTenantSwitchCreatesNewVersion() {
    return tenantSwitchCreatesNewVersion;
  }

  public long RTMcpSessLifecyContractGetInactivityTtlSeconds() {
    return inactivityTtlSeconds;
  }

  public String RTMcpSessLifecyContractGetSummary() {
    return summary;
  }

  public String RTMcpSessLifecyContractDescribe() {
    return sessionType.RTMcpSessTypeGetId()
        + "[initialPhase=" + initialPhase.RTMcpSessPhaseGetId()
        + ", durableTarget=" + durableTarget
        + ", resumeSupported=" + resumeSupported
        + ", requiresActiveTenant=" + requiresActiveTenant
        + ", preSessionAllowed=" + preSessionAllowed
        + ", tenantSwitchCreatesNewVersion=" + tenantSwitchCreatesNewVersion
        + ", inactivityTtlSeconds=" + inactivityTtlSeconds
        + "]";
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName);
    String normalized = value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
