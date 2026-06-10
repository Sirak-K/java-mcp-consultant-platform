package mcp.server.foundation.transport;

import mcp.server.foundation.rpc.RPCMappedExcep;
import mcp.server.foundation.rpc.RPCReqsPayl;
import mcp.server.foundation.rpc.RPCRespPayl;
import mcp.server.foundation.rpc.RPCRouter;

import java.util.Objects;

/**
 * Canonical transport-layer RPC error responses shared across transports.
 */
public final class TranspRPCRespFactory {

  public static final String MESSAGE_TRANSPORT_OVERLOADED = "Transp overloaded, retry later";
  public static final String MESSAGE_TRANSPORT_TIMEOUT = "Transp response timed out";

  private TranspRPCRespFactory() {
  }

  public static RPCRespPayl parseErr(RuntimeException exception) {
    return RPCRouter.RPCRouterMapParseRTExcepToResp(
        Objects.requireNonNull(exception, "exception"));
  }

  public static RPCRespPayl overload(RPCReqsPayl request) {
    return RPCRouter.RPCRouterMapMappedExcepToResponse(
        RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(MESSAGE_TRANSPORT_OVERLOADED),
        Objects.requireNonNull(request, "request"));
  }

  public static RPCRespPayl timeout(RPCReqsPayl request) {
    return RPCRouter.RPCRouterMapMappedExcepToResponse(
        RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(MESSAGE_TRANSPORT_TIMEOUT),
        Objects.requireNonNull(request, "request"));
  }
}
