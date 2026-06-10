package mcp.server.foundation.server_process.client_context.session.persistence.service;

import java.util.Objects;
import java.util.Optional;

import mcp.server.foundation.security.request_binding.ActiveTenantCtx;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.security.request_binding.ReqsBindingStage;
import mcp.server.foundation.security.request_binding.ReqsPrincipal;
import mcp.server.foundation.security.request_binding.ReqsPrincipalType;
import mcp.server.foundation.security.request_binding.TenantBindingSource;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMeta;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.orchestration.RTMcpSessLifecyContract;
import mcp.server.foundation.server_process.orchestration.RTMcpSessModelReg;
import mcp.server.foundation.server_process.orchestration.RTMcpSessPhase;
import mcp.server.foundation.server_process.orchestration.RTMcpSessRestoreCore;
import mcp.server.foundation.server_process.orchestration.RTMcpSessType;
import mcp.server.foundation.server_process.client_context.session.persistence.entity.McpSessRTEntity;
import mcp.server.foundation.server_process.client_context.session.persistence.repository.McpSessRTRepo;
import mcp.server.foundation.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence-backed store/mapper for runtime-session state.
 *
 * <p>This keeps runtime meta and restore core durable without introducing a
 * parallel session model.
 */
@Service
public class McpSessRTStore {

  private final McpSessRTRepo repository;
  private final RTMcpSessModelReg runtimeSessionModelRegistry;
  private final AuditService auditService;

  public McpSessRTStore(
      McpSessRTRepo repository,
      RTMcpSessModelReg runtimeSessionModelRegistry,
      AuditService auditService) {

    this.repository = Objects.requireNonNull(repository, "repository");
    this.runtimeSessionModelRegistry = Objects.requireNonNull(
        runtimeSessionModelRegistry,
        "runtimeSessionModelRegistry");
    this.auditService = Objects.requireNonNull(auditService, "auditService");
  }

  @Transactional
  public McpSessRTMeta persist(McpSessRTMeta runtimeMeta) {
    Objects.requireNonNull(runtimeMeta, "runtimeMeta");
    String sessionId = runtimeMeta.restoreCore().sessionId();
    boolean isNewSession = !repository.existsBySessionId(sessionId);
    McpSessRTEntity saved = repository.save(toEntity(runtimeMeta));
    if (isNewSession) {
      auditService.recordIdentityEvent(
          "SESSION_CREATE", "SUCCESS",
          saved.getPrincipalId(), saved.getActiveTenantId(),
          saved.getSessionId(), null,
          "sessionType=" + saved.getSessionType() + ", phase=" + saved.getSessionPhase());
    }
    return toDomain(saved);
  }

