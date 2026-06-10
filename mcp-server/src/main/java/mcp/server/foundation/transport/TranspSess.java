package mcp.server.foundation.transport;

import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMeta;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.transport.websocket.WsConnId;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TranspSess
 *
 * Dual identity:
 *
 * 1) Physical identity → WsConnId
 * 2) Logical identity → McpSessId
 * 3) Operating surface → OperatingSurface
 *
 * Invariant:
 * - TranspSess är transport-scope.
 * - Logical lifecycle hanteras av McpSessReg.
 * - Active-flag gäller endast transport-livscykel.
 */
public final class TranspSess {

  public static final String TRANSPORT_WEBSOCKET = "websocket";
  public static final String REQUEST_SCOPED_CONN_MARKER = ":req:";

  private final String transportName;
  private final String transportConnectionId;
  private final McpSessId mcpSessId;
  private final OperatingSurface operatingSurface;
  private final McpSessRTMetaFactory runtimeMetaFactory;
  private volatile ReqsAuthBinding requestAuthBinding;
  private volatile McpSessRTMeta runtimeMeta;
  private final TranspMessageLedger messageLedger;

  private final AtomicBoolean active = new AtomicBoolean(true);

  public TranspSess(
      String transportName,
      String transportConnectionId,
      McpSessId mcpSessId) {

    this(transportName, transportConnectionId, mcpSessId, OperatingSurface.MCP_DIRECT);
  }

  public TranspSess(
      String transportName,
      String transportConnectionId,
      McpSessId mcpSessId,
      OperatingSurface operatingSurface) {

    this(
        transportName,
        transportConnectionId,
        mcpSessId,
        operatingSurface,
        null);
  }

  public TranspSess(
      String transportName,
      String transportConnectionId,
      McpSessId mcpSessId,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding) {

    this(
        transportName,
        transportConnectionId,
        mcpSessId,
        operatingSurface,
        requestAuthBinding,
        new McpSessRTMetaFactory());
  }

  public TranspSess(
      String transportName,
      String transportConnectionId,
      McpSessId mcpSessId,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding,
      McpSessRTMetaFactory runtimeMetaFactory) {

    this.transportName = Objects.requireNonNull(transportName, "transportName");
    this.transportConnectionId = Objects.requireNonNull(transportConnectionId, "transportConnectionId");
    this.mcpSessId = Objects.requireNonNull(mcpSessId, "mcpSessId");
    this.operatingSurface = Objects.requireNonNull(operatingSurface, "operatingSurface");
    this.runtimeMetaFactory = Objects.requireNonNull(runtimeMetaFactory, "runtimeMetaFactory");
    this.requestAuthBinding = requestAuthBinding;
    this.runtimeMeta = this.runtimeMetaFactory.McpSessRTMetaFactoryDefault(
        this.mcpSessId,
        this.operatingSurface,
        requestAuthBinding);
    this.messageLedger = new TranspMessageLedger();
  }

  public TranspSess(
      WsConnId wsConnId,
      McpSessId mcpSessId) {

    this(
        TRANSPORT_WEBSOCKET,
        Objects.requireNonNull(wsConnId, "wsConnId").asString(),
        mcpSessId,
        OperatingSurface.MCP_DIRECT);
  }

  public TranspSess(
      WsConnId wsConnId,
      McpSessId mcpSessId,
      OperatingSurface operatingSurface) {

    this(
        TRANSPORT_WEBSOCKET,
        Objects.requireNonNull(wsConnId, "wsConnId").asString(),
        mcpSessId,
        operatingSurface,
        null);
  }

  public TranspSess(
      WsConnId wsConnId,
      McpSessId mcpSessId,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding) {

    this(
        wsConnId,
        mcpSessId,
        operatingSurface,
        requestAuthBinding,
        new McpSessRTMetaFactory());
  }

  public TranspSess(
      WsConnId wsConnId,
      McpSessId mcpSessId,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding,
      McpSessRTMetaFactory runtimeMetaFactory) {

    this(
        TRANSPORT_WEBSOCKET,
        Objects.requireNonNull(wsConnId, "wsConnId").asString(),
        mcpSessId,
        operatingSurface,
        requestAuthBinding,
        runtimeMetaFactory);
  }

  // =========================================================
  // PHYSICAL IDENTITY (TRANSPORT)
  // =========================================================

  public String TranspSessGetTranspName() {
    return transportName;
  }

  public String TranspSessGetTranspConnId() {
    return transportConnectionId;
  }

  public boolean TranspSessIsWsTransp() {
    return TRANSPORT_WEBSOCKET.equals(transportName);
  }

  public boolean TranspSessIsReqScoped() {
    return transportConnectionId.contains(REQUEST_SCOPED_CONN_MARKER);
  }

  // =========================================================
  // PHYSICAL IDENTITY (WS COMPAT)
  // =========================================================

