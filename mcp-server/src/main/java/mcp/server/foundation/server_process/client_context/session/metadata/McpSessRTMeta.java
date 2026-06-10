package mcp.server.foundation.server_process.client_context.session.metadata;

import java.time.Instant;
import java.util.Objects;

import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.server_process.orchestration.RTMcpSessLifecyContract;
import mcp.server.foundation.server_process.orchestration.RTMcpSessPhase;
import mcp.server.foundation.server_process.orchestration.RTMcpSessRestoreCore;
import mcp.server.foundation.server_process.orchestration.RTMcpSessType;

/**
 * Canonical runtime envelope for one session instance.
 *
 * <p>Purpose:
 * - Keep the AGN-01 canonical runtime/session types explicit in the transport
 *   chain.
 * - Carry the minimal restore core plus the small amount of live runtime
 *   metadata needed for reconnect/resume preparation.
 *
 * <p>Notes:
 * - This is intentionally a thin carrier, not a new parallel model.
 * - Persistence is handled by the runtime-session store, not by this carrier.
 */
public record McpSessRTMeta(
    RTMcpSessLifecyContract lifecycleContract,
    RTMcpSessRestoreCore restoreCore,
    String activeTenantId,
    String resumeCapabilityId,
    Instant createdAt,
    Instant lastActivityAt,
    Instant expiresAt) {

  public McpSessRTMeta {
    lifecycleContract = Objects.requireNonNull(lifecycleContract, "lifecycleContract");
    restoreCore = Objects.requireNonNull(restoreCore, "restoreCore");
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
    lastActivityAt = Objects.requireNonNull(lastActivityAt, "lastActivityAt");
    expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
  }

  public String McpSessRTMetaGetSessionId() {
    return restoreCore.sessionId();
  }

  public ReqsAuthBinding McpSessRTMetaGetReqsAuthBinding() {
    return restoreCore.requestAuthBinding();
  }

  public RTMcpSessType McpSessRTMetaGetSessionType() {
    return restoreCore.sessionType();
  }

  public RTMcpSessPhase McpSessRTMetaGetSessionPhase() {
    return restoreCore.sessionPhase();
  }

  public long McpSessRTMetaGetSessionVersion() {
    return restoreCore.sessionVersion();
  }

  public long McpSessRTMetaGetInactivityTtlSeconds() {
    return restoreCore.inactivityTtlSeconds();
  }

  public boolean McpSessRTMetaIsDurableTarget() {
    return lifecycleContract.RTMcpSessLifecyContractIsDurableTarget();
  }

  public boolean McpSessRTMetaIsResumeSupported() {
    return lifecycleContract.RTMcpSessLifecyContractIsResumeSupported();
  }

  public boolean McpSessRTMetaRequiresActiveTenant() {
    return lifecycleContract.RTMcpSessLifecyContractRequiresActiveTenant();
  }

  public boolean McpSessRTMetaIsPreSessionAllowed() {
    return lifecycleContract.RTMcpSessLifecyContractIsPreSessionAllowed();
  }

  public boolean McpSessRTMetaTenantSwitchCreatesNewVersion() {
    return lifecycleContract.RTMcpSessLifecyContractTenantSwitchCreatesNewVersion();
  }

  public String McpSessRTMetaDescribe() {
    return lifecycleContract.RTMcpSessLifecyContractDescribe()
        + ", sessionId=" + McpSessRTMetaGetSessionId()
        + ", version=" + McpSessRTMetaGetSessionVersion()
        + ", ttl=" + McpSessRTMetaGetInactivityTtlSeconds()
        + ", activeTenantId=" + activeTenantId
        + ", resumeCapabilityId=" + resumeCapabilityId
        + ", createdAt=" + createdAt
        + ", lastActivityAt=" + lastActivityAt
        + ", expiresAt=" + expiresAt;
  }

  public McpSessRTMeta McpSessRTMetaTouch(Instant now) {
    Instant effectiveNow = Objects.requireNonNull(now, "now");
    long ttlSeconds = Math.max(1L, McpSessRTMetaGetInactivityTtlSeconds());
    return new McpSessRTMeta(
        lifecycleContract,
        restoreCore,
        activeTenantId,
        resumeCapabilityId,
        createdAt,
        effectiveNow,
        effectiveNow.plusSeconds(ttlSeconds));
  }

  public McpSessRTMeta McpSessRTMetaWithActiveTenantId(String newActiveTenantId) {
    return new McpSessRTMeta(
        lifecycleContract,
        restoreCore,
        normalize(newActiveTenantId),
        resumeCapabilityId,
        createdAt,
        lastActivityAt,
        expiresAt);
  }

  public McpSessRTMeta McpSessRTMetaWithResumeCapabilityId(String newResumeCapabilityId) {
    return new McpSessRTMeta(
        lifecycleContract,
        restoreCore,
        activeTenantId,
        normalize(newResumeCapabilityId),
        createdAt,
        lastActivityAt,
        expiresAt);
  }

  public McpSessRTMeta McpSessRTMetaWithRestoreCore(RTMcpSessRestoreCore newRestoreCore) {
    return new McpSessRTMeta(
        lifecycleContract,
        Objects.requireNonNull(newRestoreCore, "newRestoreCore"),
        activeTenantId,
        resumeCapabilityId,
        createdAt,
        lastActivityAt,
        expiresAt);
  }

  public McpSessRTMeta McpSessRTMetaWithLifecycleContract(
      RTMcpSessLifecyContract newLifecycleContract) {

    return new McpSessRTMeta(
        Objects.requireNonNull(newLifecycleContract, "newLifecycleContract"),
        restoreCore,
        activeTenantId,
        resumeCapabilityId,
        createdAt,
        lastActivityAt,
        expiresAt);
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
