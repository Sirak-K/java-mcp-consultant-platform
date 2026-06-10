package mcp.server.foundation.transport.http.shared;

import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMeta;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.transport.TranspSess;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared HTTP session wrapper around the transport-neutral session core.
 */
public final class HTTPTranspSess {

  private final TranspSess transportSession;
  private final String endpointPath;
  private final Instant createdAt;
  private final HTTPReqsMetadata openingRequestMetadata;
  private final AtomicReference<HTTPReqsMetadata> lastRequestMetadata;

  public HTTPTranspSess(
      String transportName,
      String transportConnectionId,
      McpSessId mcpSessId,
      String endpointPath,
      HTTPReqsMetadata openingRequestMetadata) {

    this(
        transportName,
        transportConnectionId,
        mcpSessId,
        endpointPath,
        openingRequestMetadata,
        OperatingSurface.MCP_DIRECT);
  }

  public HTTPTranspSess(
      String transportName,
      String transportConnectionId,
      McpSessId mcpSessId,
      String endpointPath,
      HTTPReqsMetadata openingRequestMetadata,
      OperatingSurface operatingSurface) {

    this(
        transportName,
        transportConnectionId,
        mcpSessId,
        endpointPath,
        openingRequestMetadata,
        operatingSurface,
        null);
  }

  public HTTPTranspSess(
      String transportName,
      String transportConnectionId,
      McpSessId mcpSessId,
      String endpointPath,
      HTTPReqsMetadata openingRequestMetadata,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding) {

    this(
        transportName,
        transportConnectionId,
        mcpSessId,
        endpointPath,
        openingRequestMetadata,
        operatingSurface,
        requestAuthBinding,
        new McpSessRTMetaFactory());
  }

  public HTTPTranspSess(
      String transportName,
      String transportConnectionId,
      McpSessId mcpSessId,
      String endpointPath,
      HTTPReqsMetadata openingRequestMetadata,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding,
      McpSessRTMetaFactory runtimeMetaFactory) {

    this.transportSession = new TranspSess(
        transportName,
        transportConnectionId,
        Objects.requireNonNull(mcpSessId, "mcpSessId"),
        Objects.requireNonNull(operatingSurface, "operatingSurface"),
        requestAuthBinding,
        Objects.requireNonNull(runtimeMetaFactory, "runtimeMetaFactory"));

    this.endpointPath = HTTPTranspCfg.normalizePath(endpointPath);
    this.createdAt = Instant.now();
    this.openingRequestMetadata = Objects.requireNonNull(openingRequestMetadata, "openingRequestMetadata");
    this.lastRequestMetadata = new AtomicReference<>(openingRequestMetadata);
  }

  public TranspSess HtsGetTranspSess() {
    return transportSession;
  }

  public String HtsGetTranspName() {
    return transportSession.TranspSessGetTranspName();
  }

  public String HtsGetTranspConnId() {
    return transportSession.TranspSessGetTranspConnId();
  }

  public McpSessId HtsGetMcpSessIdObject() {
    return transportSession.TranspSessGetMcpSessIdObject();
  }

  public String HtsGetMcpSessId() {
    return transportSession.TranspSessGetMcpSessId();
  }

  public OperatingSurface HtsGetOperatingSurface() {
    return transportSession.TranspSessGetOperatingSurface();
  }

  public String HtsGetOperatingSurfaceId() {
    return transportSession.TranspSessGetOperatingSurfaceId();
  }

  public ReqsAuthBinding HtsGetReqsAuthBinding() {
    return transportSession.TranspSessGetReqsAuthBinding();
  }

  public void HtsSetReqsAuthBinding(ReqsAuthBinding requestAuthBinding) {
    transportSession.TranspSessSetReqsAuthBinding(requestAuthBinding);
  }

  public McpSessRTMeta HtsGetRuntimeMeta() {
    return transportSession.TranspSessGetRuntimeMeta();
  }

  public void HtsSetRuntimeMeta(McpSessRTMeta runtimeMeta) {
    transportSession.TranspSessSetRuntimeMeta(runtimeMeta);
  }

  public String HtsGetEndpointPath() {
    return endpointPath;
  }

  public Instant HtsGetCreatedAt() {
    return createdAt;
  }

  public HTTPReqsMetadata HtsGetOpeningReqsMetadata() {
    return openingRequestMetadata;
  }

  public HTTPReqsMetadata HtsGetLastReqsMetadata() {
    return lastRequestMetadata.get();
  }

  public void HtsUpdLastReqsMetadata(HTTPReqsMetadata requestMetadata) {
    lastRequestMetadata.set(Objects.requireNonNull(requestMetadata, "requestMetadata"));
  }
}
