package mcp.server.foundation.observability.triage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Operational symptom-to-signal triage endpoint.
 */
@RestController
public final class OperTriageCtrl {

  private final RTTriageService runtimeTriageService;

  public OperTriageCtrl(RTTriageService runtimeTriageService) {
    this.runtimeTriageService = Objects.requireNonNull(runtimeTriageService, "runtimeTriageService");
  }

  @GetMapping("/ops/triage")
  public ResponseEntity<RTTriageView> OpsTriageCtrlGetTriage() {
    return ResponseEntity.ok(runtimeTriageService.RTTriageSvcGetView());
  }
}
