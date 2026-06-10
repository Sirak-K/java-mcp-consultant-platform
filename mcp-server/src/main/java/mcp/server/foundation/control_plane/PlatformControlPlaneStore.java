package mcp.server.foundation.control_plane;

import mcp.server.foundation.observability.context.ObservCtx;

import java.util.Map;
import java.util.Objects;

/**
 * Neutral control-plane store scaffold kept while the official control-plane model is rebuilt.
 */
public final class PlatformControlPlaneStore {

  private static final PlatformControlPlaneStore NO_OP =
      new PlatformControlPlaneStore(new PlatformControlPlaneStatusView(false, Map.of()));

  private final PlatformControlPlaneStatusView statusView;

  public PlatformControlPlaneStore(PlatformControlPlaneStatusView statusView) {
    this.statusView = Objects.requireNonNull(statusView, "statusView");
  }

  public static PlatformControlPlaneStore PlatformControlPlaneStoreNoOp() {
    return NO_OP;
  }

  public PlatformControlPlaneStatusView PlatformControlPlaneStoreGetStatusView() {
    return statusView;
  }

  public void PlatformControlPlaneStoreRecordToolOutcome(
      ObservCtx context,
      String toolName,
      String outcome,
      String errorType,
      Long durationMs) {

    // Intentional no-op during the domain reset phase.
  }
}
