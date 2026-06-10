package mcp.server.foundation.server_process.client_context.session;

import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.transport.TranspSess;
import mcp.server.foundation.transport.websocket.WsConnId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * McpSessBindingReg
 *
 * Logical (MCP) ↔ Physical (WS) bijection.
 *
 * Ansvar:
 * - Hantera koppling mellan MCP-session och WS-connection.
 * - Säkerställa 1:1 relation mellan logical och physical id.
 *
 * Viktiga invariants:
 *
 * 1. En MCP-session får ha max en WS-connection.
 * 2. En WS-connection får ha max en MCP-session.
 * 3. Binding-layer är separerad från session-registry.
 *
 * Concurrency modell:
 *
 * - ConcurrentHashMap för lookup
 * - Single lock för atomiska bijection updates
 */
public final class McpSessBindingReg {

  public record TranspBinding(String transportName, String transportConnectionId) {
  }

  private final ConcurrentHashMap<McpSessId, TranspBinding> logicalToPhysical = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, McpSessId> physicalToLogical = new ConcurrentHashMap<>();

  private final Object lock = new Object();

  // =========================================================
  // BIND
  // =========================================================

  public void bind(McpSessId sessionId, TranspSess transportSession) {

    Objects.requireNonNull(transportSession, "transportSession");

    bind(
        sessionId,
        transportSession.TranspSessGetTranspName(),
        transportSession.TranspSessGetTranspConnId());
  }

  public void bind(
      McpSessId sessionId,
      String transportName,
      String transportConnectionId) {

    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(transportName, "transportName");
    Objects.requireNonNull(transportConnectionId, "transportConnectionId");

    if (transportName.isBlank()) {
      throw new IllegalArgumentException("transportName must not be blank");
    }

    if (transportConnectionId.isBlank()) {
      throw new IllegalArgumentException("transportConnectionId must not be blank");
    }

    synchronized (lock) {

      TranspBinding newBinding = new TranspBinding(transportName, transportConnectionId);
      TranspBinding prevPhysical = logicalToPhysical.get(sessionId);

      if (prevPhysical != null && !prevPhysical.transportConnectionId().equals(transportConnectionId)) {
        physicalToLogical.remove(prevPhysical.transportConnectionId());
      }

      McpSessId prevLogical = physicalToLogical.get(transportConnectionId);

      if (prevLogical != null && !prevLogical.equals(sessionId)) {
        logicalToPhysical.remove(prevLogical);
      }

      logicalToPhysical.put(sessionId, newBinding);
      physicalToLogical.put(transportConnectionId, sessionId);
    }
  }

  /**
   * Bind logical MCP session till WS connection.
   *
   * Contract:
   * - Atomisk bijection update.
   * - Tidigare kopplingar ersätts deterministiskt.
   */
  public void bind(McpSessId sessionId, WsConnId connectionId) {
    bind(sessionId, TranspSess.TRANSPORT_WEBSOCKET, Objects.requireNonNull(connectionId, "connectionId").asString());
  }

  // =========================================================
  // UNBIND
  // =========================================================

  /**
   * Unbind via WS connection.
   *
   * Används när transport disconnect inträffar.
   */
  public void unbindByConn(WsConnId connectionId) {
    unbindByTranspConnId(Objects.requireNonNull(connectionId, "connectionId").asString());
  }

  public void unbindByTranspConnId(String transportConnectionId) {

    Objects.requireNonNull(transportConnectionId, "transportConnectionId");

    synchronized (lock) {

      McpSessId logical = physicalToLogical.remove(transportConnectionId);

      if (logical != null) {
        logicalToPhysical.remove(logical);
      }
    }
  }

  /**
   * Unbind via MCP session.
   *
   * Används vid session close.
   */
  public void unbind(McpSessId sessionId) {

    Objects.requireNonNull(sessionId, "sessionId");

    synchronized (lock) {

      TranspBinding physical = logicalToPhysical.remove(sessionId);

      if (physical != null) {
        physicalToLogical.remove(physical.transportConnectionId());
      }
    }
  }

  // =========================================================
  // LOOKUP
  // =========================================================

  public Optional<WsConnId> getConn(McpSessId sessionId) {

    return Optional.ofNullable(getConnId(sessionId));
  }

  public WsConnId getConnId(McpSessId sessionId) {

    Objects.requireNonNull(sessionId, "sessionId");

    TranspBinding binding = logicalToPhysical.get(sessionId);
    if (binding == null) {
      return null;
    }

    if (!TranspSess.TRANSPORT_WEBSOCKET.equals(binding.transportName())) {
      return null;
    }

    return WsConnId.fromString(binding.transportConnectionId());
  }

  public TranspBinding getTranspBinding(McpSessId sessionId) {

    Objects.requireNonNull(sessionId, "sessionId");

    return logicalToPhysical.get(sessionId);
  }

  public String getTranspConnId(McpSessId sessionId) {

    Objects.requireNonNull(sessionId, "sessionId");

    TranspBinding binding = logicalToPhysical.get(sessionId);
    return binding == null ? null : binding.transportConnectionId();
  }

  public String getTranspName(McpSessId sessionId) {

    Objects.requireNonNull(sessionId, "sessionId");

    TranspBinding binding = logicalToPhysical.get(sessionId);
    return binding == null ? null : binding.transportName();
  }

  public Optional<McpSessId> getSess(WsConnId connectionId) {

    return Optional.ofNullable(getSessByTranspConnId(
        Objects.requireNonNull(connectionId, "connectionId").asString()));
  }

  public McpSessId getSessByTranspConnId(String transportConnectionId) {

    Objects.requireNonNull(transportConnectionId, "transportConnectionId");

    return physicalToLogical.get(transportConnectionId);
  }

  // =========================================================
  // SNAPSHOT VIEW (Concurrency safe)
  // =========================================================

  /**
   * Snapshot av alla bindings.
   *
   * Viktigt:
   * - används för observability
   * - undviker iteration över live-map
   */
  public Map<McpSessId, String> getBindingsSnapshot() {

    Map<McpSessId, String> snapshot = new ConcurrentHashMap<>();

    for (Map.Entry<McpSessId, TranspBinding> entry : logicalToPhysical.entrySet()) {
      snapshot.put(entry.getKey(), entry.getValue().transportConnectionId());
    }

    return Map.copyOf(snapshot);
  }

  public Set<McpSessId> getSessIdsSnapshot() {
    return Set.copyOf(logicalToPhysical.keySet());
  }

  // =========================================================
  // STATUS
  // =========================================================

  public int getActiveBindingCount() {
    return physicalToLogical.size();
  }

  public boolean hasBinding(McpSessId sessionId) {

    Objects.requireNonNull(sessionId, "sessionId");

    return logicalToPhysical.containsKey(sessionId);
  }

  // =========================================================
  // RESET
  // =========================================================

  public void clearAll() {

    synchronized (lock) {
      logicalToPhysical.clear();
      physicalToLogical.clear();
    }
  }
}
