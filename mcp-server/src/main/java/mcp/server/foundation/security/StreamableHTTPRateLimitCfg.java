package mcp.server.foundation.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamableHTTPRateLimitCfg {

  @Bean
  public StreamableHTTPRateLimitSettings streamableHttpRateLimitSettings(
      @Value("${mcp.security.rate-limit.enabled:true}") boolean enabled,
      @Value("${mcp.security.rate-limit.max-requests-per-minute:120}") int maxRequestsPerMinute) {

    return new StreamableHTTPRateLimitSettings(enabled, maxRequestsPerMinute);
  }

  @Bean
  public StreamableHTTPRateLimiter streamableHttpRateLimiter(
      StreamableHTTPRateLimitSettings streamableHttpRateLimitSettings) {

    return new StreamableHTTPRateLimiter(streamableHttpRateLimitSettings);
  }
}
