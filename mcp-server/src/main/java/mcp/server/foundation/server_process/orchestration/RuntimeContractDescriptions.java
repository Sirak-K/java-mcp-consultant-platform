package mcp.server.foundation.server_process.orchestration;

/**
 * Java-owned descriptions for foundation runtime contracts.
 */
public final class RuntimeContractDescriptions {

  private RuntimeContractDescriptions() {
  }

  public static String operatingSurfaceDescription(OperatingSurface operatingSurface) {
    return switch (operatingSurface) {
      case MCP_DIRECT -> "Direct MCP surface for humans and integrations.";
      case APP_ADAPTER -> "Future application-layer adapter above the shared server core.";
      case PLATFORM_OPS -> "Internal platform operations surface.";
    };
  }

  public static String operatingSurfaceContractSummary(OperatingSurface operatingSurface) {
    return switch (operatingSurface) {
      case MCP_DIRECT -> "Direct MCP is a first-class product surface above the shared server core.";
      case APP_ADAPTER -> "A future app-layer must consume the shared core and must not become a parallel truth layer.";
      case PLATFORM_OPS -> "Internal platform operations are first-class but must remain an internal operating surface.";
    };
  }

  public static String requestLifecycleSummary(OperatingSurface operatingSurface) {
    return switch (operatingSurface) {
      case MCP_DIRECT -> "Direct MCP requests must authenticate first and then execute within exactly one active tenant context.";
      case APP_ADAPTER -> "A future app adapter may start in pre-session but must bind to one tenant before business actions.";
      case PLATFORM_OPS -> "Platform ops run on the shared core with platform identity by default and may only switch tenant via audited assume-tenant.";
    };
  }

  public static String runtimeSessionLifecycleSummary(RTMcpSessType sessionType) {
    return switch (sessionType) {
      case MCP_SESSION -> "Direct MCP sessions start in PRE_INIT, may reconnect/resume, and become bound via later business/session phases.";
      case APP_PRE_SESSION -> "App pre-sessions remain separate from bound business execution and may resume before tenant binding.";
      case BOUND_BUSINESS_SESSION -> "Bound business sessions require one active tenant and any tenant switch creates a new bound session version.";
      case PLATFORM_OPS_SESSION -> "Platform ops sessions are durable and resumable, and any assume-tenant change creates a new bound version.";
    };
  }
}
