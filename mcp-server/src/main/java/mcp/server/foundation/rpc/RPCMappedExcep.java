package mcp.server.foundation.rpc;

import java.io.Serial;

public final class RPCMappedExcep extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Marked transient to avoid javac serial warning under -Werror.
   * Runtime usage does not depend on Java serialization.
   */
  private final transient RPCErr rpcError;

  public RPCMappedExcep(RPCErr rpcError) {
    super(rpcError.RPCErrGetMessage());
    this.rpcError = rpcError;
  }

  public RPCErr RPCMappedExcepGetRPCErr() {
    return rpcError;
  }

  public static RPCMappedExcep RPCMappedExcepBusinessRuleViolation(String message) {
    return new RPCMappedExcep(
        RPCErr.RPCErrBusinessRuleViolation(message));
  }

  public static RPCMappedExcep RPCMappedExcepInvalidParams(String message) {
    return new RPCMappedExcep(
        RPCErr.RPCErrInvalidParams(message));
  }
}
