package mcp.server.foundation.transport.http.shared;

import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.transport.TranspSessBindRegSupport;

import java.util.Objects;
import java.util.Set;
import java.util.Map;

/**
 * Shared registry for active HTTP transport sessions.
 */
public final class HTTPSessBindingReg {

  private final TranspSessBindRegSupport<String, HTTPTranspSess> bindings =
      new TranspSessBindRegSupport<>(
          HTTPTranspSess::HtsGetTranspConnId,
          HTTPTranspSess::HtsGetMcpSessId);

  public void register(HTTPTranspSess session) {
    bindings.register(Objects.requireNonNull(session, "session"));
  }

  public HTTPTranspSess getByTranspConnId(String transportConnectionId) {
    return bindings.getByConnId(Objects.requireNonNull(transportConnectionId, "transportConnectionId"));
  }

  public HTTPTranspSess getByMcpSessId(McpSessId sessionId) {

    Objects.requireNonNull(sessionId, "sessionId");

    return bindings.getByLogicalSessId(sessionId.asString());
  }

  public HTTPTranspSess getByMcpSessId(String sessionId) {
    return getByMcpSessId(McpSessId.fromString(sessionId));
  }

  public HTTPTranspSess unregisterByTranspConnId(String transportConnectionId) {
    return bindings.unregisterByConnId(Objects.requireNonNull(transportConnectionId, "transportConnectionId"));
  }

  public HTTPTranspSess unregisterByMcpSessId(McpSessId sessionId) {

    Objects.requireNonNull(sessionId, "sessionId");

    return bindings.unregisterByLogicalSessId(sessionId.asString());
  }

  public boolean hasTranspConn(String transportConnectionId) {
    return bindings.hasConn(Objects.requireNonNull(transportConnectionId, "transportConnectionId"));
  }

  public boolean hasMcpSess(McpSessId sessionId) {
    Objects.requireNonNull(sessionId, "sessionId");
    return bindings.hasLogicalSess(sessionId.asString());
  }

  public int getActiveSessCount() {
    return bindings.getActiveBindingCount();
  }

  public Map<String, String> getConnToSessSnapshot() {
    return bindings.getConnToLogicalSessSnapshot();
  }

  public Set<String> getTranspConnIdsSnapshot() {
    return bindings.getConnIdsSnapshot();
  }

  public void clearAll() {
    bindings.clearAll();
  }
}
