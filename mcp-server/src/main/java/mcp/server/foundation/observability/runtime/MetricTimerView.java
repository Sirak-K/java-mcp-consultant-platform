package mcp.server.foundation.observability.runtime;

/**
 * Read-only timer metric view for operational runtime visibility.
 */
public record MetricTimerView(
    long count,
    long totalMillis,
    long maxMillis,
    long avgMillis,
    long p95Millis,
    long p99Millis) {
}
