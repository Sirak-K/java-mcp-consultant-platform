package mcp.server.foundation.observability.runtime;

import mcp.server.foundation.observability.metrics.RTMetrics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class RTMetricsViewAssembler {

  private final RTMetrics runtimeMetrics;

  RTMetricsViewAssembler(RTMetrics runtimeMetrics) {
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
  }

  RTMetricsView assemble() {

    Map<String, MetricTimerView> timers = runtimeMetrics.RTMetricsGetTimerNames()
        .stream()
        .collect(Collectors.toMap(
            metricName -> metricName,
            metricName -> {
              RTMetrics.TimerSnapshot timer = runtimeMetrics.RTMetricsGetTimerSnapshot(metricName);
              return new MetricTimerView(
                  timer.count(),
                  timer.totalMillis(),
                  timer.maxMillis(),
                  timer.avgMillis(),
                  timer.p95Millis(),
                  timer.p99Millis());
            },
            (left, right) -> right,
            LinkedHashMap::new));

    return new RTMetricsView(
        new LinkedHashMap<>(runtimeMetrics.RTMetricsGetCountersSnapshot()),
        new LinkedHashMap<>(runtimeMetrics.RTMetricsGetGaugesSnapshot()),
        timers);
  }
}
