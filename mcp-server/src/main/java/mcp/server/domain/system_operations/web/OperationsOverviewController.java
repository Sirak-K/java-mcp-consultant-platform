package mcp.server.domain.system_operations.web;

import mcp.server.domain.system_operations.application.OperationsOverviewQueryService;
import mcp.server.domain.system_operations.application.OperationsOverviewQueryService.OperationsOverview;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/ops")
public final class OperationsOverviewController {

  private final OperationsOverviewQueryService queryService;

  public OperationsOverviewController(OperationsOverviewQueryService queryService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService");
  }

  @GetMapping("/overview")
  public OperationsOverviewResponse overview() {
    OperationsOverview overview = queryService.overview();

    return new OperationsOverviewResponse(
        overview.activeSessions(),
        overview.lastSessionAt(),
        overview.customerCount(),
        overview.candidateCount(),
        overview.totalMissions());
  }
}
