package mcp.server.foundation.observability.readiness;

/**
 * Readiness outcome for a criterion or the full gate.
 */
public enum RTReadinessStatus {
  PASS,
  FAIL,
  NOT_APPLICABLE
}
