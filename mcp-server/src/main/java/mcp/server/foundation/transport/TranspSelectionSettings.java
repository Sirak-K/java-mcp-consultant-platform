package mcp.server.foundation.transport;

import java.util.Objects;

public record TranspSelectionSettings(
    TranspSelection activeTransp,
    boolean propertySelectionMode,
    String propertySelectionSourceProperty) {

  public TranspSelectionSettings {
    activeTransp = Objects.requireNonNull(activeTransp, "activeTransp");
    propertySelectionSourceProperty = propertySelectionSourceProperty == null
        ? null
        : propertySelectionSourceProperty.trim();

    if (!propertySelectionMode) {
      propertySelectionSourceProperty = null;
    }
  }

  public String TranspSelectionSettingsActiveTranspName() {
    return activeTransp.TranspSelectionPropertyValue();
  }

  public boolean TranspSelectionSettingsIsNetworkTransp() {
    return activeTransp.TranspSelectionIsNetworkTransp();
  }
}
