package mcp.server.foundation.server_process.client_context.session;

import mcp.server.foundation.rpc.RPCSessPhase;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessMetadata;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMeta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * McpSessReg
 *
 * Ansvar:
 * - Logical MCP session lifecycle
 * - Sentinel subscriptions
 * - Transp-loss separation
 *
 * Invariant:
 * - Logical lifecycle ≠ physical transport lifecycle
 *
 * Concurrency model:
 * - ConcurrentHashMap + atomic compute transitions
 * - No iteration over live map without snapshot
 */
public final class McpSessReg {

  private final Map<McpSessId, McpSessState> states = new ConcurrentHashMap<>();

  private final Map<McpSessId, Instant> createdAt = new ConcurrentHashMap<>();

  private final Set<McpSessId> sentinelIds = ConcurrentHashMap.newKeySet();

  private final Set<McpSessId> transportLostIds = ConcurrentHashMap.newKeySet();

  private final Map<McpSessId, McpSessRTMeta> runtimeMetas = new ConcurrentHashMap<>();

  // =========================================================
  // INTERNAL TRANSITION VALIDATION
  // =========================================================

  private static boolean isLegalTransition(McpSessState from, McpSessState to) {

    if (from == null) {
      return to == McpSessState.CONNECTED;
    }

    return switch (from) {
      case CONNECTED -> to == McpSessState.CONNECTED || to == McpSessState.INITIALIZED || to == McpSessState.CLOSED;
      case INITIALIZED -> to == McpSessState.INITIALIZED || to == McpSessState.CLOSED;
      case CLOSED -> to == McpSessState.CLOSED;
    };
  }

  // =========================================================
  // REGISTER / STATE TRANSITIONS (ATOMIC)
  // =========================================================

  public void SessRegRegisterConnected(McpSessId id) {

    Objects.requireNonNull(id, "id");

    transportLostIds.remove(id);

    states.compute(id, (k, current) -> {

      if (!isLegalTransition(current, McpSessState.CONNECTED)) {
        return current;
      }

      if (current == null) {
        return McpSessState.CONNECTED;
      }

      if (current == McpSessState.CLOSED) {
        return McpSessState.CLOSED;
      }

      return current;
    });

    createdAt.putIfAbsent(id, Instant.now());
  }

  public void SessRegMarkInitialized(McpSessId id) {

    Objects.requireNonNull(id, "id");

    states.computeIfPresent(id, (k, current) -> {

      if (!isLegalTransition(current, McpSessState.INITIALIZED)) {
        return current;
      }

      if (current == McpSessState.CONNECTED) {
        return McpSessState.INITIALIZED;
      }

      return current;
    });
  }

  public void SessRegTransitionClosed(McpSessId id) {

    if (id == null) {
      return;
    }

    transportLostIds.add(id);
    sentinelIds.remove(id);
  }

  public void SessRegCloseFinal(McpSessId id) {

    if (id == null) {
      return;
    }

    states.remove(id);
    createdAt.remove(id);
    sentinelIds.remove(id);
    transportLostIds.remove(id);
    runtimeMetas.remove(id);
  }

  public void SessRegMarkAsSentinel(McpSessId id) {

    Objects.requireNonNull(id, "id");

    if (!states.containsKey(id)) {
      return;
    }

    if (transportLostIds.contains(id)) {
      return;
    }

    sentinelIds.add(id);
  }

  public boolean SessRegTryMarkAsSentinel(McpSessId id) {

    if (id == null) {
      return false;
    }

    if (!states.containsKey(id)) {
      return false;
    }

    if (transportLostIds.contains(id)) {
      return false;
    }

    sentinelIds.add(id);

    return true;
  }

  public boolean SessRegTryMarkInitialized(McpSessId id) {

    if (id == null) {
      return false;
    }

    if (!states.containsKey(id)) {
      return false;
    }

    SessRegMarkInitialized(id);

    return SessRegIsInitialized(id);
  }

  public Set<McpSessId> SessRegGetAllSessIds() {
    return Set.copyOf(states.keySet());
  }

