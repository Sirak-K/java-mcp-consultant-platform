package mcp.server.foundation.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mcp.server.foundation.observability.transport.TranspSignalModel;
import mcp.server.foundation.rpc.RPCReqsPayl;
import mcp.server.foundation.rpc.RPCRespPayl;

import java.util.Objects;

/**
 * Shared transport contract helpers for response semantics, naming and
 * transport-neutral correlation extraction.
 */
public final class TranspContractSupport {

  public static final String TRANSPORT_UNBOUND = "UNBOUND";

  private static final ObjectMapper JSON = new ObjectMapper();

  private TranspContractSupport() {
  }

  public static RPCRespPayl TransContMapParseErr(RuntimeException exception) {
    return TranspRPCRespFactory.parseErr(
        Objects.requireNonNull(exception, "exception"));
  }

  public static RPCRespPayl TransContMapOverload(RPCReqsPayl request) {
    return TranspRPCRespFactory.overload(
        Objects.requireNonNull(request, "request"));
  }

  public static RPCRespPayl TransContMapTimeout(RPCReqsPayl request) {
    return TranspRPCRespFactory.timeout(
        Objects.requireNonNull(request, "request"));
  }

  public static String TransContMetricPrefix(String transportName) {

    if (transportName == null || transportName.isBlank()) {
      return "transport";
    }
    String family = TranspSignalModel.TransSigFamily(transportName);
    return switch (family) {
      case "stdio", "http", "ws" -> family;
      default -> "transport";
    };
  }

  public static String TransContNormalizeConnId(String transportConnectionId) {
    return transportConnectionId == null || transportConnectionId.isBlank()
        ? TRANSPORT_UNBOUND
        : transportConnectionId;
  }

  public static String TransContTryExtractCorrelaId(String payload) {

    if (payload == null || payload.isBlank()) {
      return null;
    }

    try {
      JsonNode root = JSON.readTree(payload);
      JsonNode idNode = root.get("id");

      if (idNode == null || idNode.isNull()) {
        return null;
      }

      if (idNode.isTextual() || idNode.isNumber() || idNode.isBoolean()) {
        return idNode.asText();
      }

      return idNode.toString();
    } catch (RuntimeException | java.io.IOException ignored) {
      return null;
    }
  }
}
