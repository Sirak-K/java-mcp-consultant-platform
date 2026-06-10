package mcp.server.foundation.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TranspSelectionSupport {

  private TranspSelectionSupport() {
  }

  public static TranspSelectionSettings TranspSelectionResolve(
      String activeTranspRaw,
      boolean websocketEnabled,
      boolean stdioEnabled,
      boolean streamableHttpEnabled) {

    String normalizedActiveTransp = normalize(activeTranspRaw);
    if (!normalizedActiveTransp.isBlank()) {
      return new TranspSelectionSettings(
          TranspSelection.TranspSelectionFromPropertyValue(normalizedActiveTransp),
          false,
          null);
    }

    List<PropertySelection> enabledPropertySelections = new ArrayList<>();
    maybeAddEnabled(enabledPropertySelections, websocketEnabled, TranspSelection.WEBSOCKET, "mcp.transport.websocket.enabled");
    maybeAddEnabled(enabledPropertySelections, stdioEnabled, TranspSelection.STDIO, "mcp.transport.stdio.enabled");
    maybeAddEnabled(
        enabledPropertySelections,
        streamableHttpEnabled,
        TranspSelection.STREAMABLE_HTTP,
        "mcp.transport.streamable-http.enabled");

    if (enabledPropertySelections.size() > 1) {
      throw new IllegalStateException(
          "Current runtime supports exactly one active transport adapter. "
              + "Set mcp.transport.active or enable only one transport boolean property.");
    }

    if (enabledPropertySelections.size() == 1) {
      PropertySelection selection = enabledPropertySelections.get(0);
      return new TranspSelectionSettings(
          selection.transportSelection(),
          true,
          selection.sourceProperty());
    }

    return new TranspSelectionSettings(TranspSelection.STREAMABLE_HTTP, false, null);
  }

  public static boolean TranspSelectionHasExplicitActiveTransp(String activeTranspRaw) {
    return !normalize(activeTranspRaw).isBlank();
  }

  private static void maybeAddEnabled(
      List<PropertySelection> enabledSelections,
      boolean enabled,
      TranspSelection transportSelection,
      String sourceProperty) {

    Objects.requireNonNull(enabledSelections, "enabledSelections");
    Objects.requireNonNull(transportSelection, "transportSelection");
    Objects.requireNonNull(sourceProperty, "sourceProperty");

    if (!enabled) {
      return;
    }

    enabledSelections.add(new PropertySelection(transportSelection, sourceProperty));
  }

  private static String normalize(String rawValue) {
    return rawValue == null ? "" : rawValue.trim();
  }

  private record PropertySelection(
      TranspSelection transportSelection,
      String sourceProperty) {
  }
}
