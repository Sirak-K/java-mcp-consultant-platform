package mcp.server.foundation.rpc;

/**
 * RPCMetPol
 *
 * RPC-layer policy for pre-initialization gating.
 *
 * Invariant:
 * - Stateless
 * - No McpSessReg
 * - No IO
 *
 * Contract:
 * - requiresInitialization(method) == false for allowlisted pre-init methods.
 */
public final class RPCMetPol {

  /**
   * Public API with unchanged signature for call-sites.
   */
  public boolean RPCMetPolRequiresInitialization(String method) {
    return requiresInitialization(method);
  }

  /**
   * Returns true if the given method requires an INITIALIZED session.
   *
   * @param method JSON-RPC method name
   * @return true if initialization is required, otherwise false
   */
  public boolean requiresInitialization(String method) {

    if (method == null || method.isBlank()) {
      // Defensive default: unknown/blank methods should be gated.
      return true;
    }

    return !RPCMetName.RPCMetNameIsPreInitAllowed(method);
  }
}
