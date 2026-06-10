package mcp.server.foundation.transport;

import java.util.Arrays;
import java.util.Objects;

public enum TranspSelection {
  STREAMABLE_HTTP("streamable-http", true),
  STDIO("stdio", false),
  WEBSOCKET("websocket", true);

  private final String propertyValue;
  private final boolean networkTransp;

  TranspSelection(String propertyValue, boolean networkTransp) {
    this.propertyValue = Objects.requireNonNull(propertyValue, "propertyValue");
    this.networkTransp = networkTransp;
  }

  public String TranspSelectionPropertyValue() {
    return propertyValue;
  }

  public boolean TranspSelectionIsNetworkTransp() {
    return networkTransp;
  }

  public static TranspSelection TranspSelectionFromPropertyValue(String rawValue) {

    Objects.requireNonNull(rawValue, "rawValue");

    String normalized = rawValue.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("Transp selection must not be blank");
    }

    return Arrays.stream(values())
        .filter(value -> value.propertyValue.equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported active transport: " + rawValue));
  }
}