  public WsConnId TranspSessGetWsConnIdObject() {
    if (!TranspSessIsWsTransp()) {
      return null;
    }
    return WsConnId.fromString(transportConnectionId);
  }

  public String TranspSessGetWsConnId() {
    if (!TranspSessIsWsTransp()) {
      return null;
    }
    return transportConnectionId;
  }

  // =========================================================
  // LOGICAL IDENTITY (MCP)
  // =========================================================

  public McpSessId TranspSessGetMcpSessIdObject() {
    return mcpSessId;
  }

  public String TranspSessGetMcpSessId() {
    return mcpSessId.asString();
  }

  public OperatingSurface TranspSessGetOperatingSurface() {
    return operatingSurface;
  }

  public String TranspSessGetOperatingSurfaceId() {
    return operatingSurface.OperatingSurfaceGetId();
  }

  public ReqsAuthBinding TranspSessGetReqsAuthBinding() {
    return requestAuthBinding;
  }

  public void TranspSessSetReqsAuthBinding(ReqsAuthBinding requestAuthBinding) {
    this.requestAuthBinding = requestAuthBinding;
    this.runtimeMeta = runtimeMetaFactory.McpSessRTMetaFactoryFor(
        mcpSessId,
        operatingSurface,
        requestAuthBinding,
        runtimeMeta == null ? null : runtimeMeta.activeTenantId(),
        runtimeMeta == null ? null : runtimeMeta.resumeCapabilityId());
  }

  public McpSessRTMeta TranspSessGetRuntimeMeta() {
    return runtimeMeta;
  }

  public void TranspSessSetRuntimeMeta(McpSessRTMeta runtimeMeta) {
    this.runtimeMeta = Objects.requireNonNull(runtimeMeta, "runtimeMeta");
  }

  public String TranspSessGetRTMcpSessTypeId() {
    McpSessRTMeta meta = runtimeMeta;
    return meta == null ? null : meta.McpSessRTMetaGetSessionType().RTMcpSessTypeGetId();
  }

  public String TranspSessGetRTMcpSessPhaseId() {
    McpSessRTMeta meta = runtimeMeta;
    return meta == null ? null : meta.McpSessRTMetaGetSessionPhase().RTMcpSessPhaseGetId();
  }

  public long TranspSessGetRuntimeSessionVersion() {
    McpSessRTMeta meta = runtimeMeta;
    return meta == null ? 0L : meta.McpSessRTMetaGetSessionVersion();
  }

  public long TranspSessGetRuntimeInactivityTtlSeconds() {
    McpSessRTMeta meta = runtimeMeta;
    return meta == null ? 0L : meta.McpSessRTMetaGetInactivityTtlSeconds();
  }

  public String TranspSessGetRuntimeResumeCapabilityId() {
    McpSessRTMeta meta = runtimeMeta;
    return meta == null ? null : meta.resumeCapabilityId();
  }

  public String TranspSessGetRuntimeActiveTenantId() {
    McpSessRTMeta meta = runtimeMeta;
    return meta == null ? null : meta.activeTenantId();
  }

  // =========================================================
  // TRANSPORT STATE
  // =========================================================

  /**
   * Transp active flag.
   *
   * Notera:
   * - Påverkar endast transport-livscykel.
   * - Logical session-state hanteras separat.
   */
  public boolean TranspSessIsActive() {
    return active.get();
  }

  /**
   * Close transport-session.
   *
   * @return true om transition skedde (active → false)
   */
  public boolean TranspSessClose() {
    boolean closed = active.compareAndSet(true, false);
    if (closed) {
      messageLedger.MsgLedgerAbortOpenMessages();
    }
    return closed;
  }

  public void TranspSessRecordReceived(String correlationId) {
    messageLedger.MsgLedgerRecordReceived(correlationId);
  }

  public void TranspSessRecordRouted(String correlationId) {
    messageLedger.MsgLedgerRecordRouted(correlationId);
  }

  public void TranspSessRecordResponded(String correlationId) {
    messageLedger.MsgLedgerRecordResponded(correlationId);
  }

  public void TranspSessRecordRejected(String correlationId) {
    messageLedger.MsgLedgerRecordRejected(correlationId);
  }

  public void TranspSessRecordTimedOut(String correlationId) {
    messageLedger.MsgLedgerRecordTimedOut(correlationId);
  }

  public void TranspSessRecordAborted(String correlationId) {
    messageLedger.MsgLedgerRecordAborted(correlationId);
  }

  public TranspMessageLedger.MessageState TranspSessGetMessageState(String correlationId) {
    return messageLedger.MsgLedgerGetState(correlationId);
  }

  public TranspMessageLedger.Snapshot TranspSessGetMessageAccountingSnapshot() {
    return messageLedger.MsgLedgerSnapshot();
  }
}
