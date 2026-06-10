package mcp.server.foundation.transport.websocket;

import mcp.server.foundation.security.NetwAuthService;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import mcp.server.foundation.security.StreamableHTTPRateLimiter;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.server_process.client_context.session.id.McpSessIdGen;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.transport.TranspActivationCondition;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

/**
 * WsTranspCfg
 *
 * Realiserar WebSocket I/O boundary utan circular dependency.
 */
@Configuration
@EnableWebSocket
@Conditional(TranspActivationCondition.WebSocketCondition.class)
public class WsTranspCfg {

  @Bean
  public WsTranspSettings wsTranspSettings(
      @Value("${mcp.transport.websocket.path:/ws}") String path,
      @Value("${mcp.transport.websocket.allowed-origins:}") String allowedOriginsRaw,
      @Value("${mcp.transport.websocket.max-active-connections:128}") int maxActiveConnections) {

    String[] allowedOrigins = java.util.Arrays.stream(allowedOriginsRaw.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toArray(String[]::new);
    return new WsTranspSettings(path, allowedOrigins, maxActiveConnections);
  }

  @Bean
  public WsAuthHandshakeInterceptor wsAuthHandshakeInterceptor(
      NetwAuthService authService,
      StreamableHTTPRateLimiter rateLimiter,
      ServerLogger serverLogger,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    return new WsAuthHandshakeInterceptor(authService, rateLimiter, serverLogger, runtimeMetrics, telemetryMetrics);
  }

  @Bean
  public WebSocketConfigurer webSocketConfigurer(
      WsTranspHandler wsTranspHandler,
      WsTranspSettings settings,
      WsAuthHandshakeInterceptor authHandshakeInterceptor) {
    return registry -> registry
        .addHandler(wsTranspHandler, settings.path())
        .addInterceptors(authHandshakeInterceptor)
        .setAllowedOrigins(settings.allowedOrigins());
  }

  // =========================================================
  // Beans
  // =========================================================

  @Bean
  public WsRTOrch wsRTOrch(
      McpSessIdGen mcpSessIdGenerator,
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      McpSessRTMetaFactory runtimeMetaFactory,
      WsTranspSettings wsTranspSettings) {

    return new WsRTOrch(
        mcpSessIdGenerator,
        serverLogger,
        obsCtxFactory,
        runtimeMetrics,
        telemetryMetrics,
        requestAuthBindingPolicy,
        runtimeMetaFactory,
        wsTranspSettings);
  }

  @Bean
  public WsTranspAdap wsTranspAdap(
      WsRTOrch runtime) {

    return new WsTranspAdap(runtime);
  }

  @Bean
  public WsTranspHandler wsTranspHandler(
      WsTranspAdap adapter,
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    return new WsTranspHandler(
        adapter,
        serverLogger,
        obsCtxFactory,
        runtimeMetrics,
        telemetryMetrics);
  }
}
