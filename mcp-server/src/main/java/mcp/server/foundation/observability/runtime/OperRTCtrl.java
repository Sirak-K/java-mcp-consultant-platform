package mcp.server.foundation.observability.runtime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Operational runtime visibility endpoints for metrics, status and checks.
 */
@RestController
public final class OperRTCtrl {

  private final RTVisibilityService runtimeVisibilityService;

  public OperRTCtrl(RTVisibilityService runtimeVisibilityService) {
    this.runtimeVisibilityService = Objects.requireNonNull(runtimeVisibilityService, "runtimeVisibilityService");
  }

  @GetMapping("/ops/metrics")
  public ResponseEntity<RTMetricsView> OpsRTCtrlGetMetrics() {
    return ResponseEntity.ok(runtimeVisibilityService.RTVisibilitySvcGetMetrics());
  }

  @GetMapping("/ops/runtime")
  public ResponseEntity<RTStatusView> OpsRTCtrlGetRTStatus() {
    return ResponseEntity.ok(runtimeVisibilityService.RTVisibilitySvcGetRTStatus());
  }

  @GetMapping("/ops/checks")
  public ResponseEntity<OperChecksView> OpsRTCtrlGetOperChecks() {
    return ResponseEntity.ok(runtimeVisibilityService.RTVisibilitySvcGetOperChecks());
  }
}
