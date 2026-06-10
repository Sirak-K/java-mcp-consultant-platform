package mcp.server.foundation.observability.runtime;

import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.tool_interface.ToolInvocEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Produces conservative runtime tuning recommendations from p95/p99 signals.
 */
public final class RTTailLatencyAdvisor {

  private static final long QUEUE_WAIT_P95_WATCH_MILLIS = 75L;
  private static final long QUEUE_WAIT_P95_ACTION_MILLIS = 150L;
  private static final long QUEUE_WAIT_P99_ACTION_MILLIS = 250L;

  public RTCalibrView RuntimeTailLatencyAdvise(
      McpTelemMetrics.TelemSnapshot telemetrySnapshot,
      ToolInvocEngine.ConcurrSnapshot concurrencySnapshot) {

    Objects.requireNonNull(telemetrySnapshot, "telemetrySnapshot");
    Objects.requireNonNull(concurrencySnapshot, "concurrencySnapshot");

    long queueWaitP95Millis = telemetrySnapshot.queueWaitP95Millis();
    long queueWaitP99Millis = telemetrySnapshot.queueWaitP99Millis();
    long toolRejectionsTotal = telemetrySnapshot.toolRejectionsTotal();
    long toolTimeoutsTotal = telemetrySnapshot.toolTimeoutsTotal();
    boolean permitsExhausted = concurrencySnapshot.globalAvailablePermits() == 0;
    boolean tailSkewObserved = queueWaitP99Millis >= Math.max(
        QUEUE_WAIT_P99_ACTION_MILLIS,
        safeMultiply(queueWaitP95Millis, 2L));

    List<RTCalibrRecommendationView> recommendations = new ArrayList<>();

    if (toolRejectionsTotal > 0L && (permitsExhausted || queueWaitP99Millis >= QUEUE_WAIT_P99_ACTION_MILLIS)) {
      recommendations.add(new RTCalibrRecommendationView(
          "tail_latency_concurrency_calibration",
          "concurrency",
          "high",
          "global/post-init concurrency",
          "p99 koetid=" + queueWaitP99Millis
              + " ms, avvisningar=" + toolRejectionsTotal
              + ", lediga globala permits=" + concurrencySnapshot.globalAvailablePermits(),
          "Kalibrera forst samtidighetsgranserna mot svanslatensen: prova stegvis justering av "
              + "`mcp.tools.execution.global-max-concurrency` och de mest belastade verktygens "
              + "`maxConcurrency` under kontrollerad last, i stallet for att styra pa medelvarden."));
    }

    if (toolTimeoutsTotal > 0L && queueWaitP95Millis >= QUEUE_WAIT_P95_ACTION_MILLIS) {
      recommendations.add(new RTCalibrRecommendationView(
          "tail_latency_timeout_calibration",
          "timeout",
          "high",
          RuntimeTailLatencyAdvisorTimeoutTarget(telemetrySnapshot),
          "p95 koetid=" + queueWaitP95Millis
              + " ms, p99 koetid=" + queueWaitP99Millis
              + " ms, timeouter=" + toolTimeoutsTotal,
          "Kalibrera timeoutnivaer med p95/p99 som baslinje. Sanka koetrycket forst, och justera sedan "
              + "timeoutMillis for timeout-drabbade verktyg sa att nivan speglar verklig svanslatens i stallet "
              + "for genomsnittlig exekveringstid."));
    }

    if ((queueWaitP95Millis >= QUEUE_WAIT_P95_WATCH_MILLIS || tailSkewObserved)
        && concurrencySnapshot.globalAvailablePermits() <= Math.max(1, concurrencySnapshot.globalMaxConcurrency() / 4)) {
      recommendations.add(new RTCalibrRecommendationView(
          "tail_latency_resource_flow_calibration",
          "resource_flow",
          queueWaitP95Millis >= QUEUE_WAIT_P95_ACTION_MILLIS ? "high" : "medium",
          "burst/resource flow",
          "p95 koetid=" + queueWaitP95Millis
              + " ms, p99 koetid=" + queueWaitP99Millis
              + " ms, global tillganglighet="
              + concurrencySnapshot.globalAvailablePermits() + "/" + concurrencySnapshot.globalMaxConcurrency(),
          "Jamna ut burst-floden runt inbound och verktygskoer innan fler resurser slapps pa. Nar p99 drar ifran "
              + "p95 ar det ett tecken pa tail-problem och inte bara normal medelbelastning."));
    }

    if (recommendations.isEmpty()) {
      return new RTCalibrView(
          "stable",
          false,
          "Kalibrering styrs av p95/p99 och koetid fore exekvering.",
          List.of());
    }

    boolean actionRequired = recommendations.stream()
        .anyMatch(recommendation -> "high".equals(recommendation.priority()));

    return new RTCalibrView(
        actionRequired ? "action_required" : "watch",
        actionRequired,
        "Kalibrering styrs av p95/p99 och koetid fore exekvering.",
        recommendations);
  }

  private static String RuntimeTailLatencyAdvisorTimeoutTarget(
      McpTelemMetrics.TelemSnapshot telemetrySnapshot) {

    String failingTools = telemetrySnapshot.mostFailingTools().stream()
        .filter(tool -> tool.timeoutCount() > 0L)
        .map(McpTelemMetrics.ToolFailureSnapshot::toolName)
        .limit(3)
        .collect(Collectors.joining(", "));

    if (failingTools.isBlank()) {
      return "tool timeout policy";
    }

    return "tool timeout policy (" + failingTools + ")";
  }

  private static long safeMultiply(long value, long factor) {
    if (value <= 0L || factor <= 0L) {
      return 0L;
    }
    if (value > Long.MAX_VALUE / factor) {
      return Long.MAX_VALUE;
    }
    return value * factor;
  }
}
