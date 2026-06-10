package mcp.server.foundation.transport;

import mcp.server.foundation.rpc.RPCReqsPayl;
import mcp.server.foundation.rpc.RPCRespPayl;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * TranspAdap
 *
 * Abstraktion över underliggande transport (WebSocket, TCP, etc).
 *
 * Foundation-level.
 */
public interface TranspAdap {

  String TranspAdapGetTranspName();

  // =========================================================
  // Lifecycle
  // =========================================================

  void TranspAdapStart();

  void TranspAdapStop();

  // =========================================================
  // Messaging
  // =========================================================

  /**
   * Register inbound raw message handler.
   *
   * session = transport-level session
   * message = raw JSON payload
   */
  void TranspAdapSetMessageHandler(
      BiConsumer<TranspSess, String> handler);

  /**
   * Send raw JSON message to session.
   */
  void TranspAdapSendTo(
      TranspSess session,
      String message);

  /**
   * Lookup session by ID.
   */
  TranspSess TranspAdapGetSessionById(String sessionId);

  /**
   * Best-effort transport-side close/cleanup for a logical session.
   */
  default boolean TranspAdapCloseSessById(String sessionId) {
    return false;
  }

  /**
   * Exposes the transport's configured operational limits.
   */
  default Map<String, Long> TranspAdapDescribeCapacityProfile() {
    return Map.of();
  }

  /**
   * Canonical transport parse-error contract.
   */
  default RPCRespPayl TranspAdapMapParseErr(RuntimeException exception) {
    return TranspContractSupport.TransContMapParseErr(exception);
  }

  /**
   * Canonical transport overload contract.
   */
  default RPCRespPayl TranspAdapMapOverload(RPCReqsPayl request) {
    return TranspContractSupport.TransContMapOverload(request);
  }

  /**
   * Canonical transport timeout contract.
   */
  default RPCRespPayl TranspAdapMapTimeout(RPCReqsPayl request) {
    return TranspContractSupport.TransContMapTimeout(request);
  }

  /**
   * Canonical metric prefix for the active transport family.
   */
  default String TranspAdapMetricPrefix() {
    return TranspContractSupport.TransContMetricPrefix(TranspAdapGetTranspName());
  }

  /**
   * Canonical normalization for transport connection ids used in logs and
   * lifecycle events.
   */
  default String TranspAdapNormalizeConnId(String transportConnectionId) {
    return TranspContractSupport.TransContNormalizeConnId(transportConnectionId);
  }

  // =========================================================
  // Optional Session Lifecycle Hooks
  // =========================================================

  /**
   * Called when a new transport session is opened.
   */
  default void TranspAdapSetSessionOpenHandler(
      Consumer<TranspSess> handler) {
    // no-op
  }

  /**
   * Called when a transport session is closed.
   */
  default void TranspAdapSetSessionCloseHandler(
      Consumer<TranspSess> handler) {
    // no-op
  }
}
