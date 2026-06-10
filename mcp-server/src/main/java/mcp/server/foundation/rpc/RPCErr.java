package mcp.server.foundation.rpc;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public final class RPCErr implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final int code;
  private final String message;

  private RPCErr(int code, String message) {
    this.code = code;
    this.message = Objects.requireNonNull(message, "message");
  }

  // =========================================================
  // JSON-RPC Standard Codes
  // =========================================================

  public static final int CODE_PARSE_ERROR = -32700;
  public static final int CODE_INVALID_REQUEST = -32600;
  public static final int CODE_METHOD_NOT_FOUND = -32601;
  public static final int CODE_INVALID_PARAMS = -32602;
  public static final int CODE_INTERNAL_ERROR = -32603;

  // Server custom range (-32000 to -32099)
  public static final int CODE_BUSINESS_RULE_VIOLATION = -32000;

  // =========================================================
  // FACTORIES
  // =========================================================

  public static RPCErr RPCErrParseErr(String message) {
    return new RPCErr(CODE_PARSE_ERROR, message);
  }

  public static RPCErr RPCErrInvalidReqs(String message) {
    return new RPCErr(CODE_INVALID_REQUEST, message);
  }

  public static RPCErr RPCErrMetNotFound(String message) {
    return new RPCErr(CODE_METHOD_NOT_FOUND, message);
  }

  public static RPCErr RPCErrInvalidParams(String message) {
    return new RPCErr(CODE_INVALID_PARAMS, message);
  }

  public static RPCErr RPCErrInternalErr(String message) {
    return new RPCErr(CODE_INTERNAL_ERROR, message);
  }

  public static RPCErr RPCErrBusinessRuleViolation(String message) {
    return new RPCErr(CODE_BUSINESS_RULE_VIOLATION, message);
  }

  // =========================================================
  // GETTERS
  // =========================================================

  public int RPCErrGetCode() {
    return code;
  }

  public String RPCErrGetMessage() {
    return message;
  }

  // =========================================================
  // Canonical JSON Representation
  // =========================================================

  public Map<String, Object> RPCErrToJsonRPCFormat() {
    return Map.of(
        "code", code,
        "message", message);
  }
}