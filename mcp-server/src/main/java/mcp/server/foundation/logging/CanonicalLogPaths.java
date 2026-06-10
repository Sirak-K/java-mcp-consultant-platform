package mcp.server.foundation.logging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Resolves canonical log destinations relative to the project root when possible.
 */
public final class CanonicalLogPaths {

  private static final String LOGS_DIR_NAME = "logs";
  private static final String MODULE_DIR_NAME = "mcp-server";

  private final Path projectRoot;

  public CanonicalLogPaths() {
    this(Path.of("").toAbsolutePath().normalize());
  }

  public CanonicalLogPaths(Path workingDirectory) {
    this.projectRoot = resolveProjectRoot(Objects.requireNonNull(workingDirectory, "workingDirectory"));
  }

  public Path CanonicalLogPathsGetProjectRoot() {
    return projectRoot;
  }

  public Path CanonicalLogPathsGetLogsDirectory() {
    return projectRoot.resolve(LOGS_DIR_NAME);
  }

  public Path CanonicalLogPathsResolve(String configuredPath, String fallbackFileName) {

    Objects.requireNonNull(fallbackFileName, "fallbackFileName");

    if (configuredPath == null || configuredPath.isBlank()) {
      return CanonicalLogPathsGetLogsDirectory().resolve(fallbackFileName).normalize();
    }

    Path configured = Path.of(configuredPath.trim());
    if (configured.isAbsolute()) {
      return configured.normalize();
    }

    if (configured.getParent() == null) {
      return CanonicalLogPathsGetLogsDirectory().resolve(configured.getFileName()).normalize();
    }

    if (LOGS_DIR_NAME.equalsIgnoreCase(configured.getName(0).toString())) {
      return projectRoot.resolve(configured).normalize();
    }

    return CanonicalLogPathsGetLogsDirectory().resolve(configured.getFileName()).normalize();
  }

  private static Path resolveProjectRoot(Path workingDirectory) {

    Path normalized = workingDirectory.toAbsolutePath().normalize();
    Path current = normalized;

    while (current != null) {
      if (Files.isDirectory(current.resolve(MODULE_DIR_NAME))) {
        return current.normalize();
      }

      Path dirName = current.getFileName();
      if (dirName != null
          && MODULE_DIR_NAME.equalsIgnoreCase(dirName.toString())
          && current.getParent() != null) {
        return current.getParent().normalize();
      }

      current = current.getParent();
    }

    return normalized;
  }
}
