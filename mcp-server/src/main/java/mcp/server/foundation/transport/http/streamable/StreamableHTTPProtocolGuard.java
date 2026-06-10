package mcp.server.foundation.transport.http.streamable;

import jakarta.servlet.http.HttpServletRequest;
import mcp.server.foundation.rpc.RPCCapaDscr;
import mcp.server.foundation.transport.http.shared.HTTPTranspSupport;

import java.util.Objects;

final class StreamableHTTPProtocolGuard {

  private StreamableHTTPProtocolGuard() {
  }

  static String sessionHeader(HttpServletRequest request) {
    Objects.requireNonNull(request, "request");
    return request.getHeader(HTTPTranspSupport.HEADER_MCP_SESSION_ID);
  }

  static boolean hasSessionHeader(String sessionHeader) {
    return sessionHeader != null && !sessionHeader.isBlank();
  }

  static String requireSessionHeader(String sessionHeader) {
    if (!hasSessionHeader(sessionHeader)) {
      throw new IllegalArgumentException("MCP-Session-Id header is required");
    }
    String normalizedSessionHeader = sessionHeader.trim();
    for (int idx = 0; idx < normalizedSessionHeader.length(); idx++) {
      char candidate = normalizedSessionHeader.charAt(idx);
      if (candidate < 0x21 || candidate > 0x7E) {
        throw new IllegalArgumentException("MCP-Session-Id header must contain visible ASCII characters only");
      }
    }
    return normalizedSessionHeader;
  }

  static void validatePostAcceptHeader(HttpServletRequest request) {
    requireAccepts(request, HTTPTranspSupport.CONTENT_TYPE_JSON);
    requireAccepts(request, HTTPTranspSupport.CONTENT_TYPE_SSE);
  }

  static void validateGetAcceptHeader(HttpServletRequest request) {
    requireAccepts(request, HTTPTranspSupport.CONTENT_TYPE_SSE);
  }

  static void validateAfterInit(
      HTTPTranspSupport httpTranspSupport,
      HttpServletRequest request,
      String sessionHeader) {

    Objects.requireNonNull(httpTranspSupport, "httpTranspSupport");
    Objects.requireNonNull(request, "request");

    if (!hasSessionHeader(sessionHeader)) {
      return;
    }

    if (!httpTranspSupport.HTTPSupRequireProtocolVersionHeaderAfterInit()) {
      return;
    }

    String protocolVersion = request.getHeader(HTTPTranspSupport.HEADER_MCP_PROTOCOL_VERSION);
    if (protocolVersion == null || protocolVersion.isBlank()) {
      throw new IllegalArgumentException("Missing MCP-Protocol-Version header");
    }

    if (!RPCCapaDscr.RPCCapaDescIsSupportedProtocolVersion(protocolVersion)) {
      throw new IllegalArgumentException("Unsupported MCP-Protocol-Version header");
    }
  }

  private static void requireAccepts(
      HttpServletRequest request,
      String requiredContentType) {

    String acceptHeader = request.getHeader("Accept");
    if (!accepts(acceptHeader, requiredContentType)) {
      throw new IllegalArgumentException("Accept header must include " + requiredContentType);
    }
  }

  private static boolean accepts(
      String acceptHeader,
      String requiredContentType) {

    if (acceptHeader == null || acceptHeader.isBlank()) {
      return false;
    }

    String required = requiredContentType.toLowerCase(java.util.Locale.ROOT);
    String requiredType = required.substring(0, required.indexOf('/'));

    for (String rawPart : acceptHeader.split(",")) {
      String candidate = rawPart.trim().toLowerCase(java.util.Locale.ROOT);
      int parameterDelimiter = candidate.indexOf(';');
      if (parameterDelimiter >= 0) {
        candidate = candidate.substring(0, parameterDelimiter).trim();
      }

      if (candidate.equals(required) || candidate.equals("*/*") || candidate.equals(requiredType + "/*")) {
        return true;
      }
    }

    return false;
  }
}
