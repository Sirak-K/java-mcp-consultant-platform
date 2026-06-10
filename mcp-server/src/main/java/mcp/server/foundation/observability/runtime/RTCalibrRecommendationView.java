package mcp.server.foundation.observability.runtime;

import java.util.Objects;

/**
 * Tail-latency-driven runtime tuning recommendation.
 */
public record RTCalibrRecommendationView(
    String id,
    String category,
    String priority,
    String target,
    String rationale,
    String suggestedAction) {

  public RTCalibrRecommendationView {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(priority, "priority");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(rationale, "rationale");
    Objects.requireNonNull(suggestedAction, "suggestedAction");
  }
}
