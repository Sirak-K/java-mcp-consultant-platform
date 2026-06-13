package mcp.server.foundation.server_process.client_context.session.metadata;

import java.time.Instant;
import java.util.Objects;

import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.orchestration.RTMcpSessLifecyContract;
import mcp.server.foundation.server_process.orchestration.RTMcpSessModelReg;
import mcp.server.foundation.server_process.orchestration.RTMcpSessPhase;
import mcp.server.foundation.server_process.orchestration.RTMcpSessRestoreCore;
import mcp.server.foundation.server_process.orchestration.RTMcpSessType;

/**
 * Helper for building the canonical runtime/session envelope.
 */
public final class McpSessRTMetaFactory {

  private static final long DEFAULT_INACTIVITY_TTL_SECONDS = 1800L;
  private static final long DEFAULT_SESSION_VERSION = 1L;
  private final RTMcpSessModelReg runtimeSessionModelRegistry;

  public McpSessRTMetaFactory() {
    this(McpSessRTMetaFactoryDefaultRegistry());
  }

  public McpSessRTMetaFactory(RTMcpSessModelReg runtimeSessionModelRegistry) {
    this.runtimeSessionModelRegistry = Objects.requireNonNull(
        runtimeSessionModelRegistry,
        "runtimeSessionModelRegistry");
  }

  public McpSessRTMeta McpSessRTMetaFactoryDefault(
      McpSessId sessionId,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding) {

    RTMcpSessType sessionType = resolveSessionType(operatingSurface);
    RTMcpSessLifecyContract lifecycleContract = runtimeSessionModelRegistry
        .RTMcpSessModelRegGet(sessionType);
    RTMcpSessPhase initialPhase = lifecycleContract
        .RTMcpSessLifecyContractGetInitialPhase();

    Instant now = Instant.now();
    RTMcpSessRestoreCore restoreCore = new RTMcpSessRestoreCore(
        sessionId.asString(),
        requestAuthBinding,
        sessionType,
        initialPhase,
        lifecycleContract.RTMcpSessLifecyContractGetInactivityTtlSeconds(),
        DEFAULT_SESSION_VERSION);

    String activeTenantId = resolveActiveTenantId(requestAuthBinding);
    String resumeCapabilityId = sessionId.asString()
        + ":"
        + sessionType.RTMcpSessTypeGetId()
        + ":v"
        + DEFAULT_SESSION_VERSION;

    return new McpSessRTMeta(
        lifecycleContract,
        restoreCore,
        activeTenantId,
        resumeCapabilityId,
        now,
        now,
        now.plusSeconds(lifecycleContract.RTMcpSessLifecyContractGetInactivityTtlSeconds()));
  }

  public McpSessRTMeta McpSessRTMetaFactoryFor(
      McpSessId sessionId,
      OperatingSurface operatingSurface,
      ReqsAuthBinding requestAuthBinding,
      String activeTenantId,
      String resumeCapabilityId) {

    McpSessRTMeta base = McpSessRTMetaFactoryDefault(
        sessionId,
        operatingSurface,
        requestAuthBinding);

    McpSessRTMeta withTenant = base.McpSessRTMetaWithActiveTenantId(activeTenantId);
    return withTenant.McpSessRTMetaWithResumeCapabilityId(resumeCapabilityId);
  }

  public McpSessRTMeta McpSessRTMetaFactoryTouch(McpSessRTMeta runtimeMeta) {
    if (runtimeMeta == null) {
      return null;
    }
    return runtimeMeta.McpSessRTMetaTouch(Instant.now());
  }

  public McpSessRTMeta McpSessRTMetaFactoryAdvancePhase(
      McpSessRTMeta runtimeMeta,
      RTMcpSessPhase nextPhase) {

    Objects.requireNonNull(runtimeMeta, "runtimeMeta");
    Objects.requireNonNull(nextPhase, "nextPhase");

    RTMcpSessRestoreCore currentRestoreCore = runtimeMeta.restoreCore();
    RTMcpSessRestoreCore nextRestoreCore =
        currentRestoreCore.RTMcpSessRestoreCoreWithPhase(nextPhase);

    return runtimeMeta
        .McpSessRTMetaWithRestoreCore(nextRestoreCore)
        .McpSessRTMetaTouch(Instant.now());
  }

  private static RTMcpSessType resolveSessionType(OperatingSurface operatingSurface) {
    if (operatingSurface == null) {
      return RTMcpSessType.MCP_SESSION;
    }

    return switch (operatingSurface) {
      case APP_ADAPTER -> RTMcpSessType.APP_PRE_SESSION;
      case PLATFORM_OPS -> RTMcpSessType.PLATFORM_OPS_SESSION;
      case MCP_DIRECT -> RTMcpSessType.MCP_SESSION;
    };
  }

  private static String resolveActiveTenantId(ReqsAuthBinding requestAuthBinding) {
    if (requestAuthBinding == null || !requestAuthBinding.ReqsAuthBindingHasActiveTenant()) {
      return null;
    }
    return requestAuthBinding.activeTenant().tenantId();
  }

  private static RTMcpSessModelReg McpSessRTMetaFactoryDefaultRegistry() {
    return RTMcpSessModelReg.RTMcpSessModelRegDefault(DEFAULT_INACTIVITY_TTL_SECONDS);
  }
}
