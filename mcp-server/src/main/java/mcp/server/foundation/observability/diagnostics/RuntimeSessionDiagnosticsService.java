package mcp.server.foundation.observability.diagnostics;

import mcp.server.foundation.server_process.client_context.session.persistence.entity.McpSessRTEntity;
import mcp.server.foundation.server_process.client_context.session.persistence.repository.McpSessRTRepo;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only diagnostics facade over persisted runtime sessions.
 */
public final class RuntimeSessionDiagnosticsService {

  private static final String PLATFORM_SYSTEM_TENANT = "platform_system";
  private static final String PLATFORM_BOUND_STAGE = "PLATFORM_BOUND";

  private final McpSessRTRepo runtimeSessionRepository;

  public RuntimeSessionDiagnosticsService(McpSessRTRepo runtimeSessionRepository) {
    this.runtimeSessionRepository = Objects.requireNonNull(runtimeSessionRepository, "runtimeSessionRepository");
  }

  public Optional<Instant> lastSessionActivityAt() {
    return runtimeSessionRepository.findAll().stream()
        .map(McpSessRTEntity::getLastActivityAt)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder());
  }

  public List<RuntimeSessionDiagnosticView> sessions() {
    return runtimeSessionRepository.findAll().stream()
        .sorted(Comparator.comparing(McpSessRTEntity::getCreatedAt).reversed())
        .map(this::toSessionView)
        .toList();
  }

  public List<RuntimeBindingDiagnosticView> bindings() {
    return runtimeSessionRepository.findAll().stream()
        .map(this::toBindingView)
        .distinct()
        .toList();
  }

  private RuntimeSessionDiagnosticView toSessionView(McpSessRTEntity session) {
    RuntimeBindingDiagnosticView binding = toBindingView(session);
    return new RuntimeSessionDiagnosticView(
        session.getSessionId(),
        binding.tenantType(),
        binding.activeTenantId(),
        binding.bindingStage(),
        session.getCreatedAt(),
        session.getLastActivityAt());
  }

  private RuntimeBindingDiagnosticView toBindingView(McpSessRTEntity session) {
    String activeTenantId = session.getActiveTenantId();
    String bindingStage = session.getBindingStage();
    boolean platformSystem = isPlatformSystem(activeTenantId, bindingStage);
    return new RuntimeBindingDiagnosticView(
        platformSystem ? "PLATFORM_SYSTEM" : "ASSUMED_CONTEXT",
        platformSystem ? PLATFORM_SYSTEM_TENANT : activeTenantId,
        bindingStage,
        platformSystem);
  }

  private boolean isPlatformSystem(String activeTenantId, String bindingStage) {
    return activeTenantId == null
        || activeTenantId.isBlank()
        || PLATFORM_SYSTEM_TENANT.equalsIgnoreCase(activeTenantId)
        || PLATFORM_BOUND_STAGE.equalsIgnoreCase(bindingStage);
  }
}
