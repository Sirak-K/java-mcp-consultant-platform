package mcp.server.foundation.control_plane;

import java.util.Map;
import java.util.Objects;

/**
 * Neutral control-plane status view used during the domain reset phase.
 */
public record PlatformControlPlaneStatusView(
    boolean enabled,
    Map<String, Object> tracks) {

  public PlatformControlPlaneStatusView {
    tracks = Map.copyOf(Objects.requireNonNull(tracks, "tracks"));
  }
}
