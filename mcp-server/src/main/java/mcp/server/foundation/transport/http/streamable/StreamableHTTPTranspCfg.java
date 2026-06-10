package mcp.server.foundation.transport.http.streamable;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.rpc.RPCJsonEntry;
import mcp.server.foundation.security.NetwAuthService;
import mcp.server.foundation.security.StreamableHTTPRateLimiter;
import mcp.server.foundation.transport.TranspActivationCondition;
import mcp.server.foundation.transport.http.shared.HTTPSessBindingReg;
import mcp.server.foundation.transport.http.shared.HTTPTranspCfg;
import mcp.server.foundation.transport.http.shared.HTTPTranspSupport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(TranspActivationCondition.StreamableHTTPCondition.class)
public class StreamableHTTPTranspCfg {

  @Bean
  public StreamableHTTPRTOrch streamableHttpRTOrch(
      HTTPTranspCfg.StreamableHTTPSettings streamableHttpSettings,
      HTTPTranspSupport httpTranspSupport,
      HTTPSessBindingReg httpSessBindingReg,
      RPCJsonEntry rpcJsonEntrypoint,
      ServerLogger logger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    if (!streamableHttpSettings.enabled()) {
      throw new IllegalStateException("Streamable HTTP settings must be enabled when transport config is active");
    }

    return new StreamableHTTPRTOrch(
        httpTranspSupport,
        httpSessBindingReg,
        rpcJsonEntrypoint,
        logger,
        obsCtxFactory,
        runtimeMetrics,
        telemetryMetrics);
  }

  @Bean
  public StreamableHTTPOutb streamableHttpOutbound(
      StreamableHTTPRTOrch streamableHttpRTOrch,
      ServerLogger logger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    return new StreamableHTTPOutb(
        streamableHttpRTOrch,
        logger,
        obsCtxFactory,
        runtimeMetrics,
        telemetryMetrics);
  }

  @Bean
  public StreamableHTTPInb streamableHttpInbound(
      StreamableHTTPRTOrch streamableHttpRTOrch,
      HTTPTranspSupport httpTranspSupport,
      RPCJsonEntry rpcJsonEntrypoint,
      ServerLogger logger,
      NetwAuthService authService,
      StreamableHTTPRateLimiter streamableHttpRateLimiter,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    return new StreamableHTTPInb(
        streamableHttpRTOrch,
        httpTranspSupport,
        rpcJsonEntrypoint,
        logger,
        authService,
        streamableHttpRateLimiter,
        runtimeMetrics,
        telemetryMetrics);
  }

  @Bean
  public StreamableHTTPTranspAdap streamableHttpTranspAdap(
      StreamableHTTPRTOrch streamableHttpRTOrch,
      StreamableHTTPOutb streamableHttpOutbound) {

    return new StreamableHTTPTranspAdap(
        streamableHttpRTOrch,
        streamableHttpOutbound);
  }
}
