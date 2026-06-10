package mcp.server.foundation.support.architecture;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Canonical architecture boundary registry.
 */
public final class ArchitectureBoundaryRegistry {

  private static final Map<ArchitectureLayerRole, ArchitectureBoundary> BOUNDARIES =
      buildBoundaries();

  private ArchitectureBoundaryRegistry() {
  }

  public static ArchitectureBoundary ArchitectureBoundaryRegistryGet(ArchitectureLayerRole role) {
    return BOUNDARIES.get(Objects.requireNonNull(role, "role"));
  }

  public static List<ArchitectureBoundary> ArchitectureBoundaryRegistryAll() {
    return List.copyOf(BOUNDARIES.values());
  }

  public static boolean ArchitectureBoundaryRegistryAllows(
      ArchitectureLayerRole role,
      String dependencyPackageName) {

    ArchitectureBoundary boundary = ArchitectureBoundaryRegistryGet(role);
    return boundary != null && boundary.ArchitectureBoundaryAllowsDependency(dependencyPackageName);
  }

  public static String ArchitectureBoundaryRegistryDescribe() {
    return BOUNDARIES.values().stream()
        .map(ArchitectureBoundary::ArchitectureBoundaryDescribe)
        .collect(Collectors.joining(" | "));
  }

  private static Map<ArchitectureLayerRole, ArchitectureBoundary> buildBoundaries() {
    EnumMap<ArchitectureLayerRole, ArchitectureBoundary> boundaries =
        new EnumMap<>(ArchitectureLayerRole.class);

    boundaries.put(
        ArchitectureLayerRole.DOMAIN_SERVICE_CORE,
        new ArchitectureBoundary(
            ArchitectureLayerRole.DOMAIN_SERVICE_CORE,
            "mcp.server.domain.service",
            List.of(
                "mcp.server.domain.model",
                "mcp.server.domain.repository",
                "mcp.server.domain.data",
                "mcp.server.foundation.support.architecture"),
            "Canonical workflow core. No direct ownership of transport, Spring wiring, or runtime orchestration."));

    boundaries.put(
        ArchitectureLayerRole.FOUNDATION_SERVER_PROCESS,
        new ArchitectureBoundary(
            ArchitectureLayerRole.FOUNDATION_SERVER_PROCESS,
            "mcp.server.foundation.server_process",
            List.of(
                "mcp.server.domain.service",
                "mcp.server.foundation.support.architecture",
                "mcp.server.foundation.transport",
                "mcp.server.foundation.security",
                "mcp.server.foundation.observability"),
            "Server lifecycle, runtime orchestration, request processing, and session lifecycle coordination."));

    boundaries.put(
        ArchitectureLayerRole.FOUNDATION_TOOL_INTERFACE,
        new ArchitectureBoundary(
            ArchitectureLayerRole.FOUNDATION_TOOL_INTERFACE,
            "mcp.server.foundation.tool_interface",
            List.of(
                "mcp.server.domain.service",
                "mcp.server.foundation.support.architecture",
                "mcp.server.foundation.transport",
                "mcp.server.foundation.security",
                "mcp.server.foundation.observability"),
            "Tool contracts, tool execution, and tool-facing orchestration only."));

    boundaries.put(
        ArchitectureLayerRole.FOUNDATION_SPRING_INTEGRATION,
        new ArchitectureBoundary(
            ArchitectureLayerRole.FOUNDATION_SPRING_INTEGRATION,
            "mcp.server.foundation.spring_integration",
            List.of(
                "mcp.server.domain.service",
                "mcp.server.foundation.support.architecture",
                "mcp.server.foundation.tool_interface",
                "mcp.server.foundation.server_process",
                "mcp.server.foundation.transport",
                "mcp.server.foundation.security",
                "mcp.server.foundation.observability"),
            "Wiring only: bean registration, runtime composition, and configuration bridging."));

    return Map.copyOf(boundaries);
  }
}
