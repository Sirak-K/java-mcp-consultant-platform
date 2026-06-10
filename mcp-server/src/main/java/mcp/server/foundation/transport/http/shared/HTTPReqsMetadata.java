package mcp.server.foundation.transport.http.shared;

import java.util.Objects;

/**
 * Shared request metadata captured at the HTTP boundary.
 */
public record HTTPReqsMetadata(
    String requestId,
    String requestMethod,
    String requestUri,
    String acceptHeader,
    String contentType,
    String originHeader,
    String hostHeader,
    String remoteAddress,
    String forwardedFor,
    String forwardedHost,
    String forwardedProto,
    boolean trustedForwardHeaders) {

  public HTTPReqsMetadata(
      String requestId,
      String requestMethod,
      String requestUri,
      String acceptHeader,
      String contentType,
      String originHeader,
      String remoteAddress,
      String forwardedFor) {

    this(
        requestId,
        requestMethod,
        requestUri,
        acceptHeader,
        contentType,
        originHeader,
        null,
        remoteAddress,
        forwardedFor,
        null,
        null,
        false);
  }

  public HTTPReqsMetadata {
    requestId = requireTrimmed(requestId, "requestId");
    requestMethod = requireTrimmed(requestMethod, "requestMethod");
    requestUri = requireTrimmed(requestUri, "requestUri");
    acceptHeader = trimToNull(acceptHeader);
    contentType = trimToNull(contentType);
    originHeader = trimToNull(originHeader);
    hostHeader = trimToNull(hostHeader);
    remoteAddress = trimToNull(remoteAddress);
    forwardedFor = trimToNull(forwardedFor);
    forwardedHost = trimToNull(forwardedHost);
    forwardedProto = trimToNull(forwardedProto);
  }

  public String HTTPReqMetaPreferredClientAddress() {
    if (!trustedForwardHeaders) {
      return remoteAddress;
    }

    String preferredForwarded = firstCsvValue(forwardedFor);
    if (preferredForwarded == null || preferredForwarded.isBlank()) {
      return remoteAddress;
    }

    return preferredForwarded;
  }

  public String HTTPReqMetaPreferredHost() {
    if (trustedForwardHeaders) {
      String preferredForwardedHost = firstCsvValue(forwardedHost);
      if (preferredForwardedHost != null && !preferredForwardedHost.isBlank()) {
        return preferredForwardedHost;
      }
    }

    return hostHeader;
  }

  public String HTTPReqMetaPreferredProto() {
    if (!trustedForwardHeaders) {
      return null;
    }

    return firstCsvValue(forwardedProto);
  }

  public boolean HTTPReqMetaHasOriginHeader() {
    return originHeader != null && !originHeader.isBlank();
  }

  private static String firstCsvValue(String value) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      return null;
    }

    int commaIdx = trimmed.indexOf(',');
    if (commaIdx < 0) {
      return trimmed.trim();
    }

    return trimmed.substring(0, commaIdx).trim();
  }

  private static String requireTrimmed(String value, String fieldName) {

    Objects.requireNonNull(value, fieldName);

    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return trimmed;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }

    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
