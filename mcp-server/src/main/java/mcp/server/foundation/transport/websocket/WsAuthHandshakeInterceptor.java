package mcp.server.foundation.transport.websocket;

import mcp.server.foundation.security.NetwAuthService;
import mcp.server.foundation.security.NetwAuthHTTPResp;
import mcp.server.foundation.security.StreamableHTTPRateLimiter;
import mcp.server.foundation.security.TranspAuthExcep;
import mcp.server.foundation.security.TranspRateLimitExcep;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspSignalModel;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Objects;

public final class WsAuthHandshakeInterceptor implements HandshakeInterceptor {

  private final NetwAuthService authService;
  private final StreamableHTTPRateLimiter rateLimiter;
  private final ServerLogger logger;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;

  public WsAuthHandshakeInterceptor(
      NetwAuthService authService,
      StreamableHTTPRateLimiter rateLimiter,
      ServerLogger logger,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    this.authService = Objects.requireNonNull(authService, "authService");
    this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {

    String clientIp = request.getRemoteAddress() != null
        ? request.getRemoteAddress().getAddress().getHostAddress()
        : null;

    try {
      rateLimiter.RateLimiterAssert(clientIp);
      authService.NetAuthAssertAuthorized(
          request.getHeaders().getFirst(authService.NetAuthHeaderName()),
          "websocket");
      return true;
    } catch (TranspRateLimitExcep ex) {
      runtimeMetrics.RTMetricsIncrementCounter(
          TranspSignalModel.TransSigOverloadRejectionsMetricName("websocket"));
      response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
      return false;
    } catch (TranspAuthExcep ex) {
      runtimeMetrics.RTMetricsIncrementCounter(
          TranspSignalModel.TransSigAuthDeniedMetricName("websocket"));
      telemetryMetrics.McpTelemIncrementAuthDenied("websocket", ex.TransAuthExcepGetReason().name());
      telemetryMetrics.McpTelemIncrementSecurityDenied("websocket", "AUTH_DENIED");
      logger.ServerLogSecurityAuditWarnDeniedObserved(
          ServerLogger.Component.WS,
          null,
          "AUTH_DENIED",
          "WsAuthHandshakeInterceptor: websocket auth denied reason="
              + ex.TransAuthExcepGetReason().name());
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      response.getHeaders().putAll(NetwAuthHTTPResp.NetAuthUnauthorizedHeaders(authService, ex));
      return false;
    }
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // No-op.
  }
}
