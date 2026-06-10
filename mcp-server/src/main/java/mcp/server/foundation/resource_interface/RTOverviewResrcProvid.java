package mcp.server.foundation.resource_interface;

import mcp.server.foundation.control_plane.PlatformControlPlaneStore;
import mcp.server.foundation.observability.runtime.RTVisibilityService;

import java.util.Map;
import java.util.Objects;

public final class RTOverviewResrcProvid implements ResrcProvid {

  private final String resourceName;
  private final RTVisibilityService runtimeVisibilityService;
  private final PlatformControlPlaneStore controlPlaneStore;
  private final Map<String, Object> productIdentity;
  private final Map<String, Object> runtimeContract;

  public RTOverviewResrcProvid(
      String resourceName,
      RTVisibilityService runtimeVisibilityService,
      PlatformControlPlaneStore controlPlaneStore,
      Map<String, Object> productIdentity,
      Map<String, Object> runtimeContract) {
    this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
    this.runtimeVisibilityService = Objects.requireNonNull(runtimeVisibilityService, "runtimeVisibilityService");
    this.controlPlaneStore = Objects.requireNonNull(controlPlaneStore, "controlPlaneStore");
    this.productIdentity = Map.copyOf(Objects.requireNonNull(productIdentity, "productIdentity"));
    this.runtimeContract = Map.copyOf(Objects.requireNonNull(runtimeContract, "runtimeContract"));
  }

  @Override
  public Map<String, Object> ResourceProvRead() {
    return Map.of(
        "resource", resourceName,
        "productIdentity", productIdentity,
        "runtimeContract", runtimeContract,
        "runtimeStatus", runtimeVisibilityService.RTVisibilitySvcGetRTStatus(),
        "metrics", runtimeVisibilityService.RTVisibilitySvcGetMetrics(),
        "operationalChecks", runtimeVisibilityService.RTVisibilitySvcGetOperChecks(),
        "controlPlane", controlPlaneStore.PlatformControlPlaneStoreGetStatusView());
  }
}
