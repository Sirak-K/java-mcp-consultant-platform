package mcp.server.domain.system_operations.web;

public final class RuntimeDiagnosticsResponse {

  private RuntimeDiagnosticsResponse() {
  }

  public record TriageEntryResponse(Long id, String timestamp, String type, String context, String message) {
  }

  public record SessionEntryResponse(
      String id,
      String tenantType,
      String activeTenantId,
      String bindingStage,
      String createdAt) {
  }

  public record BindingEntryResponse(
      String tenantType,
      String activeTenantId,
      String bindingStage,
      boolean isPlatformSystem) {
  }
}
