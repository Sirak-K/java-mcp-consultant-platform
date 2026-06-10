package mcp.server.foundation.security;

public record StreamableHTTPRateLimitSettings(
    boolean enabled,
    int maxRequestsPerMinute) {

  public StreamableHTTPRateLimitSettings {
    if (maxRequestsPerMinute <= 0) {
      throw new IllegalArgumentException("maxRequestsPerMinute must be > 0");
    }
  }
}
