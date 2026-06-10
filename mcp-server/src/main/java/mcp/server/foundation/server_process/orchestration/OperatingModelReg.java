package mcp.server.foundation.server_process.orchestration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * OperatingModelReg
 *
 * <p>Small immutable registry for the current operating-model freeze.
 */
public final class OperatingModelReg {

  private final EnumMap<OperatingSurface, OperatingSurfaceContract> contracts;

  public OperatingModelReg(
      List<OperatingSurfaceContract> contracts) {

    Objects.requireNonNull(contracts, "contracts");

    EnumMap<OperatingSurface, OperatingSurfaceContract> mapped = new EnumMap<>(OperatingSurface.class);
    for (OperatingSurfaceContract contract : contracts) {
      Objects.requireNonNull(contract, "contract");
      OperatingSurface key = contract.OperatingSurfaceContractGetSurface();
      if (mapped.putIfAbsent(key, contract) != null) {
        throw new IllegalArgumentException("Duplicate operating surface contract: " + key);
      }
    }

    for (OperatingSurface surface : OperatingSurface.values()) {
      if (!mapped.containsKey(surface)) {
        throw new IllegalArgumentException("Missing operating surface contract: " + surface);
      }
    }

    this.contracts = mapped;
  }

  public OperatingSurfaceContract OperatingModelRegGet(
      OperatingSurface operatingSurface) {
    Objects.requireNonNull(operatingSurface, "operatingSurface");
    return contracts.get(operatingSurface);
  }

  public List<OperatingSurfaceContract> OperatingModelRegList() {
    return List.copyOf(contracts.values());
  }

  public Map<OperatingSurface, OperatingSurfaceContract> OperatingModelRegAsMap() {
    return Map.copyOf(contracts);
  }

  public String OperatingModelRegDescribe() {
    return contracts.values().stream()
        .map(OperatingSurfaceContract::OperatingSurfaceContractDescribe)
        .collect(Collectors.joining(", "));
  }
}
