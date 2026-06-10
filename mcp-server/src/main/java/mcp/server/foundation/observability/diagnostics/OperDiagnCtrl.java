package mcp.server.foundation.observability.diagnostics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public final class OperDiagnCtrl {

  private final RTDiagnService runtimeDiagnosticsService;

  public OperDiagnCtrl(RTDiagnService runtimeDiagnosticsService) {
    this.runtimeDiagnosticsService = Objects.requireNonNull(runtimeDiagnosticsService, "runtimeDiagnosticsService");
  }

  @GetMapping("/ops/diagnostics")
  public RTDiagnView diagnostics() {
    return runtimeDiagnosticsService.RTDiagSvcGetView();
  }
}
