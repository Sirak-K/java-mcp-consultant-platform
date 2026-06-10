package mcp.server.foundation.security.request_binding;

import mcp.server.foundation.server_process.orchestration.OperatingSurface;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable registry for request lifecycle contracts.
 */
public final class ReqsLifecyReg {

  private final EnumMap<OperatingSurface, ReqsLifecyContract> contracts;

  public ReqsLifecyReg(
      List<ReqsLifecyContract> contracts) {

    Objects.requireNonNull(contracts, "contracts");

    EnumMap<OperatingSurface, ReqsLifecyContract> mapped = new EnumMap<>(OperatingSurface.class);
    for (ReqsLifecyContract contract : contracts) {
      Objects.requireNonNull(contract, "contract");
      OperatingSurface key = contract.ReqsLifecyContractGetSurface();
      if (mapped.putIfAbsent(key, contract) != null) {
        throw new IllegalArgumentException("Duplicate request lifecycle contract: " + key);
      }
    }

    for (OperatingSurface surface : OperatingSurface.values()) {
      if (!mapped.containsKey(surface)) {
        throw new IllegalArgumentException("Missing request lifecycle contract: " + surface);
      }
    }

    this.contracts = mapped;
  }

  public ReqsLifecyContract ReqsLifecyRegGet(OperatingSurface operatingSurface) {
    Objects.requireNonNull(operatingSurface, "operatingSurface");
    return contracts.get(operatingSurface);
  }

  public Map<OperatingSurface, ReqsLifecyContract> ReqsLifecyRegAsMap() {
    return Map.copyOf(contracts);
  }

  public String ReqsLifecyRegDescribe() {
    return contracts.values().stream()
        .map(ReqsLifecyContract::ReqsLifecyContractDescribe)
        .collect(Collectors.joining(", "));
  }
}
