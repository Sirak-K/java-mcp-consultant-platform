package mcp.server.foundation.security;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.transport.http.shared.HTTPTranspCfg;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class StreamableHTTPReqsSizeLimitCfg {

  @Bean
  public StreamableHTTPReqsSizeLimitSettings streamableHttpRequestSizeLimitSettings(
      @org.springframework.beans.factory.annotation.Value(
          "${mcp.transport.streamable-http.max-request-body-bytes:1048576}") long maxRequestBodyBytes) {

    return new StreamableHTTPReqsSizeLimitSettings(maxRequestBodyBytes);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "mcp.transport.streamable-http",
      name = "enabled",
      havingValue = "true")
  public FilterRegistrationBean<StreamableHTTPReqsSizeLimitFilter> streamableHttpRequestSizeLimitFilter(
      HTTPTranspCfg.StreamableHTTPSettings streamableHttpSettings,
      StreamableHTTPReqsSizeLimitSettings streamableHttpRequestSizeLimitSettings,
      ServerLogger logger,
      McpTelemMetrics telemetryMetrics) {

    FilterRegistrationBean<StreamableHTTPReqsSizeLimitFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new StreamableHTTPReqsSizeLimitFilter(
        streamableHttpRequestSizeLimitSettings,
        logger,
        telemetryMetrics));
    registration.addUrlPatterns(streamableHttpSettings.endpointPath());
    registration.setName("streamableHttpRequestSizeLimitFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
  }
}