  @Transactional(readOnly = true)
  public Optional<McpSessRTMeta> load(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return Optional.empty();
    }
    Optional<McpSessRTEntity> entityOpt = repository.findBySessionId(sessionId.trim());
    entityOpt.ifPresent(entity ->
        auditService.recordIdentityEvent(
            "SESSION_RESUME", "SUCCESS",
            entity.getPrincipalId(), entity.getActiveTenantId(),
            entity.getSessionId(), null,
            "phase=" + entity.getSessionPhase() + ", version=" + entity.getSessionVersion()));
    return entityOpt.map(this::toDomain);
  }

  @Transactional(readOnly = true)
  public Optional<McpSessRTMeta> load(McpSessId sessionId) {
    if (sessionId == null) {
      return Optional.empty();
    }
    return load(sessionId.asString());
  }

  @Transactional
  public void delete(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }
    String trimmed = sessionId.trim();
    Optional<McpSessRTEntity> entityOpt = repository.findBySessionId(trimmed);
    repository.deleteBySessionId(trimmed);
    entityOpt.ifPresent(entity ->
        auditService.recordIdentityEvent(
            "SESSION_CLOSE", "SUCCESS",
            entity.getPrincipalId(), entity.getActiveTenantId(),
            entity.getSessionId(), null,
            "Session deleted"));
  }

  @Transactional
  public void delete(McpSessId sessionId) {
    if (sessionId == null) {
      return;
    }
    delete(sessionId.asString());
  }

  private McpSessRTEntity toEntity(McpSessRTMeta runtimeMeta) {
    RTMcpSessRestoreCore restoreCore = runtimeMeta.restoreCore();
    ReqsAuthBinding requestAuthBinding = restoreCore.requestAuthBinding();
    ReqsPrincipal principal = requestAuthBinding.principal();
    String activeTenantId = resolveActiveTenantId(runtimeMeta, requestAuthBinding);
    String resumeCapabilityId = normalize(runtimeMeta.resumeCapabilityId());
    if (resumeCapabilityId == null) {
      resumeCapabilityId = buildResumeCapabilityId(
          restoreCore.sessionId(),
          restoreCore.sessionType(),
          restoreCore.sessionVersion());
    }

    return new McpSessRTEntity(
        restoreCore.sessionId(),
        restoreCore.sessionType().name(),
        restoreCore.sessionPhase().name(),
        restoreCore.sessionVersion(),
        restoreCore.inactivityTtlSeconds(),
        principal.principalId(),
        principal.principalType().name(),
        principal.authorityId(),
        principal.operatingSurface().name(),
        requestAuthBinding.bindingSource().name(),
        requestAuthBinding.bindingStage().name(),
        activeTenantId,
        resumeCapabilityId,
        runtimeMeta.createdAt(),
        runtimeMeta.lastActivityAt(),
        runtimeMeta.expiresAt());
  }

  private McpSessRTMeta toDomain(McpSessRTEntity entity) {
    RTMcpSessType sessionType = RTMcpSessType.valueOf(entity.getSessionType());
    RTMcpSessLifecyContract lifecycleContract = runtimeSessionModelRegistry
        .RTMcpSessModelRegGet(sessionType);
    if (lifecycleContract == null) {
      throw new IllegalStateException("Missing runtime session contract for " + sessionType);
    }

    OperatingSurface operatingSurface = OperatingSurface.valueOf(entity.getOperatingSurface());
    ReqsPrincipal principal = new ReqsPrincipal(
        entity.getPrincipalId(),
        ReqsPrincipalType.valueOf(entity.getPrincipalType()),
        entity.getPrincipalAuthorityId(),
        operatingSurface);

    ActiveTenantCtx activeTenantContext = resolveActiveTenantCtx(entity.getActiveTenantId());
    ReqsAuthBinding requestAuthBinding = new ReqsAuthBinding(
        principal,
        activeTenantContext,
        TenantBindingSource.valueOf(entity.getBindingSource()),
        ReqsBindingStage.valueOf(entity.getBindingStage()));

    RTMcpSessRestoreCore restoreCore = new RTMcpSessRestoreCore(
        entity.getSessionId(),
        requestAuthBinding,
        sessionType,
        RTMcpSessPhase.valueOf(entity.getSessionPhase()),
        entity.getInactivityTtlSeconds(),
        entity.getSessionVersion());

    return new McpSessRTMeta(
        lifecycleContract,
        restoreCore,
        normalize(entity.getActiveTenantId()),
        normalize(entity.getResumeCapabilityId()),
        entity.getCreatedAt(),
        entity.getLastActivityAt(),
        entity.getExpiresAt());
  }

  private static String resolveActiveTenantId(
      McpSessRTMeta runtimeMeta,
      ReqsAuthBinding requestAuthBinding) {

    ActiveTenantCtx activeTenantContext = requestAuthBinding.activeTenant();
    if (activeTenantContext != null) {
      return activeTenantContext.tenantId();
    }
    return normalize(runtimeMeta.activeTenantId());
  }

  private static ActiveTenantCtx resolveActiveTenantCtx(String activeTenantId) {
    String normalized = normalize(activeTenantId);
    if (normalized == null) {
      return null;
    }
    if (ActiveTenantCtx.PLATFORM_SYSTEM_TENANT_ID.equals(normalized)) {
      return ActiveTenantCtx.ActiveTenantCtxPlatformSystem();
    }
    return ActiveTenantCtx.ActiveTenantCtxForTenant(normalized);
  }

  private static String buildResumeCapabilityId(
      String sessionId,
      RTMcpSessType sessionType,
      long sessionVersion) {

    return sessionId + ":" + sessionType.RTMcpSessTypeGetId() + ":v" + sessionVersion;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
