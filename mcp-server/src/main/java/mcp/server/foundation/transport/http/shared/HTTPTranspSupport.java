package mcp.server.foundation.transport.http.shared;

import jakarta.servlet.http.HttpServletRequest;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.client_context.session.id.McpSessIdGen;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import mcp.server.foundation.security.request_binding.ReqsBindingComplianceGuard;
import mcp.server.foundation.security.JwtReqsBindingExtractor;
import mcp.server.foundation.transport.TranspSess;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared helper surface for the HTTP transport family.
 */
public final class HTTPTranspSupport {

  public static final String HEADER_MCP_SESSION_ID = "MCP-Session-Id";
  public static final String HEADER_MCP_PROTOCOL_VERSION = "MCP-Protocol-Version";
  public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
  public static final String HEADER_X_FORWARDED_HOST = "X-Forwarded-Host";
  public static final String HEADER_X_FORWARDED_PROTO = "X-Forwarded-Proto";
  public static final String HEADER_HOST = "Host";
  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final String CONTENT_TYPE_SSE = "text/event-stream";
  public static final String SSE_EVENT_ENDPOINT = "endpoint";
  public static final String SSE_EVENT_MESSAGE = "message";

  private final HTTPTranspCfg config;
  private final McpSessIdGen sessionIdGenerator;
  private final ReqsAuthBindingPolicy requestAuthBindingPolicy;
  private final McpSessRTMetaFactory runtimeMetaFactory;
  private final ReqsBindingComplianceGuard requestBindingComplianceGuard;
  private final JwtReqsBindingExtractor jwtRequestBindingExtractor; // nullable, only in prod

  public HTTPTranspSupport(
      HTTPTranspCfg config,
      McpSessIdGen sessionIdGenerator,
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      McpSessRTMetaFactory runtimeMetaFactory,
      ReqsBindingComplianceGuard requestBindingComplianceGuard) {
    this(config, sessionIdGenerator, requestAuthBindingPolicy,
        runtimeMetaFactory, requestBindingComplianceGuard, null);
  }

  public HTTPTranspSupport(
      HTTPTranspCfg config,
      McpSessIdGen sessionIdGenerator,
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      McpSessRTMetaFactory runtimeMetaFactory,
      ReqsBindingComplianceGuard requestBindingComplianceGuard,
      JwtReqsBindingExtractor jwtRequestBindingExtractor) {

    this.config = Objects.requireNonNull(config, "config");
    this.sessionIdGenerator = Objects.requireNonNull(sessionIdGenerator, "sessionIdGenerator");
    this.requestAuthBindingPolicy = Objects.requireNonNull(requestAuthBindingPolicy, "requestAuthBindingPolicy");
    this.runtimeMetaFactory = Objects.requireNonNull(runtimeMetaFactory, "runtimeMetaFactory");
    this.requestBindingComplianceGuard = Objects.requireNonNull(
        requestBindingComplianceGuard, "requestBindingComplianceGuard");
    this.jwtRequestBindingExtractor = jwtRequestBindingExtractor; // nullable
  }

  public HTTPReqsMetadata HTTPSupBuildMetadata(HttpServletRequest request) {

    Objects.requireNonNull(request, "request");

    return new HTTPReqsMetadata(
        UUID.randomUUID().toString(),
        request.getMethod(),
        HTTPSupBuildReqsUri(request),
        request.getHeader("Accept"),
        request.getContentType(),
        request.getHeader("Origin"),
        HTTPSupResolveHostHeader(request),
        request.getRemoteAddr(),
        request.getHeader(HEADER_X_FORWARDED_FOR),
        request.getHeader(HEADER_X_FORWARDED_HOST),
        request.getHeader(HEADER_X_FORWARDED_PROTO),
        HTTPSupIsTrustedProxyAddress(request.getRemoteAddr()));
  }

