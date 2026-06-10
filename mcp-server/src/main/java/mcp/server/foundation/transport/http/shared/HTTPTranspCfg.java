package mcp.server.foundation.transport.http.shared;

import java.util.Objects;

/**
 * Shared immutable config for the HTTP transport family.
 */
public final class HTTPTranspCfg {

  public static final String TRANSPORT_STREAMABLE_HTTP = "streamable-http";

  public record TrustedEdgeSettings(
      boolean required,
      boolean requireForwardedHttps,
      String[] trustedProxyAddresses) {

    public TrustedEdgeSettings {
      trustedProxyAddresses = normalizeValues(trustedProxyAddresses, "trustedProxyAddresses", false);
    }
  }

  public record StreamableHTTPSettings(
      boolean enabled,
      String endpointPath,
      boolean requireOriginValidation,
      boolean localhostOnly,
      boolean allowSessionDelete,
      boolean requireProtocolVersionHeaderAfterInit,
      int maxActiveSessions,
      int maxActiveStreams,
      int maxBufferedOutboundMessagesPerSession,
      long postResponseTimeoutMillis,
      String[] allowedOrigins,
      String[] allowedHosts) {

    public StreamableHTTPSettings(
        boolean enabled,
        String endpointPath,
        boolean requireOriginValidation,
        boolean localhostOnly,
        boolean allowSessionDelete,
        boolean requireProtocolVersionHeaderAfterInit,
        int maxActiveSessions,
        int maxActiveStreams,
        int maxBufferedOutboundMessagesPerSession,
        long postResponseTimeoutMillis) {

      this(
          enabled,
          endpointPath,
          requireOriginValidation,
          localhostOnly,
          allowSessionDelete,
          requireProtocolVersionHeaderAfterInit,
          maxActiveSessions,
          maxActiveStreams,
          maxBufferedOutboundMessagesPerSession,
          postResponseTimeoutMillis,
          new String[0],
          new String[0]);
    }

    public StreamableHTTPSettings {
      endpointPath = normalizePath(endpointPath);
      maxActiveSessions = requirePositive(maxActiveSessions, "maxActiveSessions");
      maxActiveStreams = requirePositive(maxActiveStreams, "maxActiveStreams");
      maxBufferedOutboundMessagesPerSession = requirePositive(
          maxBufferedOutboundMessagesPerSession,
          "maxBufferedOutboundMessagesPerSession");
      postResponseTimeoutMillis = requirePositive(postResponseTimeoutMillis, "postResponseTimeoutMillis");
      allowedOrigins = normalizeValues(allowedOrigins, "allowedOrigins", true);
      allowedHosts = normalizeValues(allowedHosts, "allowedHosts", true);
    }
  }

  private final TrustedEdgeSettings trustedEdge;
  private final StreamableHTTPSettings streamableHttp;

  public HTTPTranspCfg(
      TrustedEdgeSettings trustedEdge,
      StreamableHTTPSettings streamableHttp) {

    this.trustedEdge = Objects.requireNonNull(trustedEdge, "trustedEdge");
    this.streamableHttp = Objects.requireNonNull(streamableHttp, "streamableHttp");
  }

  public TrustedEdgeSettings HTTPCfgGetTrustedEdge() {
    return trustedEdge;
  }

  public StreamableHTTPSettings HTTPCfgGetStreamableHTTP() {
    return streamableHttp;
  }

  public boolean HTTPCfgIsAnyHTTPTranspEnabled() {
    return streamableHttp.enabled();
  }

  public boolean HTTPCfgIsSupportedTransp(String transportName) {
    return TRANSPORT_STREAMABLE_HTTP.equals(transportName);
  }

  public boolean HTTPCfgIsEnabled(String transportName) {
    return switch (transportName) {
      case TRANSPORT_STREAMABLE_HTTP -> streamableHttp.enabled();
      default -> false;
    };
  }

  public String HTTPCfgResolvePrimaryEndpointPath(String transportName) {
    return switch (transportName) {
      case TRANSPORT_STREAMABLE_HTTP -> streamableHttp.endpointPath();
      default -> throw new IllegalArgumentException("Unsupported HTTP transport: " + transportName);
    };
  }

  public boolean HTTPCfgRequiresOriginValidation(String transportName) {
    return switch (transportName) {
      case TRANSPORT_STREAMABLE_HTTP -> streamableHttp.requireOriginValidation();
      default -> throw new IllegalArgumentException("Unsupported HTTP transport: " + transportName);
    };
  }

  public boolean HTTPCfgIsLocalhostOnly(String transportName) {
    return switch (transportName) {
      case TRANSPORT_STREAMABLE_HTTP -> streamableHttp.localhostOnly();
      default -> throw new IllegalArgumentException("Unsupported HTTP transport: " + transportName);
    };
  }

  public String[] HTTPCfgResolveAllowedOrigins(String transportName) {
    return switch (transportName) {
      case TRANSPORT_STREAMABLE_HTTP -> streamableHttp.allowedOrigins();
      default -> throw new IllegalArgumentException("Unsupported HTTP transport: " + transportName);
    };
  }

  public String[] HTTPCfgResolveAllowedHosts(String transportName) {
    return switch (transportName) {
      case TRANSPORT_STREAMABLE_HTTP -> streamableHttp.allowedHosts();
      default -> throw new IllegalArgumentException("Unsupported HTTP transport: " + transportName);
    };
  }

  public static String normalizePath(String rawPath) {

    Objects.requireNonNull(rawPath, "rawPath");

    String normalized = rawPath.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Path must not be blank");
    }

    if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }

    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }

    return normalized;
  }

  private static int requirePositive(int value, String fieldName) {
    if (value <= 0) {
      throw new IllegalArgumentException(fieldName + " must be > 0");
    }
    return value;
  }

  private static long requirePositive(long value, String fieldName) {
    if (value <= 0L) {
      throw new IllegalArgumentException(fieldName + " must be > 0");
    }
    return value;
  }

  private static String[] normalizeValues(
      String[] rawValues,
      String fieldName,
      boolean lowerCase) {

    Objects.requireNonNull(rawValues, fieldName);

    return java.util.Arrays.stream(rawValues)
        .map(value -> value == null ? "" : value.trim())
        .filter(value -> !value.isBlank())
        .map(value -> lowerCase ? value.toLowerCase() : value)
        .peek(value -> {
          if ("*".equals(value)) {
            throw new IllegalArgumentException(fieldName + " must not contain wildcard '*'");
          }
        })
        .distinct()
        .toArray(String[]::new);
  }
}