  public boolean SessRegTryMarkClosed(McpSessId id) {

    if (id == null) {
      return false;
    }

    final boolean[] existed = new boolean[] { false };

    states.computeIfPresent(id, (k, current) -> {

      existed[0] = true;

      if (!isLegalTransition(current, McpSessState.CLOSED)) {
        return current;
      }

      return McpSessState.CLOSED;
    });

    if (!existed[0]) {
      return false;
    }

    sentinelIds.remove(id);
    transportLostIds.remove(id);

    return true;
  }

  public void SessRegMarkTranspLost(McpSessId id) {
    SessRegTransitionClosed(id);
  }

  // =========================================================
  // PHASE
  // =========================================================

  public RPCSessPhase SessRegGetPhase(McpSessId id) {

    if (id == null) {
      return RPCSessPhase.CLOSED;
    }

    if (transportLostIds.contains(id)) {
      return RPCSessPhase.CLOSED;
    }

    McpSessState state = states.get(id);

    if (state == null) {
      return RPCSessPhase.CLOSED;
    }

    return switch (state) {
      case CONNECTED -> RPCSessPhase.PRE_INIT;
      case INITIALIZED -> RPCSessPhase.POST_INIT;
      case CLOSED -> RPCSessPhase.CLOSED;
    };
  }

  // =========================================================
  // VERIFICATION
  // =========================================================

  public boolean SessRegIsInitialized(McpSessId id) {

    if (id == null) {
      return false;
    }

    return states.get(id) == McpSessState.INITIALIZED;
  }

  public boolean SessRegIsTranspLost(McpSessId id) {

    if (id == null) {
      return false;
    }

    return transportLostIds.contains(id);
  }

  public Set<McpSessId> SessRegGetSentinelMcpSessIds() {
    return Set.copyOf(sentinelIds);
  }

  // =========================================================
  // OBSERVABILITY
  // =========================================================

  public List<McpSessMetadata> SessRegGetAllMetadata() {

    List<Map.Entry<McpSessId, McpSessState>> snapshot = new ArrayList<>(states.entrySet());

    List<McpSessMetadata> list = new ArrayList<>(snapshot.size());

    for (Map.Entry<McpSessId, McpSessState> e : snapshot) {

      McpSessId id = e.getKey();
      McpSessState state = e.getValue();

      Instant ts = createdAt.getOrDefault(id, Instant.EPOCH);

      list.add(new McpSessMetadata(id, null, state, ts, runtimeMetas.get(id)));
    }

    list.sort(Comparator.comparing(McpSessMetadata::createdAt));

    return List.copyOf(list);
  }

  public McpSessMetadata SessRegGetMetadata(McpSessId id) {

    if (id == null) {
      return null;
    }

    McpSessState state = states.get(id);

    if (state == null) {
      return null;
    }

    Instant ts = createdAt.getOrDefault(id, Instant.EPOCH);

    return new McpSessMetadata(id, null, state, ts, runtimeMetas.get(id));
  }

  public void SessRegSetRuntimeMeta(McpSessId id, McpSessRTMeta runtimeMeta) {

    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(runtimeMeta, "runtimeMeta");

    runtimeMetas.put(id, runtimeMeta);
  }

  public McpSessRTMeta SessRegGetRuntimeMeta(McpSessId id) {

    if (id == null) {
      return null;
    }

    return runtimeMetas.get(id);
  }

  public void SessRegTouchRuntimeMeta(McpSessId id) {

    if (id == null) {
      return;
    }

    runtimeMetas.computeIfPresent(id, (k, current) -> current.McpSessRTMetaTouch(Instant.now()));
  }

  // =========================================================
  // STATUS
  // =========================================================

  public int SessRegGetActiveSessCount() {
    return states.size();
  }

  // =========================================================
  // RESET
  // =========================================================

  public void SessRegClearAll() {

    states.clear();
    createdAt.clear();
    sentinelIds.clear();
    transportLostIds.clear();
    runtimeMetas.clear();
  }
}