  public HTTPTranspSess HttpSupCreateSession(
      String transportName,
      HTTPReqsMetadata openingRequestMetadata) {

    return HttpSupCreateSession(
        transportName,
        openingRequestMetadata,
        OperatingSurface.MCP_DIRECT);
  }

  public HTTPTranspSess HttpSupCreateSession(
      String transportName,
      HTTPReqsMetadata openingRequestMetadata,
      OperatingSurface operatingSurface) {

    return HttpSupCreateSession(
        transportName,
        openingRequestMetadata,
        operatingSurface,
        HTTPTranspSupportDefaultBinding(transportName, operatingSurface));
  }

  public HTTPTranspSess HttpSupCreateSession(
      String transportName,
      HTTPReqsMetadata openingRequestMetadata,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding) {

    HTTPSupAssertSupportedTransp(transportName);

    return new HTTPTranspSess(
        transportName,
        HTTPSupGenerateConnId(transportName),
        sessionIdGenerator.generate(),
        config.HTTPCfgResolvePrimaryEndpointPath(transportName),
        openingRequestMetadata,
        operatingSurface,
        HTTPSupRequireCompliantBinding(operatingSurface, requestAuthBinding),
        runtimeMetaFactory);
  }

  public HTTPTranspSess HttpSupCreateReqScopedSession(
      String transportName,
      HTTPReqsMetadata openingRequestMetadata) {

    return HttpSupCreateReqScopedSession(
        transportName,
        openingRequestMetadata,
        OperatingSurface.MCP_DIRECT);
  }

  public HTTPTranspSess HttpSupCreateReqScopedSession(
      String transportName,
      HTTPReqsMetadata openingRequestMetadata,
      OperatingSurface operatingSurface) {

    return HttpSupCreateReqScopedSession(
        transportName,
        openingRequestMetadata,
        operatingSurface,
        HTTPTranspSupportDefaultBinding(transportName, operatingSurface));
  }

  private ReqsAuthBinding HTTPTranspSupportDefaultBinding(
      String transportName,
      OperatingSurface operatingSurface) {

    if (operatingSurface == OperatingSurface.PLATFORM_OPS) {
      return requestAuthBindingPolicy.ReqsAuthBindingPolicyResolvePlatformOpsDefault(
          transportName + "_platform_ops");
    }

    if (operatingSurface == OperatingSurface.APP_ADAPTER) {
      return requestAuthBindingPolicy.ReqsAuthBindingPolicyResolveAppPreSession(
          transportName + "_app_adapter");
    }

    if (jwtRequestBindingExtractor != null) {
      ReqsAuthBinding jwtBinding = jwtRequestBindingExtractor.JwtExtractBinding();
      if (jwtBinding != null) {
        return jwtBinding;
      }
    }

    return requestAuthBindingPolicy.ReqsAuthBindingPolicyResolveDirectMcpDefault(
        transportName + "_direct_mcp");
  }

  public HTTPTranspSess HttpSupCreateReqScopedSession(
      String transportName,
      HTTPReqsMetadata openingRequestMetadata,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding) {

    HTTPSupAssertSupportedTransp(transportName);

    return new HTTPTranspSess(
        transportName,
        HTTPSupGenerateReqScopedConnId(transportName),
        sessionIdGenerator.generate(),
        config.HTTPCfgResolvePrimaryEndpointPath(transportName),
        openingRequestMetadata,
        operatingSurface,
        HTTPSupRequireCompliantBinding(operatingSurface, requestAuthBinding),
        runtimeMetaFactory);
  }

  private ReqsAuthBinding HTTPSupRequireCompliantBinding(
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding) {

    return requestBindingComplianceGuard.ReqsBindingComplianceGuardRequireCompliant(
        operatingSurface,
        requestAuthBinding);
  }

  public String HTTPSupGenerateConnId(String transportName) {
    HTTPSupAssertSupportedTransp(transportName);
    return transportName + ":" + UUID.randomUUID();
  }

