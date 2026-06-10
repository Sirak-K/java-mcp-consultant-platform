package mcp.server.foundation.transport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canonical operational capacity profile for transport adapters.
 */
public final class TranspCapacityProfile {

  public static final String MAX_ACTIVE_CONNECTIONS = "maxActiveConnections";
  public static final String MAX_ACTIVE_SESSIONS = "maxActiveSessions";
  public static final String MAX_ACTIVE_STREAMS = "maxActiveStreams";
  public static final String MAX_BUFFERED_OUTBOUND_MESSAGES_PER_SESSION = "maxBufferedOutboundMessagesPerSession";
  public static final String POST_RESPONSE_TIMEOUT_MILLIS = "postResponseTimeoutMillis";

  private TranspCapacityProfile() {
  }

  public static Map<String, Long> TransCapProfileWs(long maxActiveConnections) {
    return Map.of(MAX_ACTIVE_CONNECTIONS, maxActiveConnections);
  }

  public static Map<String, Long> TransCapProfileSTDIOSingleConn() {
    return TransCapProfileWs(1L);
  }

  public static Map<String, Long> TransCapProfileHttpStreamable(
      long maxActiveSessions,
      long maxActiveStreams,
      long maxBufferedOutboundMessagesPerSession,
      long postResponseTimeoutMillis) {

    return buildProfile(
        MAX_ACTIVE_SESSIONS, maxActiveSessions,
        MAX_ACTIVE_STREAMS, maxActiveStreams,
        MAX_BUFFERED_OUTBOUND_MESSAGES_PER_SESSION, maxBufferedOutboundMessagesPerSession,
        POST_RESPONSE_TIMEOUT_MILLIS, postResponseTimeoutMillis);
  }

  public static Map<String, Long> TransCapProfileHttpSse(
      long maxActiveSessions,
      long maxActiveStreams,
      long maxBufferedOutboundMessagesPerSession) {

    return buildProfile(
        MAX_ACTIVE_SESSIONS, maxActiveSessions,
        MAX_ACTIVE_STREAMS, maxActiveStreams,
        MAX_BUFFERED_OUTBOUND_MESSAGES_PER_SESSION, maxBufferedOutboundMessagesPerSession);
  }

  private static Map<String, Long> buildProfile(Object... entries) {

    if (entries.length == 0 || entries.length % 2 != 0) {
      throw new IllegalArgumentException("entries must be non-empty key/value pairs");
    }

    Map<String, Long> profile = new LinkedHashMap<>();
    for (int i = 0; i < entries.length; i += 2) {
      profile.put((String) entries[i], (Long) entries[i + 1]);
    }
    return Map.copyOf(profile);
  }
}
