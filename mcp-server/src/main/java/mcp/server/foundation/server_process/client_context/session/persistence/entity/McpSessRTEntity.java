package mcp.server.foundation.server_process.client_context.session.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Durable runtime-session row.
 *
 * <p>This stores the minimal restore core plus the live runtime metadata needed
 * to resume a session without introducing a parallel session model.
 */
@Entity
@Table(schema = "runtime", name = "mcp_runtime_session")
public class McpSessRTEntity {

  @Id
  @Column(name = "session_id", nullable = false, length = 64, updatable = false)
  private String sessionId;

  @Column(name = "session_type", nullable = false, length = 64)
  private String sessionType;

  @Column(name = "session_phase", nullable = false, length = 64)
  private String sessionPhase;

  @Column(name = "session_version", nullable = false)
  private long sessionVersion;

  @Column(name = "inactivity_ttl_seconds", nullable = false)
  private long inactivityTtlSeconds;

  @Column(name = "principal_id", nullable = false, length = 128)
  private String principalId;

  @Column(name = "principal_type", nullable = false, length = 64)
  private String principalType;

  @Column(name = "principal_authority_id", nullable = false, length = 128)
  private String principalAuthorityId;

  @Column(name = "operating_surface", nullable = false, length = 64)
  private String operatingSurface;

  @Column(name = "binding_source", nullable = false, length = 64)
  private String bindingSource;

  @Column(name = "binding_stage", nullable = false, length = 64)
  private String bindingStage;

  @Column(name = "active_tenant_id", length = 128)
  private String activeTenantId;

  @Column(name = "resume_capability_id", length = 256)
  private String resumeCapabilityId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_activity_at", nullable = false)
  private Instant lastActivityAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  protected McpSessRTEntity() {
  }

  public McpSessRTEntity(
      String sessionId,
      String sessionType,
      String sessionPhase,
      long sessionVersion,
      long inactivityTtlSeconds,
      String principalId,
      String principalType,
      String principalAuthorityId,
      String operatingSurface,
      String bindingSource,
      String bindingStage,
      String activeTenantId,
      String resumeCapabilityId,
      Instant createdAt,
      Instant lastActivityAt,
      Instant expiresAt) {

    this.sessionId = requireText(sessionId, "sessionId");
    this.sessionType = requireText(sessionType, "sessionType");
    this.sessionPhase = requireText(sessionPhase, "sessionPhase");
    this.sessionVersion = sessionVersion;
    this.inactivityTtlSeconds = inactivityTtlSeconds;
    this.principalId = requireText(principalId, "principalId");
    this.principalType = requireText(principalType, "principalType");
    this.principalAuthorityId = requireText(principalAuthorityId, "principalAuthorityId");
    this.operatingSurface = requireText(operatingSurface, "operatingSurface");
    this.bindingSource = requireText(bindingSource, "bindingSource");
    this.bindingStage = requireText(bindingStage, "bindingStage");
    this.activeTenantId = normalize(activeTenantId);
    this.resumeCapabilityId = normalize(resumeCapabilityId);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.lastActivityAt = Objects.requireNonNull(lastActivityAt, "lastActivityAt");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getSessionType() {
    return sessionType;
  }

  public void setSessionType(String sessionType) {
    this.sessionType = sessionType;
  }

  public String getSessionPhase() {
    return sessionPhase;
  }

  public void setSessionPhase(String sessionPhase) {
    this.sessionPhase = sessionPhase;
  }

  public long getSessionVersion() {
    return sessionVersion;
  }

  public void setSessionVersion(long sessionVersion) {
    this.sessionVersion = sessionVersion;
  }

  public long getInactivityTtlSeconds() {
    return inactivityTtlSeconds;
  }

  public void setInactivityTtlSeconds(long inactivityTtlSeconds) {
    this.inactivityTtlSeconds = inactivityTtlSeconds;
  }

  public String getPrincipalId() {
    return principalId;
  }

  public void setPrincipalId(String principalId) {
    this.principalId = principalId;
  }

  public String getPrincipalType() {
    return principalType;
  }

  public void setPrincipalType(String principalType) {
    this.principalType = principalType;
  }

  public String getPrincipalAuthorityId() {
    return principalAuthorityId;
  }

  public void setPrincipalAuthorityId(String principalAuthorityId) {
    this.principalAuthorityId = principalAuthorityId;
  }

  public String getOperatingSurface() {
    return operatingSurface;
  }

  public void setOperatingSurface(String operatingSurface) {
    this.operatingSurface = operatingSurface;
  }

  public String getBindingSource() {
    return bindingSource;
  }

  public void setBindingSource(String bindingSource) {
    this.bindingSource = bindingSource;
  }

  public String getBindingStage() {
    return bindingStage;
  }

  public void setBindingStage(String bindingStage) {
    this.bindingStage = bindingStage;
  }

  public String getActiveTenantId() {
    return activeTenantId;
  }

  public void setActiveTenantId(String activeTenantId) {
    this.activeTenantId = activeTenantId;
  }

  public String getResumeCapabilityId() {
    return resumeCapabilityId;
  }

  public void setResumeCapabilityId(String resumeCapabilityId) {
    this.resumeCapabilityId = resumeCapabilityId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastActivityAt() {
    return lastActivityAt;
  }

  public void setLastActivityAt(Instant lastActivityAt) {
    this.lastActivityAt = lastActivityAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName);
    String normalized = value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
