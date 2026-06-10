package mcp.server.foundation.server_process.client_context.session;

import mcp.server.foundation.rpc.RPCMappedExcep;
import mcp.server.foundation.rpc.RPCMetName;
import mcp.server.foundation.rpc.RPCSessPhase;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * McpSessStateVerif
 *
 * Ansvar:
 * - Verifiera att en session får utföra en viss RPC-metod.
 * - Implementerar den canonical RPC gating policy för MCP sessioner.
 *
 * Viktigt:
 * - Klassen muterar aldrig session state.
 * - Den endast verifierar state enligt registry.
 *
 * Ontologisk roll:
 * Logical MCP Session Verification Layer
 */
public final class McpSessStateVerif {

  private static final Duration PRE_INIT_TTL = Duration.ofSeconds(10);

  private final McpSessReg sessionRegistry;

  public McpSessStateVerif(McpSessReg sessionRegistry) {
    this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry");
  }

  // =========================================================
  // CANONICAL RPC VERIFICATION
  // =========================================================

  /**
   * Canonical verifiering som används av RPCRouter.
   *
   * Policy:
   *
   * PRE_INIT:
   * tillåtet enligt RPCMetName pre-init policy.
   *
   * POST_INIT:
   * alla metoder tillåtna
   *
   * CLOSED:
   * inga metoder tillåtna
   */
  public void SessStateVerifVerifyRequestAllowed(
      McpSessId sessionId,
      String rpcMet) {

    if (sessionId == null) {
      throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "McpSessId missing");
    }

    if (rpcMet == null || rpcMet.isBlank()) {
      throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "RPC method missing");
    }

    RPCSessPhase phase = sessionRegistry.SessRegGetPhase(sessionId);

    switch (phase) {

      case CLOSED -> throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "Session closed");

      case PRE_INIT -> verifyPreInitAllowed(sessionId, rpcMet);

      case POST_INIT -> {
        // Full access
      }
    }
  }

  // =========================================================
  // PRE_INIT POLICY
  // =========================================================

  private void verifyPreInitAllowed(
      McpSessId sessionId,
      String rpcMet) {

    if (RPCMetName.RPCMetNameIsPreInitAllowed(rpcMet)) {
      return;
    }

    // PRE_INIT TTL guard

    var meta = sessionRegistry.SessRegGetMetadata(sessionId);

    if (meta != null) {

      Instant createdAt = meta.createdAt();

      if (createdAt != null && !Instant.EPOCH.equals(createdAt)) {

        Duration age = Duration.between(createdAt, Instant.now());

        if (age.compareTo(PRE_INIT_TTL) > 0) {
          throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
              "Session pre-init expired");
        }
      }
    }

    throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
        "RPC method not allowed before initialize");
  }
}
