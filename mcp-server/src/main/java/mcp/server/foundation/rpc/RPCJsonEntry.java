package mcp.server.foundation.rpc;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public class RPCJsonEntry {

  private final RPCJsonSeria serializer;

  public RPCJsonEntry(RPCJsonSeria serializer) {
    this.serializer = Objects.requireNonNull(serializer, "serializer");
  }

  // =========================================================
  // Canonical API (preferred)
  // =========================================================

  public RPCReqsPayl RPCJsonEntryParseReqs(String rawJson) {

    JsonNode root = serializer.JsonRPCSerReadTree(rawJson);

    return RPCReqsPayl.RPCReqPlFromJson(root);
  }

  public String RPCJsonEntryToRespJson(RPCRespPayl response) {

    Objects.requireNonNull(response, "response");

    return serializer.JsonRPCSerSerialize(response.RPCRespPlToMap());
  }

}
