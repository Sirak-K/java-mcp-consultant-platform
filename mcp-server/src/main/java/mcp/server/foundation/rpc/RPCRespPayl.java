package mcp.server.foundation.rpc;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RPCRespPayl {

  private final JsonNode id;
  private final Object result;
  private final RPCErr error;

  private RPCRespPayl(
      JsonNode id,
      Object result,
      RPCErr error) {

    this.id = id;
    this.result = result;
    this.error = error;
  }

  // =========================================================
  // FACTORIES
  // =========================================================

  public static RPCRespPayl RpcRespPlResult(
      JsonNode id,
      Object result) {

    Objects.requireNonNull(id, "id");
    return new RPCRespPayl(id, result, null);
  }

  public static RPCRespPayl RpcRespPlError(
      JsonNode id,
      RPCErr error) {

    Objects.requireNonNull(error, "error");
    return new RPCRespPayl(id, null, error);
  }

  // =========================================================
  // GETTERS
  // =========================================================

  public JsonNode RPCRespPlGetId() {
    return id;
  }

  public Object RPCRespPlGetResult() {
    return result;
  }

  public RPCErr RPCRespPlGetErr() {
    return error;
  }

  public boolean RPCRespPlIsSuccess() {
    return error == null;
  }

  public boolean RPCRespPlHasErr() {
    return error != null;
  }

  // =========================================================
  // SERIALIZATION SUPPORT
  // =========================================================

  public Map<String, Object> RPCRespPlToMap() {

    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put("jsonrpc", "2.0");
    map.put("id", id);

    if (error == null) {
      map.put("result", result);
    } else {
      map.put("error", error.RPCErrToJsonRPCFormat());
    }

    return map;
  }
}