package mcp.server.foundation.logging;

/**
 * Simple local file rotation policy.
 */
public record LogRotationPolicy(
    long maxFileSizeBytes,
    int maxHistoryFiles) {

  public LogRotationPolicy {
    if (maxFileSizeBytes < 256L) {
      throw new IllegalArgumentException("maxFileSizeBytes must be at least 256");
    }
    if (maxHistoryFiles < 1) {
      throw new IllegalArgumentException("maxHistoryFiles must be at least 1");
    }
  }
}
