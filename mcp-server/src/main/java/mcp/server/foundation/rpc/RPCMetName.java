package mcp.server.foundation.rpc;

import java.util.Set;

/**
 * Canonical JSON-RPC method names owned by the MCP server foundation layer.
 */
public final class RPCMetName {

  public static final String INITIALIZE = "initialize";
  public static final String SESSIONS_SUBSCRIBE = "sessions/subscribe";
  public static final String SESSIONS_SNAPSHOT = "sessions/snapshot";
  public static final String SESSION_EVENT = "session/event";
  public static final String TOOLS_LIST = "tools/list";
  public static final String TOOLS_CALL = "tools/call";
  public static final String TOOLS_CANCEL = "tools/cancel";
  public static final String RESOURCES_LIST = "resources/list";
  public static final String RESOURCES_TEMPLATES_LIST = "resources/templates/list";
  public static final String RESOURCES_READ = "resources/read";
  public static final String PROMPTS_LIST = "prompts/list";
  public static final String PROMPTS_GET = "prompts/get";
  public static final String OPS_HEALTHCHECK = "ops.healthcheck";
  public static final String PING = "ping";

  private static final Set<String> PRE_INIT_ALLOWED_METHODS = Set.of(
      INITIALIZE,
      SESSIONS_SUBSCRIBE,
      OPS_HEALTHCHECK,
      TOOLS_LIST,
      RESOURCES_LIST,
      RESOURCES_TEMPLATES_LIST,
      RESOURCES_READ,
      PROMPTS_LIST,
      PROMPTS_GET);

  private static final Set<String> SESSIONLESS_FOUNDATION_METHODS = Set.of(
      TOOLS_LIST,
      RESOURCES_LIST,
      RESOURCES_TEMPLATES_LIST,
      RESOURCES_READ,
      PROMPTS_LIST,
      PROMPTS_GET,
      OPS_HEALTHCHECK,
      PING);

  private static final Set<String> RESERVED_TOOL_NAMES = Set.of(
      INITIALIZE,
      SESSIONS_SUBSCRIBE,
      SESSIONS_SNAPSHOT,
      TOOLS_LIST,
      TOOLS_CANCEL,
      SESSION_EVENT,
      PING);

  private RPCMetName() {
  }

  public static boolean RPCMetNameIsPreInitAllowed(String method) {
    return method != null && PRE_INIT_ALLOWED_METHODS.contains(method);
  }

  public static boolean RPCMetNameIsSessionlessFoundationMethod(String method) {
    return method != null && SESSIONLESS_FOUNDATION_METHODS.contains(method);
  }

  public static boolean RPCMetNameIsReservedToolName(String toolName) {
    return toolName != null && RESERVED_TOOL_NAMES.contains(toolName);
  }
}
