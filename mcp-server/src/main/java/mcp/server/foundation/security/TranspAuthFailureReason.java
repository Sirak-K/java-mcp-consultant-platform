package mcp.server.foundation.security;

public enum TranspAuthFailureReason {
  MISSING_HEADER,
  MALFORMED_HEADER,
  UNSUPPORTED_SCHEME,
  INVALID_TOKEN
}
