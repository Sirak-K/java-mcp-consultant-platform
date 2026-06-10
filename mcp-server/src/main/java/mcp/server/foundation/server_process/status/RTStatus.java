package mcp.server.foundation.server_process.status;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RTStatus
 *
 * Runtime status for MCP server process.
 *
 * Hårda invariants:
 * - Deterministiska transitions.
 * - Fail-fast vid otillåten transition.
 */
public final class RTStatus {

  public enum ServerState {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    FAILED
  }

  private final AtomicReference<ServerState> state = new AtomicReference<>(ServerState.STOPPED);

  public RTStatus() {
    // default: STOPPED
  }

  public ServerState RTStatusGet() {
    return state.get();
  }

  public void RTStatusMarkStarting() {
    RTStatusTransition(ServerState.STARTING, ServerState.STOPPED, ServerState.STARTING);
  }

  public void RTStatusMarkRunning() {
    RTStatusTransition(ServerState.RUNNING, ServerState.STARTING, ServerState.RUNNING);
  }

  public void RTStatusMarkStopping() {

    ServerState cur = state.get();

    if (cur == ServerState.FAILED) {
      // allow FAILED -> STOPPING (best-effort stop)
      state.set(ServerState.STOPPING);
      return;
    }

    RTStatusTransition(ServerState.STOPPING, ServerState.RUNNING, ServerState.STOPPING);
  }

  public void RTStatusMarkStopped() {

    ServerState cur = state.get();

    if (cur == ServerState.FAILED) {
      // allow FAILED -> STOPPED (best-effort terminal)
      state.set(ServerState.STOPPED);
      return;
    }

    RTStatusTransition(ServerState.STOPPED, ServerState.STOPPING, ServerState.STOPPED);
  }

  public void RTStatusMarkFailed() {
    state.set(ServerState.FAILED);
  }

  private void RTStatusTransition(
      ServerState target,
      ServerState expectedFrom,
      ServerState expectedToSame) {

    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(expectedFrom, "expectedFrom");
    Objects.requireNonNull(expectedToSame, "expectedToSame");

    boolean ok = state.compareAndSet(expectedFrom, target);

    if (!ok) {

      // Idempotens: redan i target -> ok
      if (state.get() == expectedToSame) {
        return;
      }

      throw new IllegalStateException(
          "Illegal RTStatus transition. expectedFrom=" + expectedFrom
              + " target=" + target
              + " but was=" + state.get());
    }
  }
}