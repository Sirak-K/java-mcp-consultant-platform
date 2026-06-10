package mcp.server.foundation.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class RPCReqsPayl {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String jsonrpc;
  private final JsonNode id;
  private final String method;
  private final JsonNode params;

  private RPCReqsPayl(
      String jsonrpc,
      JsonNode id,
      String method,
      JsonNode params) {

    this.jsonrpc = jsonrpc;
    this.id = id;
    this.method = method;
    this.params = params;
  }

  public static RPCReqsPayl RPCReqPlParse(String json) throws Exception {

    if (json == null || json.isBlank()) {
      throw new IllegalArgumentException("Empty JSON-RPC payload");
    }

    JsonNode root = MAPPER.readTree(json);
    return RPCReqPlFromJson(root);
  }

  public static RPCReqsPayl RPCReqPlFromJson(JsonNode root) {

    if (root == null || root.isNull()) {
      throw new IllegalArgumentException("Empty JSON-RPC payload");
    }

    String jsonrpc = root.path("jsonrpc").asText(null);
    JsonNode id = root.get("id");
    String method = root.path("method").asText(null);
    JsonNode params = root.get("params");

    if (!"2.0".equals(jsonrpc)) {
      throw new IllegalArgumentException("Invalid jsonrpc version");
    }

    if (method == null || method.isBlank()) {
      throw new IllegalArgumentException("Missing method");
    }

    return new RPCReqsPayl(jsonrpc, id, method, params);
  }

  public String RPCReqPlGetJsonrpc() {
    return jsonrpc;
  }

  public JsonNode RPCReqPlGetId() {
    return id;
  }

  public String RPCReqPlGetMet() {
    return method;
  }

  public JsonNode RPCReqPlGetParams() {
    return params;
  }

  public boolean RPCReqPlIsNotification() {
    return id == null || id.isNull();
  }
}
