package mcp.server.foundation.observability.readiness;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Canonical operational readiness gate surface.
 */
@RestController
public final class OperReadinessCtrl {

  private final RTReadinessService runtimeReadinessService;

  public OperReadinessCtrl(RTReadinessService runtimeReadinessService) {
    this.runtimeReadinessService = Objects.requireNonNull(runtimeReadinessService, "runtimeReadinessService");
  }

  @GetMapping("/ops/readiness")
  public ResponseEntity<RTReadinessGateView> OpsReadinessCtrlGetGate() {
    RTReadinessGateView view = runtimeReadinessService.RTReadinessSvcBuildOperGate();
    return ResponseEntity.status(view.ready() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(view);
  }
}
