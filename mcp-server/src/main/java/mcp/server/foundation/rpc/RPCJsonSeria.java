package mcp.server.foundation.rpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.util.Objects;

public final class RPCJsonSeria {

  private final ObjectMapper mapper;

  public RPCJsonSeria() {

    this.mapper = new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  // =========================================================
  // READ
  // =========================================================

  public JsonNode JsonRPCSerReadTree(String rawJson) {

    Objects.requireNonNull(rawJson, "rawJson");

    try {
      return mapper.readTree(rawJson);
    } catch (Exception ex) {
      throw new RPCMappedExcep(
          RPCErr.RPCErrParseErr(ex.getMessage()));
    }
  }

  public Map<String, Object> RPCJsonSerNodeToMap(JsonNode node) {

    Objects.requireNonNull(node, "node");

    try {
      return mapper.convertValue(node, new TypeReference<Map<String, Object>>() {
      });
    } catch (IllegalArgumentException ex) {
      throw new RPCMappedExcep(
          RPCErr.RPCErrInvalidParams(ex.getMessage()));
    }
  }

  // =========================================================
  // SERIALIZE
  // =========================================================

  public String JsonRPCSerSerialize(Map<String, Object> payload) {

    Objects.requireNonNull(payload, "payload");

    try {
      return mapper.writeValueAsString(payload);
    } catch (Exception ex) {
      throw new RPCMappedExcep(
          RPCErr.RPCErrInternalErr(ex.getMessage()));
    }
  }
}
