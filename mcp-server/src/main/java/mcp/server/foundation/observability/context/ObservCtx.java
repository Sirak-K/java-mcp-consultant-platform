package mcp.server.foundation.observability.context;

import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMeta;

/**
 * Immutable observation context.
 *
 * Holds optional tracing fields without forcing every runtime event to provide
 * them.
 */
public final class ObservCtx {

  private final String mcpSessId;
  private final String wsConnId;
  private final String rpcCorrelaId;
  private final String rpcMet;
  private final String toolName;
  private final Long requestStartNano;
  private final String transportName;
  private final String sessionPhase;
  private final String clientAddress;
  private final String errorType;
  private final ReqsAuthBinding requestAuthBinding;
  private final McpSessRTMeta runtimeMeta;

  private ObservCtx(Builder builder) {
    this.mcpSessId = normalize(builder.mcpSessId);
    this.wsConnId = normalize(builder.wsConnId);
    this.rpcCorrelaId = normalize(builder.rpcCorrelaId);
    this.rpcMet = normalize(builder.rpcMet);
    this.toolName = normalize(builder.toolName);
    this.requestStartNano = builder.requestStartNano;
    this.transportName = normalize(builder.transportName);
    this.sessionPhase = normalize(builder.sessionPhase);
    this.clientAddress = normalize(builder.clientAddress);
    this.errorType = normalize(builder.errorType);
    this.requestAuthBinding = builder.requestAuthBinding;
    this.runtimeMeta = builder.runtimeMeta;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String ObservCtxGetMcpSessId() {
    return mcpSessId;
  }

  public String ObservCtxGetWsConnId() {
    return wsConnId;
  }

  public String ObservCtxGetRPCCorrelaId() {
    return rpcCorrelaId;
  }

  public String ObservCtxGetRPCMet() {
    return rpcMet;
  }

  public String ObservCtxGetToolName() {
    return toolName;
  }

  public Long ObservCtxGetReqsStartNano() {
    return requestStartNano;
  }

  public String ObservCtxGetTranspName() {
    return transportName;
  }

  public String ObservCtxGetSessPhase() {
    return sessionPhase;
  }

  public String ObservCtxGetClientAddress() {
    return clientAddress;
  }

  public String ObservCtxGetErrType() {
    return errorType;
  }

  public ReqsAuthBinding ObservCtxGetReqsAuthBinding() {
    return requestAuthBinding;
  }

  public McpSessRTMeta ObservCtxGetRuntimeMeta() {
    return runtimeMeta;
  }

  public String ObservCtxGetRTMcpSessTypeId() {
    return runtimeMeta == null ? null : runtimeMeta.McpSessRTMetaGetSessionType().RTMcpSessTypeGetId();
  }

  public String ObservCtxGetRTMcpSessPhaseId() {
    return runtimeMeta == null ? null : runtimeMeta.McpSessRTMetaGetSessionPhase().RTMcpSessPhaseGetId();
  }

  public long ObservCtxGetRuntimeSessionVersion() {
    return runtimeMeta == null ? 0L : runtimeMeta.McpSessRTMetaGetSessionVersion();
  }

  public long ObservCtxGetRuntimeInactivityTtlSeconds() {
    return runtimeMeta == null ? 0L : runtimeMeta.McpSessRTMetaGetInactivityTtlSeconds();
  }

  public String ObservCtxGetRuntimeResumeCapabilityId() {
    return runtimeMeta == null ? null : runtimeMeta.resumeCapabilityId();
  }

  public String ObservCtxGetRuntimeActiveTenantId() {
    return runtimeMeta == null ? null : runtimeMeta.activeTenantId();
  }

  public boolean ObservCtxGetRuntimeDurableTarget() {
    return runtimeMeta != null && runtimeMeta.McpSessRTMetaIsDurableTarget();
  }

  public boolean ObservCtxGetRuntimeResumeSupported() {
    return runtimeMeta != null && runtimeMeta.McpSessRTMetaIsResumeSupported();
  }

  public boolean ObservCtxGetRuntimeRequiresActiveTenant() {
    return runtimeMeta != null && runtimeMeta.McpSessRTMetaRequiresActiveTenant();
  }

  public Builder ObservCtxToBuilder() {
    return ObservCtx.builder()
        .mcpSessId(mcpSessId)
        .wsConnId(wsConnId)
        .rpcCorrelaId(rpcCorrelaId)
        .rpcMet(rpcMet)
        .toolName(toolName)
        .requestStartNano(requestStartNano)
        .transportName(transportName)
        .sessionPhase(sessionPhase)
        .clientAddress(clientAddress)
        .errorType(errorType)
        .requestAuthBinding(requestAuthBinding)
        .runtimeMeta(runtimeMeta);
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  public static final class Builder {

    private String mcpSessId;
    private String wsConnId;
    private String rpcCorrelaId;
    private String rpcMet;
    private String toolName;
    private Long requestStartNano;
    private String transportName;
    private String sessionPhase;
    private String clientAddress;
    private String errorType;
    private ReqsAuthBinding requestAuthBinding;
    private McpSessRTMeta runtimeMeta;

    private Builder() {
    }

    public Builder mcpSessId(String mcpSessId) {
      this.mcpSessId = mcpSessId;
      return this;
    }

    public Builder wsConnId(String wsConnId) {
      this.wsConnId = wsConnId;
      return this;
    }

    public Builder rpcCorrelaId(String rpcCorrelaId) {
      this.rpcCorrelaId = rpcCorrelaId;
      return this;
    }

    public Builder rpcMet(String rpcMet) {
      this.rpcMet = rpcMet;
      return this;
    }

    public Builder toolName(String toolName) {
      this.toolName = toolName;
      return this;
    }

    public Builder requestStartNano(Long requestStartNano) {
      this.requestStartNano = requestStartNano;
      return this;
    }

    public Builder transportName(String transportName) {
      this.transportName = transportName;
      return this;
    }

    public Builder sessionPhase(String sessionPhase) {
      this.sessionPhase = sessionPhase;
      return this;
    }

    public Builder clientAddress(String clientAddress) {
      this.clientAddress = clientAddress;
      return this;
    }

    public Builder errorType(String errorType) {
      this.errorType = errorType;
      return this;
    }

    public Builder requestAuthBinding(ReqsAuthBinding requestAuthBinding) {
      this.requestAuthBinding = requestAuthBinding;
      return this;
    }

    public Builder runtimeMeta(McpSessRTMeta runtimeMeta) {
      this.runtimeMeta = runtimeMeta;
      return this;
    }

    public ObservCtx build() {
      return new ObservCtx(this);
    }
  }
}
