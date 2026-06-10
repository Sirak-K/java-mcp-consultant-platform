package mcp.server.foundation.logging;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;
import java.util.Objects;

/**
 * Shared size-based rotation and retention support for local structured log files.
 */
public final class RollingLogFileSupport {

  private final LogRotationPolicy policy;

  public RollingLogFileSupport(LogRotationPolicy policy) {
    this.policy = Objects.requireNonNull(policy, "policy");
  }

  public void RollingLogFileEnsureExists(Path path) {
    try {
      ensureFileExists(path);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to initialize log target", ex);
    }
  }

  public void RollingLogFileReset(Path path, String headerLine) {
    Objects.requireNonNull(path, "path");

    try {
      ensureFileExists(path);
      clearRotatedHistory(path);
      Files.writeString(path, "", StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
      writeHeaderIfConfigured(path, headerLine);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to reset structured log file", ex);
    }
  }

  public void RollingLogFileWriteContent(Path path, String content) {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(content, "content");

    try {
      ensureFileExists(path);
      Files.writeString(
          path,
          content,
          StandardCharsets.UTF_8,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to rewrite structured log file", ex);
    }
  }

  public void RollingLogFileRotateNow(Path path) {
    Objects.requireNonNull(path, "path");

    try {
      ensureFileExists(path);
      if (Files.size(path) == 0L) {
        return;
      }
      rotateCurrentFile(path);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to rotate structured log file", ex);
    }
  }

  public long RollingLogFileGetMaxFileSizeBytes() {
    return policy.maxFileSizeBytes();
  }

  public void RollingLogFileAppendLine(Path path, String line) {
    RollingLogFileAppendLine(path, line, "");
  }

  public void RollingLogFileAppendLine(Path path, String line, String headerLine) {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(line, "line");

    byte[] payload = (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);

    try {
      ensureFileExists(path);
      rotateIfNeeded(path, payload.length);
      writeHeaderIfConfigured(path, headerLine);
      Files.write(path, payload, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to append structured log line", ex);
    }
  }

  private void rotateIfNeeded(Path path, int incomingBytes) throws Exception {
    long currentSize = Files.size(path);
    if (currentSize == 0L || currentSize + incomingBytes <= policy.maxFileSizeBytes()) {
      return;
    }

    rotateCurrentFile(path);
  }

  private void rotateCurrentFile(Path path) throws Exception {

    deleteIfExists(rotatedPath(path, policy.maxHistoryFiles()));

    for (int index = policy.maxHistoryFiles() - 1; index >= 1; index--) {
      Path source = rotatedPath(path, index);
      if (Files.exists(source)) {
        Files.move(source, rotatedPath(path, index + 1), StandardCopyOption.REPLACE_EXISTING);
      }
    }

    Files.move(path, rotatedPath(path, 1), StandardCopyOption.REPLACE_EXISTING);
    ensureFileExists(path);
  }

  private static Path rotatedPath(Path path, int index) {
    return path.resolveSibling(path.getFileName() + "." + index);
  }

  private static void ensureFileExists(Path path) throws Exception {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    if (Files.notExists(path)) {
      Files.createFile(path);
    }
  }

  private static void writeHeaderIfConfigured(Path path, String headerLine) throws Exception {
    if (headerLine == null || headerLine.isBlank() || Files.size(path) > 0L) {
      return;
    }

    Files.writeString(
        path,
        headerLine + System.lineSeparator(),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.APPEND);
  }

  private static void deleteIfExists(Path path) throws Exception {
    if (Files.exists(path)) {
      Files.delete(path);
    }
  }

  private static void clearRotatedHistory(Path path) throws Exception {
    Path parent = path.getParent();
    if (parent == null || Files.notExists(parent)) {
      return;
    }

    String prefix = path.getFileName().toString() + ".";

    try (Stream<Path> children = Files.list(parent)) {
      for (Path child : children.toList()) {
        String fileName = child.getFileName().toString();
        if (fileName.startsWith(prefix)) {
          Files.deleteIfExists(child);
        }
      }
    }
  }
}
