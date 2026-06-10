package mcp.server.foundation.security;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-client-IP fixed-window rate limiter for the Streamable HTTP transport.
 *
 * Uses a one-minute sliding window per client key. The window resets when the
 * first request arrives after the previous window has expired. Expired windows
 * are evicted lazily and can also be purged explicitly via
 * {@link #RateLimiterEvictExpired()}.
 */
public final class StreamableHTTPRateLimiter {

  static final long WINDOW_MS = 60_000L;

  private final StreamableHTTPRateLimitSettings settings;
  private final Clock clock;

  // Value: long[2] = { windowStartMs, requestCount }
  // compute() per key is atomic in ConcurrentHashMap, so per-IP increments are safe.
  private final ConcurrentHashMap<String, long[]> clientWindows = new ConcurrentHashMap<>();
  private final AtomicLong nextEvictionAtMs;

  public StreamableHTTPRateLimiter(StreamableHTTPRateLimitSettings settings) {
    this(settings, Clock.systemUTC());
  }

  StreamableHTTPRateLimiter(StreamableHTTPRateLimitSettings settings, Clock clock) {
    this.settings = Objects.requireNonNull(settings, "settings");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.nextEvictionAtMs = new AtomicLong(clock.millis() + WINDOW_MS);
  }

  public boolean RateLimiterIsEnabled() {
    return settings.enabled();
  }

  /**
   * Increments the request counter for the given client key and throws
   * {@link TranspRateLimitExcep} if the per-minute limit is exceeded.
   *
   * @param clientKey client IP address or comparable key; null/blank treated as "unknown"
   * @throws TranspRateLimitExcep if the rate limit is exceeded
   */
  public void RateLimiterAssert(String clientKey) {

    if (!settings.enabled()) {
      return;
    }

    String key = (clientKey == null || clientKey.isBlank()) ? "unknown" : clientKey.trim();
    long now = clock.millis();
    RateLimiterMaybeEvictExpired(now);

    long[] bucket = clientWindows.compute(key, (k, existing) -> {
      if (existing == null || now - existing[0] >= WINDOW_MS) {
        return new long[]{now, 1L};
      }
      existing[1]++;
      return existing;
    });

    if (bucket[1] > settings.maxRequestsPerMinute()) {
      throw new TranspRateLimitExcep("Rate limit exceeded for client " + key);
    }
  }

  /**
   * Removes per-client windows that have been inactive for at least two full
   * window durations. Safe to call from any thread at any time.
   */
  public void RateLimiterEvictExpired() {
    long now = clock.millis();
    RateLimiterEvictExpired(now);
  }

  int RateLimiterTrackedClientCount() {
    return clientWindows.size();
  }

  private void RateLimiterMaybeEvictExpired(long now) {
    long scheduledEvictionAt = nextEvictionAtMs.get();
    if (now < scheduledEvictionAt) {
      return;
    }

    if (!nextEvictionAtMs.compareAndSet(scheduledEvictionAt, now + WINDOW_MS)) {
      return;
    }

    RateLimiterEvictExpired(now);
  }

  private void RateLimiterEvictExpired(long now) {
    clientWindows.entrySet().removeIf(entry -> now - entry.getValue()[0] >= WINDOW_MS * 2);
  }
}