  public String HTTPSupGenerateReqScopedConnId(String transportName) {
    HTTPSupAssertSupportedTransp(transportName);
    return transportName + TranspSess.REQUEST_SCOPED_CONN_MARKER + UUID.randomUUID();
  }

  public String HTTPSupResolveEndpointPath(String transportName) {
    return config.HTTPCfgResolvePrimaryEndpointPath(transportName);
  }

  public boolean HTTPSupRequiresOriginValidation(String transportName) {
    return config.HTTPCfgRequiresOriginValidation(transportName);
  }

  public boolean HTTPSupIsLocalhostOnly(String transportName) {
    return config.HTTPCfgIsLocalhostOnly(transportName);
  }

  public boolean HTTPSupIsTrustedExternalEdgeRequired() {
    return config.HTTPCfgGetTrustedEdge().required();
  }

  public boolean HTTPSupAllowSessDelete() {
    return config.HTTPCfgGetStreamableHTTP().allowSessionDelete();
  }

  public boolean HTTPSupRequireProtocolVersionHeaderAfterInit() {
    return config.HTTPCfgGetStreamableHTTP().requireProtocolVersionHeaderAfterInit();
  }

  public int HTTPSupStreamableMaxActiveSessions() {
    return config.HTTPCfgGetStreamableHTTP().maxActiveSessions();
  }

  public int HTTPSupStreamableMaxActiveStreams() {
    return config.HTTPCfgGetStreamableHTTP().maxActiveStreams();
  }

  public int HTTPSupStreamableMaxBufferedOutbMessagesPerSess() {
    return config.HTTPCfgGetStreamableHTTP().maxBufferedOutboundMessagesPerSession();
  }

  public long HTTPSupStreamablePostRespTimeoutMillis() {
    return config.HTTPCfgGetStreamableHTTP().postResponseTimeoutMillis();
  }

  public void HttpSupAssertOriginAllowed(
      String transportName,
      HTTPReqsMetadata requestMetadata) {

    Objects.requireNonNull(requestMetadata, "requestMetadata");

    if (!HTTPSupRequiresOriginValidation(transportName)) {
      return;
    }

    if (!requestMetadata.HTTPReqMetaHasOriginHeader()) {
      if (HTTPSupIsLocalhostOnly(transportName) && HTTPSupIsLoopbackReqs(requestMetadata)) {
        return;
      }
      throw new IllegalArgumentException(
          "HTTP transport " + transportName + " requires Origin header");
    }

    if (HTTPSupIsLocalhostOnly(transportName) && !HTTPSupIsLocalOrigin(requestMetadata.originHeader())) {
      throw new IllegalArgumentException(
          "HTTP transport " + transportName + " only accepts localhost Origin values");
    }

    String[] allowedOrigins = config.HTTPCfgResolveAllowedOrigins(transportName);
    if (allowedOrigins.length == 0) {
      return;
    }

    String normalizedOrigin = HTTPSupNormalizeOrigin(requestMetadata.originHeader());
    if (normalizedOrigin == null) {
      throw new IllegalArgumentException(
          "HTTP transport " + transportName + " requires an allowlisted Origin value");
    }

    for (String allowedOrigin : allowedOrigins) {
      if (allowedOrigin.equals(normalizedOrigin)) {
        return;
      }
    }

    throw new IllegalArgumentException(
        "HTTP transport " + transportName + " denied non-allowlisted Origin " + normalizedOrigin);
  }

  public void HttpSupAssertLocalhostAllowed(
      String transportName,
      HTTPReqsMetadata requestMetadata) {

    Objects.requireNonNull(requestMetadata, "requestMetadata");

    if (!HTTPSupIsLocalhostOnly(transportName)) {
      return;
    }

    if (!HTTPSupIsLoopbackReqs(requestMetadata)) {
      throw new IllegalArgumentException(
          "HTTP transport "
              + transportName
              + " is configured as localhost-only but request came from "
              + requestMetadata.HTTPReqMetaPreferredClientAddress());
    }
  }

