package mcp.server.foundation.observability.runtime;

import java.util.List;
import java.util.Objects;

/**
 * Tail-latency-focused runtime calibration snapshot.
 */
public record RTCalibrView(
    String status,
    boolean actionRequired,
    String focus,
    List<RTCalibrRecommendationView> recommendations) {

  public RTCalibrView {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(focus, "focus");
    recommendations = List.copyOf(Objects.requireNonNull(recommendations, "recommendations"));
  }
}
