package mcp.server.foundation.observability.runtime;

import java.util.Map;
import java.util.Objects;

/**
 * Canonical runtime metrics payload for ops visibility.
 */
public record RTMetricsView(
    Map<String, Long> counters,
    Map<String, Long> gauges,
    Map<String, MetricTimerView> timers) {

  public RTMetricsView {
    counters = Map.copyOf(Objects.requireNonNull(counters, "counters"));
    gauges = Map.copyOf(Objects.requireNonNull(gauges, "gauges"));
    timers = Map.copyOf(Objects.requireNonNull(timers, "timers"));
  }
}
