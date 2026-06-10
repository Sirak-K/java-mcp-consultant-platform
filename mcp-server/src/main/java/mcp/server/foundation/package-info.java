/**
 * Foundation is the server's execution plane by default.
 *
 * <p>It owns transport, ingress protection, orchestration and request-bound runtime behavior.
 * Persistence-specific customer data concerns must not leak into foundation packages.
 *
 * <p>Platform control plane behavior starts under {@code mcp.server.foundation.observability}.
 */
package mcp.server.foundation;
