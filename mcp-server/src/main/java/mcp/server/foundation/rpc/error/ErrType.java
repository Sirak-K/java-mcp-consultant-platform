package mcp.server.foundation.rpc.error;

/**
 * Internal observability error classification.
 *
 * Contract:
 * - Intended for logs and metrics.
 * - Does not change the public JSON-RPC contract by itself.
 */
public enum ErrType {
  VALIDATION_ERROR,
  DOMAIN_ERROR,
  NOT_FOUND,
  CONFLICT,
  DATA_INTEGRITY_ERROR,
  TRANSPORT_ERROR,
  RPC_PROTOCOL_ERROR,
  THROTTLE_REJECTED,
  INTERNAL_ERROR
}
