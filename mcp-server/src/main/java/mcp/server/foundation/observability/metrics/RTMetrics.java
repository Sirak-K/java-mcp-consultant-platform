package mcp.server.foundation.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory runtime metrics store for operational visibility.
 *
 * Contract:
 * - Counters and gauges are stored as single long values.
 * - Timers store count, total and max in milliseconds.
 */
public final class RTMetrics {

  private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> gauges = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, TimerMetric> timers = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Counter> micrometerCounters = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Timer> micrometerTimers = new ConcurrentHashMap<>();
  private final MeterRegistry meterRegistry;

  public RTMetrics() {
    this(null);
  }

  public RTMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void RTMetricsIncrementCounter(String metricName) {
    RTMetricsAddCounter(metricName, 1L);
  }

  public void RTMetricsAddCounter(String metricName, long delta) {
    counters.computeIfAbsent(metricName, ignored -> new AtomicLong()).addAndGet(delta);

    if (meterRegistry != null && delta > 0L) {
      micrometerCounters.computeIfAbsent(
              metricName,
              ignored -> Counter.builder(metricName).register(meterRegistry))
          .increment(delta);
    }
  }

  public void RTMetricsSetGauge(String metricName, long value) {
    AtomicLong gauge = gauges.computeIfAbsent(metricName, ignored -> {
      AtomicLong registeredGauge = new AtomicLong();

      if (meterRegistry != null) {
        meterRegistry.gauge(metricName, registeredGauge);
      }

      return registeredGauge;
    });

    gauge.set(value);
  }

  public void RTMetricsRecordTimerMillis(String metricName, long durationMillis) {

    if (durationMillis < 0L) {
      durationMillis = 0L;
    }

    timers.computeIfAbsent(metricName, ignored -> new TimerMetric())
        .record(durationMillis);

    if (meterRegistry != null) {
      micrometerTimers.computeIfAbsent(
              metricName,
              ignored -> Timer.builder(metricName).register(meterRegistry))
          .record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
  }

  public long RTMetricsGetCounter(String metricName) {
    AtomicLong counter = counters.get(metricName);
    return counter == null ? 0L : counter.get();
  }

  public long RTMetricsGetGauge(String metricName) {
    AtomicLong gauge = gauges.get(metricName);
    return gauge == null ? 0L : gauge.get();
  }

  public TimerSnapshot RTMetricsGetTimerSnapshot(String metricName) {
    TimerMetric timer = timers.get(metricName);
    return timer == null ? new TimerSnapshot(0L, 0L, 0L, 0L, 0L, 0L) : timer.snapshot();
  }

  public Map<String, Long> RTMetricsGetCountersSnapshot() {
    return counters.entrySet().stream()
        .collect(ConcurrentHashMap::new,
            (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
            ConcurrentHashMap::putAll);
  }

  public Map<String, Long> RTMetricsGetGaugesSnapshot() {
    return gauges.entrySet().stream()
        .collect(ConcurrentHashMap::new,
            (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
            ConcurrentHashMap::putAll);
  }

  public Set<String> RTMetricsGetTimerNames() {
    return Set.copyOf(timers.keySet());
  }

  public record TimerSnapshot(
      long count,
      long totalMillis,
      long maxMillis,
      long avgMillis,
      long p95Millis,
      long p99Millis) {
  }

  private static final class TimerMetric {

    private final AtomicLong count = new AtomicLong();
    private final AtomicLong totalMillis = new AtomicLong();
    private final AtomicLong maxMillis = new AtomicLong();
    private final TimerSampleReservoir reservoir = new TimerSampleReservoir();

    void record(long durationMillis) {
      count.incrementAndGet();
      totalMillis.addAndGet(durationMillis);
      maxMillis.accumulateAndGet(durationMillis, Math::max);
      reservoir.record(durationMillis);
    }

    TimerSnapshot snapshot() {
      long currentCount = count.get();
      long currentTotalMillis = totalMillis.get();
      long currentMaxMillis = maxMillis.get();
      long avgMillis = currentCount <= 0L ? 0L : currentTotalMillis / currentCount;

      TimerSampleReservoir.PercentileSnapshot percentiles = reservoir.percentileSnapshot();

      return new TimerSnapshot(
          currentCount,
          currentTotalMillis,
          currentMaxMillis,
          avgMillis,
          percentiles.p95Millis(),
          percentiles.p99Millis());
    }
  }
}
