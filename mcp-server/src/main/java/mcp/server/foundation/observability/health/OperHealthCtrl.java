package mcp.server.foundation.observability.health;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Canonical operational health surface for runtime liveness and readiness.
 */
@RestController
public final class OperHealthCtrl {

  private final RTHealthService runtimeHealthService;

  public OperHealthCtrl(RTHealthService runtimeHealthService) {
    this.runtimeHealthService = Objects.requireNonNull(runtimeHealthService, "runtimeHealthService");
  }

  @GetMapping("/ops/health/live")
  public ResponseEntity<RTHealthView> OpsHealthCtrlGetLiveness() {
    return ResponseEntity.ok(runtimeHealthService.RTHealthSvcGetLiveness());
  }

  @GetMapping("/ops/health/ready")
  public ResponseEntity<RTHealthView> OpsHealthCtrlGetReadiness() {
    RTHealthView view = runtimeHealthService.RTHealthSvcGetReadiness();
    return ResponseEntity.status(view.ready() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(view);
  }

  @GetMapping("/ops/health")
  public ResponseEntity<RTHealthView> OpsHealthCtrlGetSummary() {
    RTHealthView view = runtimeHealthService.RTHealthSvcGetSummary();
    return ResponseEntity.status(view.ready() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(view);
  }
}
