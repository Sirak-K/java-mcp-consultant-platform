package mcp.server.foundation.observability.triage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

class RuntimeTriageSymptomCatalogServiceTest {

  private final RuntimeTriageSymptomCatalogService catalogService =
      new RuntimeTriageSymptomCatalogService(new ProjectCatalogJsonLoader(new ObjectMapper()));

  @Test
  void usesRuntimeTriageKeysAsStableExposedIdentifiers() {
    TriageSignalView signal = catalogService.signal(
        "readiness_down",
        "ready_status_down",
        true,
        Map.of("status", "DOWN"));

    TriageSymptomView symptom = catalogService.symptom(
        "readiness_down",
        true,
        List.of(signal));

    assertThat(signal.source()).isEqualTo("health");
    assertThat(signal.name()).isEqualTo("ready_status_down");
    assertThat(signal.detail()).isEqualTo("status=DOWN");
    assertThat(symptom.symptom()).isEqualTo("readiness_down");
    assertThat(symptom.operatorHint()).contains("/ops/health/ready");
    assertThat(symptom.signals()).containsExactly(signal);
  }
}
