package mcp.server.foundation.support.architecture;

import java.util.List;
import java.util.Objects;

/**
 * Immutable description of one architecture boundary.
 */
public record ArchitectureBoundary(
    ArchitectureLayerRole role,
    String packagePrefix,
    List<String> allowedDependencyPrefixes,
    String notes) {

  public ArchitectureBoundary {
    role = Objects.requireNonNull(role, "role");
    packagePrefix = requireText(packagePrefix, "packagePrefix");
    allowedDependencyPrefixes = List.copyOf(
        Objects.requireNonNull(allowedDependencyPrefixes, "allowedDependencyPrefixes"));
    notes = requireText(notes, "notes");
  }

  public boolean ArchitectureBoundaryContainsPackage(String packageName) {
    return hasPrefix(packageName, packagePrefix);
  }

  public boolean ArchitectureBoundaryAllowsDependency(String dependencyPackageName) {
    String normalized = requireText(dependencyPackageName, "dependencyPackageName");
    return allowedDependencyPrefixes.stream()
        .anyMatch(prefix -> hasPrefix(normalized, prefix));
  }

  public String ArchitectureBoundaryDescribe() {
    return role.name() + " -> " + packagePrefix + " :: " + notes;
  }

  private static boolean hasPrefix(String candidate, String prefix) {
    String normalizedCandidate = requireText(candidate, "candidate");
    String normalizedPrefix = requireText(prefix, "prefix");
    return normalizedCandidate.equals(normalizedPrefix)
        || normalizedCandidate.startsWith(normalizedPrefix + ".");
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName);
    String normalized = value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
