package mcp.server.foundation.transport.http.shared;

import mcp.server.foundation.security.JwtReqsBindingExtractor;
import mcp.server.foundation.server_process.client_context.session.id.McpSessIdGen;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import mcp.server.foundation.security.request_binding.ReqsBindingComplianceGuard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared Spring wiring for the HTTP transport family.
 */
@Configuration
public class HTTPTranspSharedCfg {

  @Bean
  public HTTPTranspCfg httpTranspConfig(
      @Value("${mcp.transport.http.trusted-edge.required:false}") boolean trustedEdgeRequired,
      @Value("${mcp.transport.http.trusted-edge.require-forwarded-https:false}") boolean requireForwardedHttps,
      @Value("${mcp.transport.http.trusted-edge.trusted-proxy-addresses:}") String trustedProxyAddressesRaw,
      @Value("${mcp.transport.streamable-http.enabled:false}") boolean streamableEnabled,
      @Value("${mcp.transport.streamable-http.endpoint-path:/mcp}") String streamableEndpointPath,
      @Value("${mcp.transport.streamable-http.require-origin-validation:true}") boolean streamableRequireOriginValidation,
      @Value("${mcp.transport.streamable-http.localhost-only:true}") boolean streamableLocalhostOnly,
      @Value("${mcp.transport.streamable-http.allowed-origins:}") String streamableAllowedOriginsRaw,
      @Value("${mcp.transport.streamable-http.allowed-hosts:}") String streamableAllowedHostsRaw,
      @Value("${mcp.transport.streamable-http.allow-session-delete:true}") boolean streamableAllowSessionDelete,
      @Value("${mcp.transport.streamable-http.require-protocol-version-header-after-init:true}") boolean streamableRequireProtocolVersionHeaderAfterInit,
      @Value("${mcp.transport.streamable-http.max-active-sessions:128}") int streamableMaxActiveSessions,
      @Value("${mcp.transport.streamable-http.max-active-streams:128}") int streamableMaxActiveStreams,
      @Value("${mcp.transport.streamable-http.max-buffered-outbound-messages-per-session:64}") int streamableMaxBufferedOutboundMessagesPerSession,
      @Value("${mcp.transport.streamable-http.post-response-timeout-millis:10000}") long streamablePostResponseTimeoutMillis) {

    String[] trustedProxyAddresses = splitCsv(trustedProxyAddressesRaw);
    String[] streamableAllowedOrigins = splitCsv(streamableAllowedOriginsRaw);
    String[] streamableAllowedHosts = splitCsv(streamableAllowedHostsRaw);

    return new HTTPTranspCfg(
        new HTTPTranspCfg.TrustedEdgeSettings(
            trustedEdgeRequired,
            requireForwardedHttps,
            trustedProxyAddresses),
        new HTTPTranspCfg.StreamableHTTPSettings(
            streamableEnabled,
            streamableEndpointPath,
            streamableRequireOriginValidation,
            streamableLocalhostOnly,
            streamableAllowSessionDelete,
            streamableRequireProtocolVersionHeaderAfterInit,
            streamableMaxActiveSessions,
            streamableMaxActiveStreams,
            streamableMaxBufferedOutboundMessagesPerSession,
            streamablePostResponseTimeoutMillis,
            streamableAllowedOrigins,
            streamableAllowedHosts));
  }

  @Bean
  public HTTPSessBindingReg httpSessBindingReg() {
    return new HTTPSessBindingReg();
  }

  @Bean
  public HTTPTranspCfg.StreamableHTTPSettings streamableHttpSettings(
      HTTPTranspCfg httpTranspConfig) {
    return httpTranspConfig.HTTPCfgGetStreamableHTTP();
  }

  @Bean
  public HTTPTranspCfg.TrustedEdgeSettings trustedEdgeSettings(
      HTTPTranspCfg httpTranspConfig) {
    return httpTranspConfig.HTTPCfgGetTrustedEdge();
  }

  @Bean
  public HTTPTranspSupport httpTranspSupport(
      HTTPTranspCfg httpTranspConfig,
      McpSessIdGen sessionIdGenerator,
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      McpSessRTMetaFactory runtimeMetaFactory,
      ReqsBindingComplianceGuard requestBindingComplianceGuard,
      @Autowired(required = false) JwtReqsBindingExtractor jwtRequestBindingExtractor) {

    return new HTTPTranspSupport(
        httpTranspConfig,
        sessionIdGenerator,
        requestAuthBindingPolicy,
        runtimeMetaFactory,
        requestBindingComplianceGuard,
        jwtRequestBindingExtractor);
  }

  private static String[] splitCsv(String rawValue) {
    return rawValue == null ? new String[0] : rawValue.split(",");
  }
}
