package mcp.server.foundation.observability.readiness;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Compact operational readiness go/no-go snapshot.
 */
public record RTReadinessGateView(
    String gateId,
    String gateTag,
    RTReadinessStatus overallStatus,
    boolean ready,
    List<RTReadinessCriterion> criteria,
    int requiredCount,
    int passedCount,
    int failedCount,
    int notApplicableCount,
    Instant evaluatedAt) {

  public RTReadinessGateView {
    gateId = Objects.requireNonNull(gateId, "gateId");
    gateTag = Objects.requireNonNull(gateTag, "gateTag");
    overallStatus = Objects.requireNonNull(overallStatus, "overallStatus");
    criteria = List.copyOf(Objects.requireNonNull(criteria, "criteria"));
    evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt");

    requiredCount = (int) criteria.stream().filter(RTReadinessCriterion::required).count();
    passedCount = (int) criteria.stream().filter(RTReadinessCriterion::passed).count();
    failedCount = (int) criteria.stream().filter(c -> c.status() == RTReadinessStatus.FAIL).count();
    notApplicableCount =
        (int) criteria.stream().filter(c -> c.status() == RTReadinessStatus.NOT_APPLICABLE).count();
    ready = overallStatus == RTReadinessStatus.PASS;
  }
}
