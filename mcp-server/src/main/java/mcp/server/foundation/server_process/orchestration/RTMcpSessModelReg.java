package mcp.server.foundation.server_process.orchestration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable registry for runtime/session model contracts.
 */
public final class RTMcpSessModelReg {

  private final EnumMap<RTMcpSessType, RTMcpSessLifecyContract> contracts;

  public RTMcpSessModelReg(
      List<RTMcpSessLifecyContract> contracts) {

    Objects.requireNonNull(contracts, "contracts");

    EnumMap<RTMcpSessType, RTMcpSessLifecyContract> mapped =
        new EnumMap<>(RTMcpSessType.class);

    for (RTMcpSessLifecyContract contract : contracts) {
      Objects.requireNonNull(contract, "contract");
      RTMcpSessType key = contract.RTMcpSessLifecyContractGetSessionType();
      if (mapped.putIfAbsent(key, contract) != null) {
        throw new IllegalArgumentException("Duplicate runtime session contract: " + key);
      }
    }

    for (RTMcpSessType type : RTMcpSessType.values()) {
      if (!mapped.containsKey(type)) {
        throw new IllegalArgumentException("Missing runtime session contract: " + type);
      }
    }

    this.contracts = mapped;
  }

  public RTMcpSessLifecyContract RTMcpSessModelRegGet(RTMcpSessType sessionType) {
    Objects.requireNonNull(sessionType, "sessionType");
    return contracts.get(sessionType);
  }

  public Map<RTMcpSessType, RTMcpSessLifecyContract> RTMcpSessModelRegAsMap() {
    return Map.copyOf(contracts);
  }

  public String RTMcpSessModelRegDescribe() {
    return contracts.values().stream()
        .map(RTMcpSessLifecyContract::RTMcpSessLifecyContractDescribe)
        .collect(Collectors.joining(", "));
  }

  public static RTMcpSessModelReg RTMcpSessModelRegDefault(
      long inactivityTtlSeconds) {
    return new RTMcpSessModelReg(
        RuntimeMcpSessionLifecycleCatalog.defaultContracts(inactivityTtlSeconds));
  }
}
