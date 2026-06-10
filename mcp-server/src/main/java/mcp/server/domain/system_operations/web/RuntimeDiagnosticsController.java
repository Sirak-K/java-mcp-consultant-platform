package mcp.server.domain.system_operations.web;

import mcp.server.domain.system_operations.application.RuntimeDiagnosticsQueryService;
import mcp.server.domain.system_operations.application.RuntimeDiagnosticsQueryService.BindingEntry;
import mcp.server.domain.system_operations.application.RuntimeDiagnosticsQueryService.SessionEntry;
import mcp.server.domain.system_operations.application.RuntimeDiagnosticsQueryService.TriageEntry;
import mcp.server.domain.system_operations.web.RuntimeDiagnosticsResponse.BindingEntryResponse;
import mcp.server.domain.system_operations.web.RuntimeDiagnosticsResponse.SessionEntryResponse;
import mcp.server.domain.system_operations.web.RuntimeDiagnosticsResponse.TriageEntryResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/ops")
public final class RuntimeDiagnosticsController {

  private final RuntimeDiagnosticsQueryService queryService;

  public RuntimeDiagnosticsController(RuntimeDiagnosticsQueryService queryService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService");
  }

  @GetMapping("/triage")
  public List<TriageEntryResponse> triage() {
    return queryService.triage().stream()
        .map(this::toResponse)
        .toList();
  }

  @GetMapping("/diagnostics/sessions")
  public List<SessionEntryResponse> sessions() {
    return queryService.sessions().stream()
        .map(this::toResponse)
        .toList();
  }

  @GetMapping("/diagnostics/bindings")
  public List<BindingEntryResponse> bindings() {
    return queryService.bindings().stream()
        .map(this::toResponse)
        .toList();
  }

  private TriageEntryResponse toResponse(TriageEntry entry) {
    return new TriageEntryResponse(
        entry.id(),
        entry.timestamp(),
        entry.type(),
        entry.context(),
        entry.message());
  }

  private SessionEntryResponse toResponse(SessionEntry entry) {
    return new SessionEntryResponse(
        entry.id(),
        entry.tenantType(),
        entry.activeTenantId(),
        entry.bindingStage(),
        entry.createdAt());
  }

  private BindingEntryResponse toResponse(BindingEntry entry) {
    return new BindingEntryResponse(
        entry.tenantType(),
        entry.activeTenantId(),
        entry.bindingStage(),
        entry.isPlatformSystem());
  }
}
