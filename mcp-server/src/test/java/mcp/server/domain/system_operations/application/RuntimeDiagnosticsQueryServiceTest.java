package mcp.server.domain.system_operations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mcp.server.domain.system_operations.application.RuntimeDiagnosticsQueryService.BindingEntry;
import mcp.server.domain.system_operations.application.RuntimeDiagnosticsQueryService.SessionEntry;
import mcp.server.domain.system_operations.application.RuntimeDiagnosticsQueryService.TriageEntry;
import mcp.server.foundation.observability.diagnostics.RuntimeBindingDiagnosticView;
import mcp.server.foundation.observability.diagnostics.RuntimeSessionDiagnosticView;
import mcp.server.foundation.observability.diagnostics.RuntimeSessionDiagnosticsService;
import mcp.server.foundation.observability.triage.RTTriageService;
import mcp.server.foundation.observability.triage.RTTriageView;
import mcp.server.foundation.observability.triage.TriageSignalView;
import mcp.server.foundation.observability.triage.TriageSymptomView;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class RuntimeDiagnosticsQueryServiceTest {

  private final RTTriageService triageService = mock(RTTriageService.class);
  private final RuntimeSessionDiagnosticsService runtimeSessionDiagnosticsService =
      mock(RuntimeSessionDiagnosticsService.class);
  private final RuntimeDiagnosticsQueryService service =
      new RuntimeDiagnosticsQueryService(triageService, runtimeSessionDiagnosticsService);

  @Test
  void triageReturnsOnlyActiveSymptomsWithObservedSignalContext() {
    when(triageService.RTTriageSvcGetView()).thenReturn(new RTTriageView(List.of(
        new TriageSymptomView(
            "Inactive symptom",
            false,
            "Ignore",
            List.of(new TriageSignalView("runtime", "inactive", true, "not active"))),
        new TriageSymptomView(
            "Queue pressure",
            true,
            "Reduce concurrency",
            List.of(
                new TriageSignalView("runtime", "supporting", false, "supporting signal"),
                new TriageSignalView("metrics", "queue-pressure", true, "observed signal"))))));

    List<TriageEntry> entries = service.triage();

    assertThat(entries).hasSize(1);
    TriageEntry entry = entries.get(0);
    assertThat(entry.id()).isNull();
    assertThat(entry.type()).isEqualTo("WARN");
    assertThat(entry.context()).isEqualTo("metrics / queue-pressure");
    assertThat(entry.message()).isEqualTo("Queue pressure - Reduce concurrency");
    assertThatCode(() -> Instant.parse(entry.timestamp())).doesNotThrowAnyException();
  }

  @Test
  void sessionsAndBindingsExposeFoundationDiagnosticsWithoutFoundationPersistenceTypes() {
    when(runtimeSessionDiagnosticsService.sessions()).thenReturn(List.of(
        new RuntimeSessionDiagnosticView(
            "session-1",
            "PLATFORM_SYSTEM",
            "platform_system",
            "PLATFORM_BOUND",
            Instant.parse("2026-06-08T08:00:00Z"),
            Instant.parse("2026-06-08T08:30:00Z")),
        new RuntimeSessionDiagnosticView(
            "session-2",
            "ASSUMED_CONTEXT",
            "customer-42",
            "CONTEXT_BOUND",
            null,
            null)));
    when(runtimeSessionDiagnosticsService.bindings()).thenReturn(List.of(
        new RuntimeBindingDiagnosticView("PLATFORM_SYSTEM", "platform_system", "PLATFORM_BOUND", true)));

    List<SessionEntry> sessions = service.sessions();
    List<BindingEntry> bindings = service.bindings();

    assertThat(sessions).extracting(SessionEntry::id).containsExactly("session-1", "session-2");
    assertThat(sessions.get(0).createdAt()).isEqualTo("2026-06-08T08:00:00Z");
    assertThat(sessions.get(1).createdAt()).isNull();
    assertThat(sessions.get(1).tenantType()).isEqualTo("ASSUMED_CONTEXT");
    assertThat(sessions.get(1).activeTenantId()).isEqualTo("customer-42");
    assertThat(bindings).hasSize(1);
    assertThat(bindings.get(0).isPlatformSystem()).isTrue();
    assertThat(bindings.get(0).bindingStage()).isEqualTo("PLATFORM_BOUND");
  }
}