  public void HttpSupAssertTrustedEdgeSatisfied(
      String transportName,
      HTTPReqsMetadata requestMetadata) {

    Objects.requireNonNull(requestMetadata, "requestMetadata");

    if (!HTTPSupIsTrustedExternalEdgeRequired()) {
      return;
    }

    if (!requestMetadata.trustedForwardHeaders()) {
      throw new IllegalArgumentException(
          "HTTP transport " + transportName + " requires a trusted external edge");
    }

    if (requestMetadata.forwardedFor() == null || requestMetadata.forwardedFor().isBlank()) {
      throw new IllegalArgumentException(
          "HTTP transport " + transportName + " requires X-Forwarded-For from the trusted external edge");
    }

    if (config.HTTPCfgGetTrustedEdge().requireForwardedHttps()) {
      String preferredProto = requestMetadata.HTTPReqMetaPreferredProto();
      if (!"https".equalsIgnoreCase(preferredProto)) {
        throw new IllegalArgumentException(
            "HTTP transport " + transportName + " requires X-Forwarded-Proto=https from the trusted external edge");
      }
    }
  }

  public void HttpSupAssertHostAllowed(
      String transportName,
      HTTPReqsMetadata requestMetadata) {

    Objects.requireNonNull(requestMetadata, "requestMetadata");

    String[] allowedHosts = config.HTTPCfgResolveAllowedHosts(transportName);
    if (allowedHosts.length == 0) {
      return;
    }

    String preferredHost = HTTPSupNormalizeHost(requestMetadata.HTTPReqMetaPreferredHost());
    if (preferredHost == null) {
      throw new IllegalArgumentException(
          "HTTP transport " + transportName + " requires an allowlisted Host value");
    }

    for (String allowedHost : allowedHosts) {
      if (HTTPSupHostMatchesAllowed(preferredHost, allowedHost)) {
        return;
      }
    }

    throw new IllegalArgumentException(
        "HTTP transport " + transportName + " denied non-allowlisted Host " + preferredHost);
  }

  public boolean HTTPSupIsLoopbackReqs(HTTPReqsMetadata requestMetadata) {

    Objects.requireNonNull(requestMetadata, "requestMetadata");

    String preferredAddress = requestMetadata.HTTPReqMetaPreferredClientAddress();
    if (preferredAddress == null) {
      return false;
    }

    String normalized = preferredAddress.trim().toLowerCase();
    return "127.0.0.1".equals(normalized)
        || "::1".equals(normalized)
        || "0:0:0:0:0:0:0:1".equals(normalized)
        || "localhost".equals(normalized);
  }

  public boolean HTTPSupIsLocalOrigin(String originHeader) {

    if (originHeader == null || originHeader.isBlank()) {
      return false;
    }

    String normalized = originHeader.trim().toLowerCase();
    return normalized.startsWith("http://localhost")
        || normalized.startsWith("https://localhost")
        || normalized.startsWith("http://127.0.0.1")
        || normalized.startsWith("https://127.0.0.1")
        || normalized.startsWith("http://[::1]")
        || normalized.startsWith("https://[::1]");
  }

  public boolean HTTPSupIsTrustedProxyAddress(String remoteAddress) {

    String normalizedAddress = normalizeText(remoteAddress);
    if (normalizedAddress == null) {
      return false;
    }

    for (String rule : config.HTTPCfgGetTrustedEdge().trustedProxyAddresses()) {
      if (HTTPSupAddressMatchesRule(normalizedAddress, rule)) {
        return true;
      }
    }

    return false;
  }

  public void HTTPSupAssertSupportedTransp(String transportName) {
    if (!config.HTTPCfgIsSupportedTransp(transportName)) {
      throw new IllegalArgumentException("Unsupported HTTP transport: " + transportName);
    }
  }

