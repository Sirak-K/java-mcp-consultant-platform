package mcp.server.foundation.transport.http.streamable;

import mcp.server.foundation.rpc.RPCMetName;
import mcp.server.foundation.rpc.RPCReqsPayl;

final class StreamableHTTPFoundationMethods {

  private StreamableHTTPFoundationMethods() {
  }

  static boolean canUseSessionlessPath(
      RPCReqsPayl request,
      String mcpSessIdHeader) {

    if (StreamableHTTPProtocolGuard.hasSessionHeader(mcpSessIdHeader)) {
      return false;
    }

    String method = request == null ? null : request.RPCReqPlGetMet();
    return RPCMetName.RPCMetNameIsSessionlessFoundationMethod(method);
  }
}
