package mcp.server.foundation.transport.http.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class HTTPAsyncMvcCfg implements WebMvcConfigurer {

  private final long asyncRequestTimeoutMs;

  public HTTPAsyncMvcCfg(
      @Value("${mcp.transport.http.async-request-timeout-ms:-1}") long asyncRequestTimeoutMs) {
    this.asyncRequestTimeoutMs = asyncRequestTimeoutMs;
  }

  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setDefaultTimeout(asyncRequestTimeoutMs);
  }
}
