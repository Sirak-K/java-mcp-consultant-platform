package mcp.server.foundation.spring_integration;

import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.server_process.orchestration.RTMcpSessModelReg;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class SpringRTMcpSessCfg {

  @Bean
  public RTMcpSessModelReg runtimeSessionModelRegistry(
      @Value("${mcp.runtime.session.default-inactivity-ttl-seconds:1800}") long inactivityTtlSeconds) {

    return RTMcpSessModelReg.RTMcpSessModelRegDefault(
        requirePositiveInactivityTtlSeconds(inactivityTtlSeconds));
  }

  @Bean
  public McpSessRTMetaFactory mcpSessRuntimeMetaFactory(
      RTMcpSessModelReg runtimeSessionModelRegistry) {
    return new McpSessRTMetaFactory(
        Objects.requireNonNull(runtimeSessionModelRegistry, "runtimeSessionModelRegistry"));
  }

  private static long requirePositiveInactivityTtlSeconds(long inactivityTtlSeconds) {
    if (inactivityTtlSeconds <= 0L) {
      throw new IllegalArgumentException("inactivityTtlSeconds must be > 0");
    }
    return inactivityTtlSeconds;
  }
}
