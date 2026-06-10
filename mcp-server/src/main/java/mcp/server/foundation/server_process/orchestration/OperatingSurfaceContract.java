package mcp.server.foundation.server_process.orchestration;

import java.util.Objects;

/**
 * OperatingSurfaceContract
 *
 * <p>Compact runtime contract for one first-class operating surface.
 */
public final class OperatingSurfaceContract {

  private final OperatingSurface operatingSurface;
  private final String description;
  private final boolean firstClass;
  private final boolean usesSharedCore;
  private final boolean mayDefineCanonicalBusinessLogic;
  private final boolean internalOnly;
  private final String summary;

  public OperatingSurfaceContract(
      OperatingSurface operatingSurface,
      String description,
      boolean firstClass,
      boolean usesSharedCore,
      boolean mayDefineCanonicalBusinessLogic,
      boolean internalOnly,
      String summary) {

    this.operatingSurface = Objects.requireNonNull(operatingSurface, "operatingSurface");
    this.description = requireText(description, "description");
    this.firstClass = firstClass;
    this.usesSharedCore = usesSharedCore;
    this.mayDefineCanonicalBusinessLogic = mayDefineCanonicalBusinessLogic;
    this.internalOnly = internalOnly;
    this.summary = requireText(summary, "summary");
  }

  public OperatingSurface OperatingSurfaceContractGetSurface() {
    return operatingSurface;
  }

  public String OperatingSurfaceContractGetDescription() {
    return description;
  }

  public boolean OperatingSurfaceContractIsFirstClass() {
    return firstClass;
  }

  public boolean OperatingSurfaceContractUsesSharedCore() {
    return usesSharedCore;
  }

  public boolean OperatingSurfaceContractMayDefineCanonicalBusinessLogic() {
    return mayDefineCanonicalBusinessLogic;
  }

  public boolean OperatingSurfaceContractIsInternalOnly() {
    return internalOnly;
  }

  public String OperatingSurfaceContractGetSummary() {
    return summary;
  }

  public String OperatingSurfaceContractDescribe() {
    return operatingSurface.OperatingSurfaceGetId()
        + "[firstClass=" + firstClass
        + ", sharedCore=" + usesSharedCore
        + ", canonicalLogic=" + mayDefineCanonicalBusinessLogic
        + ", internalOnly=" + internalOnly
        + "]";
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
