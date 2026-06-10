package mcp.server.foundation.server_process.orchestration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Persists small lifecycle facts that must survive process restarts.
 */
public final class ServerLifecyStateStore {

  private static final String KEY_LAST_STOPPED_TIMESTAMP_SECONDS = "lastStoppedTimestampSeconds";

  private final Path stateFile;

  public ServerLifecyStateStore() {
    this.stateFile = null;
  }

  public ServerLifecyStateStore(Path stateFile) {
    this.stateFile = Objects.requireNonNull(stateFile, "stateFile").toAbsolutePath().normalize();
  }

  public static ServerLifecyStateStore ServerLifeStateNoOp() {
    return new ServerLifecyStateStore();
  }

  public synchronized Long ServerLifeStateReadLastStoppedTimestampSeconds() {

    if (stateFile == null || !Files.isRegularFile(stateFile)) {
      return null;
    }

    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(stateFile)) {
      properties.load(inputStream);
    } catch (IOException ignored) {
      return null;
    }

    String rawValue = properties.getProperty(KEY_LAST_STOPPED_TIMESTAMP_SECONDS);
    if (rawValue == null || rawValue.isBlank()) {
      return null;
    }

    try {
      long parsed = Long.parseLong(rawValue.trim());
      return parsed > 0L ? parsed : null;
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  public synchronized void ServerLifeStateWriteLastStoppedTimestampSeconds(long timestampSeconds) {

    if (stateFile == null || timestampSeconds <= 0L) {
      return;
    }

    try {
      Path parent = stateFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      Properties properties = new Properties();
      properties.setProperty(KEY_LAST_STOPPED_TIMESTAMP_SECONDS, Long.toString(timestampSeconds));

      try (OutputStream outputStream = Files.newOutputStream(stateFile)) {
        properties.store(outputStream, "MCP server lifecycle state");
      }
    } catch (IOException ignored) {
      // Lifecycle persistence is best-effort and must never break shutdown/startup.
    }
  }
}
