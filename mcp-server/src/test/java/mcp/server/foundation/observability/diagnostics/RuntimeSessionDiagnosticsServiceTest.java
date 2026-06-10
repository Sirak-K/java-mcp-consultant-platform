package mcp.server.foundation.observability.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mcp.server.foundation.server_process.client_context.session.persistence.entity.McpSessRTEntity;
import mcp.server.foundation.server_process.client_context.session.persistence.repository.McpSessRTRepo;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class RuntimeSessionDiagnosticsServiceTest {

  private final McpSessRTRepo runtimeSessionRepository = mock(McpSessRTRepo.class);
  private final RuntimeSessionDiagnosticsService diagnosticsService =
      new RuntimeSessionDiagnosticsService(runtimeSessionRepository);

  @Test
  void sessionsExposePersistedRuntimeDiagnosticsWithoutPersistenceTypes() {
    McpSessRTEntity older = session(
        "session-older",
        "customer-42",
        "CONTEXT_BOUND",
        Instant.parse("2026-06-08T08:00:00Z"),
        Instant.parse("2026-06-08T08:30:00Z"));
    McpSessRTEntity newer = session(
        "session-newer",
        null,
        "PLATFORM_BOUND",
        Instant.parse("2026-06-08T09:00:00Z"),
        Instant.parse("2026-06-08T09:15:00Z"));
    when(runtimeSessionRepository.findAll()).thenReturn(List.of(older, newer));

    List<RuntimeSessionDiagnosticView> sessions = diagnosticsService.sessions();

    assertThat(sessions).extracting(RuntimeSessionDiagnosticView::sessionId)
        .containsExactly("session-newer", "session-older");
    assertThat(sessions.get(0).tenantType()).isEqualTo("PLATFORM_SYSTEM");
    assertThat(sessions.get(0).activeTenantId()).isEqualTo("platform_system");
    assertThat(sessions.get(1).tenantType()).isEqualTo("ASSUMED_CONTEXT");
    assertThat(sessions.get(1).activeTenantId()).isEqualTo("customer-42");
    assertThat(diagnosticsService.lastSessionActivityAt())
        .contains(Instant.parse("2026-06-08T09:15:00Z"));
  }

  @Test
  void bindingsCollapseDuplicateSnapshotsAndPreservePlatformClassification() {
    McpSessRTEntity first = session(
        "session-1",
        "platform_system",
        "PLATFORM_BOUND",
        Instant.parse("2026-06-08T08:00:00Z"),
        Instant.parse("2026-06-08T08:30:00Z"));
    McpSessRTEntity duplicate = session(
        "session-2",
        "platform_system",
        "PLATFORM_BOUND",
        Instant.parse("2026-06-08T09:00:00Z"),
        Instant.parse("2026-06-08T09:30:00Z"));
    when(runtimeSessionRepository.findAll()).thenReturn(List.of(first, duplicate));

    List<RuntimeBindingDiagnosticView> bindings = diagnosticsService.bindings();

    assertThat(bindings).hasSize(1);
    assertThat(bindings.get(0).tenantType()).isEqualTo("PLATFORM_SYSTEM");
    assertThat(bindings.get(0).activeTenantId()).isEqualTo("platform_system");
    assertThat(bindings.get(0).platformSystem()).isTrue();
  }

  private McpSessRTEntity session(
      String sessionId,
      String activeTenantId,
      String bindingStage,
      Instant createdAt,
      Instant lastActivityAt) {

    return new McpSessRTEntity(
        sessionId,
        "STREAMABLE_HTTP",
        "ACTIVE",
        1L,
        3600L,
        "ops@example.test",
        "OPS",
        "ops",
        "OPS",
        "DEV_AUTH",
        bindingStage,
        activeTenantId,
        sessionId + ":resume",
        createdAt,
        lastActivityAt,
        lastActivityAt.plusSeconds(3600L));
  }
}