  private static String HTTPSupBuildReqsUri(HttpServletRequest request) {

    String requestUri = request.getRequestURI();
    String queryString = request.getQueryString();

    if (queryString == null || queryString.isBlank()) {
      return requestUri;
    }

    return requestUri + "?" + queryString;
  }

  private static String HTTPSupResolveHostHeader(HttpServletRequest request) {

    String explicitHostHeader = request.getHeader(HEADER_HOST);
    if (explicitHostHeader != null && !explicitHostHeader.isBlank()) {
      return explicitHostHeader;
    }

    String serverName = request.getServerName();
    int serverPort = request.getServerPort();
    if (serverName == null || serverName.isBlank()) {
      return null;
    }

    return serverPort > 0 ? serverName + ":" + serverPort : serverName;
  }

  private static String HTTPSupNormalizeOrigin(String originHeader) {
    String normalized = normalizeText(originHeader);
    if (normalized == null) {
      return null;
    }

    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }

    return normalized;
  }

  private static String HTTPSupNormalizeHost(String hostHeader) {
    String normalized = normalizeText(hostHeader);
    if (normalized == null) {
      return null;
    }

    if (normalized.startsWith("[")) {
      int closingBracket = normalized.indexOf(']');
      if (closingBracket > 0) {
        return normalized.substring(0, closingBracket + 1);
      }
    }

    int colonIdx = normalized.indexOf(':');
    if (colonIdx > 0) {
      return normalized.substring(0, colonIdx);
    }

    return normalized;
  }

  private static boolean HTTPSupHostMatchesAllowed(String normalizedHost, String allowedHost) {
    String normalizedAllowed = normalizeText(allowedHost);
    if (normalizedAllowed == null) {
      return false;
    }

    if (normalizedAllowed.contains(":")) {
      String strippedAllowedHost = HTTPSupNormalizeHost(normalizedAllowed);
      return normalizedAllowed.equals(normalizedHost)
          || (strippedAllowedHost != null && strippedAllowedHost.equals(normalizedHost));
    }

    return normalizedAllowed.equals(normalizedHost);
  }

  private static String normalizeText(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim().toLowerCase();
    return normalized.isBlank() ? null : normalized;
  }

  private static boolean HTTPSupAddressMatchesRule(String candidateAddress, String rule) {
    String normalizedRule = normalizeText(rule);
    if (normalizedRule == null) {
      return false;
    }

    if (normalizedRule.equals(candidateAddress)) {
      return true;
    }

    int slashIdx = normalizedRule.indexOf('/');
    if (slashIdx < 0) {
      return false;
    }

    try {
      InetAddress candidate = InetAddress.getByName(candidateAddress);
      InetAddress network = InetAddress.getByName(normalizedRule.substring(0, slashIdx));
      int prefixLength = Integer.parseInt(normalizedRule.substring(slashIdx + 1));
      return cidrContains(network.getAddress(), candidate.getAddress(), prefixLength);
    } catch (RuntimeException | java.net.UnknownHostException ex) {
      return false;
    }
  }

  private static boolean cidrContains(byte[] networkBytes, byte[] candidateBytes, int prefixLength) {
    if (networkBytes.length != candidateBytes.length) {
      return false;
    }

    if (prefixLength < 0 || prefixLength > networkBytes.length * 8) {
      return false;
    }

    int wholeBytes = prefixLength / 8;
    int remainingBits = prefixLength % 8;

    for (int idx = 0; idx < wholeBytes; idx++) {
      if (networkBytes[idx] != candidateBytes[idx]) {
        return false;
      }
    }

    if (remainingBits == 0) {
      return true;
    }

    int mask = 0xFF << (8 - remainingBits);
    return (networkBytes[wholeBytes] & mask) == (candidateBytes[wholeBytes] & mask);
  }
}
