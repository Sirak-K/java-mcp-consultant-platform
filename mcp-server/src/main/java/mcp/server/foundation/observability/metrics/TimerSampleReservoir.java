package mcp.server.foundation.observability.metrics;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

final class TimerSampleReservoir {

  private static final int DEFAULT_RESERVOIR_SIZE = 1024;

  private final AtomicLong sampleSequence = new AtomicLong();
  private final AtomicLongArray recentSamples;

  TimerSampleReservoir() {
    this(DEFAULT_RESERVOIR_SIZE);
  }

  private TimerSampleReservoir(int reservoirSize) {
    if (reservoirSize <= 0) {
      throw new IllegalArgumentException("reservoirSize must be positive");
    }
    this.recentSamples = new AtomicLongArray(reservoirSize);
  }

  void record(long durationMillis) {
    long normalizedDurationMillis = Math.max(0L, durationMillis);
    recentSamples.set(
        (int) (sampleSequence.getAndIncrement() % recentSamples.length()),
        normalizedDurationMillis);
  }

  PercentileSnapshot percentileSnapshot() {
    long[] samples = sortedSamplesSnapshot();
    return new PercentileSnapshot(
        percentile(samples, 0.95d),
        percentile(samples, 0.99d));
  }

  private long[] sortedSamplesSnapshot() {
    int sampleCount = (int) Math.min(sampleSequence.get(), recentSamples.length());
    long[] samples = new long[sampleCount];
    for (int index = 0; index < sampleCount; index++) {
      samples[index] = recentSamples.get(index);
    }
    Arrays.sort(samples);
    return samples;
  }

  private static long percentile(long[] sortedSamples, double quantile) {
    if (sortedSamples.length == 0) {
      return 0L;
    }

    int index = (int) Math.ceil(quantile * sortedSamples.length) - 1;
    index = Math.max(0, Math.min(index, sortedSamples.length - 1));
    return sortedSamples[index];
  }

  record PercentileSnapshot(long p95Millis, long p99Millis) {
  }
}
