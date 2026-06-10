/**
 * Readiness gate primitives for formal operational go/no-go decisions.
 *
 * <p>This package stays intentionally small and is meant to be wired by the ops/controller
 * layer. It aggregates existing health, runtime, triage, and diagnostics views instead of
 * duplicating them.</p>
 */
package mcp.server.foundation.observability.readiness;
