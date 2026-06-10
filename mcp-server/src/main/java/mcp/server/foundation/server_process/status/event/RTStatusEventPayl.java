package mcp.server.foundation.server_process.status.event;

import mcp.server.foundation.server_process.status.RTStatus;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RTStatusEventPayl {

  private final RTStatus.ServerState state;
  private final Instant timestamp;

  /**
   * rpcFallbackId
   *
   * Syfte:
   * - Korrelation för JSON-RPC notifications (utan "id") som servern emitterar.
   *
   * Kontrakt:
   * - Alltid UUID-format.
   * - Existerar endast i params (notification har aldrig root-level "id").
   */
  private final String rpcFallbackId;

  public RTStatusEventPayl(RTStatus.ServerState state) {
    this.state = Objects.requireNonNull(state, "state");
    this.timestamp = Instant.now();
    this.rpcFallbackId = UUID.randomUUID().toString();
  }

  public RTStatus.ServerState getState() {
    return state;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getRPCFallbackId() {
    return rpcFallbackId;
  }

  public Map<String, Object> toParams() {
    return Map.of(
        "runtimeState", state.name(),
        "timestamp", timestamp.toString(),
        "rpcFallbackId", rpcFallbackId);
  }
}