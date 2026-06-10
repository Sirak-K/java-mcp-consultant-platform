package mcp.server.domain.system_operations.application;

import mcp.server.foundation.observability.diagnostics.RuntimeBindingDiagnosticView;
import mcp.server.foundation.observability.diagnostics.RuntimeSessionDiagnosticView;
import mcp.server.foundation.observability.diagnostics.RuntimeSessionDiagnosticsService;
import mcp.server.foundation.observability.triage.RTTriageService;
import mcp.server.foundation.observability.triage.RTTriageView;
import mcp.server.foundation.observability.triage.TriageSignalView;
import mcp.server.foundation.observability.triage.TriageSymptomView;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public final class RuntimeDiagnosticsQueryService {

  private final RTTriageService triageService;
  private final RuntimeSessionDiagnosticsService runtimeSessionDiagnosticsService;

  public RuntimeDiagnosticsQueryService(
      RTTriageService triageService,
      RuntimeSessionDiagnosticsService runtimeSessionDiagnosticsService) {

    this.triageService = Objects.requireNonNull(triageService, "triageService");
    this.runtimeSessionDiagnosticsService = Objects.requireNonNull(
        runtimeSessionDiagnosticsService,
        "runtimeSessionDiagnosticsService");
  }

  public List<TriageEntry> triage() {
    RTTriageView triage = triageService.RTTriageSvcGetView();
    Instant now = Instant.now();

    return triage.symptoms().stream()
        .filter(TriageSymptomView::active)
        .map(symptom -> new TriageEntry(
            null,
            now.toString(),
            "WARN",
            firstSignalContext(symptom.signals()),
            symptom.symptom() + " - " + symptom.operatorHint()))
        .toList();
  }

  public List<SessionEntry> sessions() {
    return runtimeSessionDiagnosticsService.sessions().stream()
        .map(this::toSessionEntry)
        .toList();
  }

  public List<BindingEntry> bindings() {
    return runtimeSessionDiagnosticsService.bindings().stream()
        .map(this::toBindingEntry)
        .toList();
  }

  private String firstSignalContext(List<TriageSignalView> signals) {
    return signals.stream()
        .filter(TriageSignalView::observed)
        .findFirst()
        .or(() -> signals.stream().findFirst())
        .map(signal -> signal.source() + " / " + signal.name())
        .orElse(null);
  }

  private SessionEntry toSessionEntry(RuntimeSessionDiagnosticView session) {
    return new SessionEntry(
        session.sessionId(),
        session.tenantType(),
        session.activeTenantId(),
        session.bindingStage(),
        session.createdAt() == null ? null : session.createdAt().toString());
  }

  private BindingEntry toBindingEntry(RuntimeBindingDiagnosticView binding) {
    return new BindingEntry(
        binding.tenantType(),
        binding.activeTenantId(),
        binding.bindingStage(),
        binding.platformSystem());
  }

  public record TriageEntry(Long id, String timestamp, String type, String context, String message) {
  }

  public record SessionEntry(
      String id,
      String tenantType,
      String activeTenantId,
      String bindingStage,
      String createdAt) {
  }

  public record BindingEntry(
      String tenantType,
      String activeTenantId,
      String bindingStage,
      boolean isPlatformSystem) {
  }
}
