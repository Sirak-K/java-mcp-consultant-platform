package mcp.server.foundation.security;

public record StreamableHTTPReqsSizeLimitSettings(
    long maxRequestBodyBytes) {

  public StreamableHTTPReqsSizeLimitSettings {
    if (maxRequestBodyBytes <= 0L) {
      throw new IllegalArgumentException("maxRequestBodyBytes must be > 0");
    }
  }
}
