package mcp.server.foundation.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.prompt_interface.PromptLoader;
import mcp.server.foundation.prompt_interface.PromptReg;
import mcp.server.foundation.prompt_interface.PromptRenderResult;
import mcp.server.foundation.prompt_interface.PromptService;
import mcp.server.foundation.resource_interface.ResrcDefin;
import mcp.server.foundation.resource_interface.ResrcReg;
import mcp.server.foundation.server_process.client_context.session.McpSessReg;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.tool_interface.ToolInvocEngine;
import mcp.server.foundation.tool_interface.ToolReqs;
import mcp.server.foundation.tool_interface.ToolResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * RPCMetResol
 *
 * Ansvar:
 * - Metodspecifik resolution för foundation-RPC.
 * - Exponerar MCP capabilities: tools, resources och prompts.
 * - Delegation till ToolInvocEngine för domain tools.
 *
 * Viktigt:
 * - Router äger state-verification och exception->response mapping.
 * - Router/SessionRTOrch äger side-effects (t.ex markSentinel).
 */
public final class RPCMetResol {

  private final ToolInvocEngine toolEngine;
  private final PromptReg promptReg;
  private final PromptService promptService;
  private final ResrcReg resourceReg;
  private final RPCCapaDscr rpcCapaDscr;
  private final RPCJsonSeria rpcJsonSeria;
  private final McpSessReg sessionRegistry;
  private final ObservCtxFactory obsCtxFactory;
  private final ServerLogger logger;

  public RPCMetResol(
      ToolInvocEngine toolEngine,
      ResrcReg resourceReg,
      RPCCapaDscr descriptor,
      RPCJsonSeria rpcJsonSeria,
      McpSessReg sessionRegistry,
      ObservCtxFactory obsCtxFactory,
      ServerLogger logger) {
    this(
        toolEngine,
        PromptSurfaceDefaults.create(),
        resourceReg,
        descriptor,
        rpcJsonSeria,
        sessionRegistry,
        obsCtxFactory,
        logger);
  }

  private RPCMetResol(
      ToolInvocEngine toolEngine,
      PromptSurfaceDefaults promptDefaults,
      ResrcReg resourceReg,
      RPCCapaDscr descriptor,
      RPCJsonSeria rpcJsonSeria,
      McpSessReg sessionRegistry,
      ObservCtxFactory obsCtxFactory,
      ServerLogger logger) {
    this(
        toolEngine,
        promptDefaults.promptReg(),
        promptDefaults.promptService(),
        resourceReg,
        descriptor,
        rpcJsonSeria,
        sessionRegistry,
        obsCtxFactory,
        logger);
  }

  public RPCMetResol(
      ToolInvocEngine toolEngine,
      PromptReg promptReg,
      PromptService promptService,
      ResrcReg resourceReg,
      RPCCapaDscr descriptor,
      RPCJsonSeria rpcJsonSeria,
      McpSessReg sessionRegistry,
      ObservCtxFactory obsCtxFactory,
      ServerLogger logger) {

    this.toolEngine = Objects.requireNonNull(toolEngine, "toolEngine");
    this.promptReg = Objects.requireNonNull(promptReg, "promptReg");
    this.promptService = Objects.requireNonNull(promptService, "promptService");
    this.resourceReg = Objects.requireNonNull(resourceReg, "resourceReg");
    this.rpcCapaDscr = Objects.requireNonNull(descriptor, "descriptor");
    this.rpcJsonSeria = Objects.requireNonNull(rpcJsonSeria, "rpcJsonSeria");
    this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public RPCRespPayl RpcMetResoResolve(
      RPCReqsPayl request,
      McpSessId mcpSessId,
      ObservCtx context) {

    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(mcpSessId, "mcpSessId");

    String method = request.RPCReqPlGetMet();
    Objects.requireNonNull(method, "method");

    JsonNode id = request.RPCReqPlGetId();
    Objects.requireNonNull(id, "id");

    JsonNode params = request.RPCReqPlGetParams();
    ObservCtx safeContext = context == null
        ? obsCtxFactory.ObservCtxFactoryForRpc(
            mcpSessId.toString(),
            null,
            RPCRouter.RPCRouterResolveCorrelaId(request),
            method)
        : context;

    // =========================================================
    // Foundation RPC methods
    // =========================================================

    if (RPCMetName.INITIALIZE.equals(method)) {
      logResolvedFoundationMet(safeContext, RPCMetName.INITIALIZE);
      String negotiatedProtocolVersion = rpcCapaDscr.RPCCapaDescResolveNegotiatedProtocolVersion(
          extractInitializeProtocolVersion(params));
      Map<String, Object> init = rpcCapaDscr.RPCCapaDescBuildInitializeResult(
          resourceReg,
          promptReg,
          negotiatedProtocolVersion);
      return RPCRespPayl.RpcRespPlResult(id, init);
    }

    if (RPCMetName.SESSIONS_SUBSCRIBE.equals(method)) {
      logResolvedFoundationMet(safeContext, RPCMetName.SESSIONS_SUBSCRIBE);
      Map<String, Boolean> subscribe = rpcCapaDscr.RPCCapaDescBuildSessionsSubscribeResult();
      return RPCRespPayl.RpcRespPlResult(id, subscribe);
    }

    if (RPCMetName.SESSIONS_SNAPSHOT.equals(method)) {
      logResolvedFoundationMet(safeContext, RPCMetName.SESSIONS_SNAPSHOT);
      Set<McpSessId> allIds = sessionRegistry.SessRegGetAllSessIds();
      List<String> sessionIds = allIds.stream()
          .map(McpSessId::toString)
          .toList();
      Map<String, Object> snapshot = Map.of(
          "sessionIds", sessionIds,
          "sessionCount", sessionIds.size());
      return RPCRespPayl.RpcRespPlResult(id, snapshot);
    }

    if (RPCMetName.TOOLS_LIST.equals(method)) {
      logResolvedFoundationMet(safeContext, RPCMetName.TOOLS_LIST);
      // Tool list är ett RPC-metodnamn, men payload byggs från ToolReg.
      // Resultatet ska vara MCP-format: { tools: [.] }
      Map<String, Object> list = rpcCapaDscr.RPCCapaDescBuildToolsListResult(
          toolEngine.ToolInvocGetReg());
      return RPCRespPayl.RpcRespPlResult(id, list);
    }

    if (RPCMetName.RESOURCES_LIST.equals(method)) {
      logResolvedFoundationMet(safeContext, RPCMetName.RESOURCES_LIST);
      Map<String, Object> list = rpcCapaDscr.RPCCapaDescBuildResourcesListResult(resourceReg);
      return RPCRespPayl.RpcRespPlResult(id, list);
    }

    if (RPCMetName.RESOURCES_TEMPLATES_LIST.equals(method)) {
      logResolvedFoundationMet(safeContext, RPCMetName.RESOURCES_TEMPLATES_LIST);
      Map<String, Object> list = rpcCapaDscr.RPCCapaDescBuildResourceTemplatesListResult(resourceReg);
      return RPCRespPayl.RpcRespPlResult(id, list);
    }

    if (RPCMetName.PROMPTS_LIST.equals(method)) {
      logResolvedFoundationMet(safeContext, RPCMetName.PROMPTS_LIST);
      Map<String, Object> list = rpcCapaDscr.RPCCapaDescBuildPromptsListResult(promptReg);
      return RPCRespPayl.RpcRespPlResult(id, list);
    }

    if (RPCMetName.RESOURCES_READ.equals(method)) {
      String resourceUri = null;

      if (params != null && !params.isNull()) {
        JsonNode uriNode = params.get("uri");
        if (uriNode != null && uriNode.isTextual()) {
          resourceUri = uriNode.asText();
        }
      }

      if (resourceUri == null || resourceUri.isBlank()) {
        throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
            RPCMetName.RESOURCES_READ + " requires params.uri");
      }

      ResrcDefin resourceDefinition = resourceReg.ResrcRegGetDefin(resourceUri);
      if (resourceDefinition == null) {
        throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
            "Unknown resource: " + resourceUri);
      }

      logger.ServerLogInfoObserved(
          ServerLogger.Component.RPC,
          safeContext,
          "RESOLVE",
          "RPC_RESOURCE_READ_METHOD_RESOLVED",
          "RPCMetResol: resolved " + RPCMetName.RESOURCES_READ + " -> " + resourceUri);

      return RPCRespPayl.RpcRespPlResult(
          id,
          resourceDefinition.ResrcDefReadToMcpFormat(rpcJsonSeria));
    }

    if (RPCMetName.PROMPTS_GET.equals(method)) {
      String promptName = null;
      JsonNode argumentsNode = null;

      if (params != null && !params.isNull()) {
        JsonNode nameNode = params.get("name");
        if (nameNode != null && nameNode.isTextual()) {
          promptName = nameNode.asText();
        }
        argumentsNode = params.get("arguments");
      }

      if (promptName == null || promptName.isBlank()) {
        throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
            RPCMetName.PROMPTS_GET + " requires params.name");
      }

      Map<String, Object> arguments = argumentsNode == null || argumentsNode.isNull()
          ? Map.of()
          : rpcJsonSeria.RPCJsonSerNodeToMap(argumentsNode);

      try {
        PromptRenderResult promptRenderResult = promptService.PromptSvcRender(promptName, arguments);
        return RPCRespPayl.RpcRespPlResult(
            id,
            rpcCapaDscr.RPCCapaDescBuildPromptsGetResult(promptRenderResult));
      } catch (IllegalArgumentException ex) {
        throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(ex.getMessage());
      }
    }

    if (RPCMetName.TOOLS_CANCEL.equals(method)) {
      logResolvedFoundationMet(safeContext, RPCMetName.TOOLS_CANCEL);

      String requestId = null;
      if (params != null && !params.isNull()) {
        JsonNode requestIdNode = params.get("requestId");
        if (requestIdNode != null && requestIdNode.isTextual()) {
          requestId = requestIdNode.asText();
        }
      }

      if (requestId == null || requestId.isBlank()) {
        throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
            RPCMetName.TOOLS_CANCEL + " requires params.requestId");
      }

      boolean cancelled = toolEngine.ToolInvocCancel(requestId);
      return RPCRespPayl.RpcRespPlResult(id, Map.of("cancelled", cancelled));
    }

    if (RPCMetName.TOOLS_CALL.equals(method)) {
      // MCP spec: params = { "name": "<tool>", "arguments": { ... } }
      String toolName = null;
      JsonNode arguments = null;

      if (params != null && !params.isNull()) {
        JsonNode nameNode = params.get("name");
        if (nameNode != null && nameNode.isTextual()) {
          toolName = nameNode.asText();
        }
        arguments = params.get("arguments");
      }

      if (toolName == null || toolName.isBlank()) {
        logger.ServerLogWarnObserved(
            ServerLogger.Component.RPC,
            safeContext,
            "VALIDATE",
            "RPC_TOOL_NAME_INVALID",
            "RPCMetResol: " + RPCMetName.TOOLS_CALL + " missing params.name",
            "VALIDATION_ERROR");

        throw RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
            RPCMetName.TOOLS_CALL + " requires params.name");
      }

      ObservCtx toolContext = copyCtxWithToolName(safeContext, toolName);

      logger.ServerLogInfoObserved(
          ServerLogger.Component.RPC,
          toolContext,
          "RESOLVE",
          "RPC_TOOL_CALL_METHOD_RESOLVED",
          "RPCMetResol: resolved " + RPCMetName.TOOLS_CALL + " -> " + toolName);

      RPCSessPhase phase = sessionRegistry.SessRegGetPhase(mcpSessId);
      ToolInvocEngine.ExecuteMode mode = (phase == RPCSessPhase.PRE_INIT)
          ? ToolInvocEngine.ExecuteMode.PRE_INIT
          : ToolInvocEngine.ExecuteMode.POST_INIT;

      ToolReqs toolRequest = ToolReqs.ToolReqFromRpc(toolName, arguments);
      ToolResponse toolResponse = toolEngine.ToolInvocExecute(toolName, toolRequest, mode, toolContext);
      return RPCRespPayl.RpcRespPlResult(id, toolResponse.ToolRespToMcpFormat());
    }

    // RPC ping endpoint hålls reserverad här (ToolReg blockerar "ping")
    if (RPCMetName.PING.equals(method)) {
      logResolvedFoundationMet(safeContext, RPCMetName.PING);
      return RPCRespPayl.RpcRespPlResult(id, Map.of("ok", true));
    }

    // =========================================================
    // Tool Invocation (domain / non-foundation)
    // =========================================================

    ToolReqs toolRequest = ToolReqs.ToolReqFromRpc(method, params);

    RPCSessPhase phase = sessionRegistry.SessRegGetPhase(mcpSessId);

    ToolInvocEngine.ExecuteMode mode = (phase == RPCSessPhase.PRE_INIT)
        ? ToolInvocEngine.ExecuteMode.PRE_INIT
        : ToolInvocEngine.ExecuteMode.POST_INIT;

    ObservCtx directToolContext = copyCtxWithToolName(safeContext, method);

    logger.ServerLogInfoObserved(
        ServerLogger.Component.RPC,
        directToolContext,
        "RESOLVE",
        "RPC_DIRECT_TOOL_METHOD_RESOLVED",
        "RPCMetResol: resolved direct tool method " + method);

    ToolResponse toolResponse = toolEngine.ToolInvocExecute(
        method,
        toolRequest,
        mode,
        directToolContext);

    // ToolResponse payload already conforms to MCP tool result format.
    return RPCRespPayl.RpcRespPlResult(id, toolResponse.ToolRespToMcpFormat());
  }

  private void logResolvedFoundationMet(ObservCtx context, String method) {
    logger.ServerLogInfoObserved(
        ServerLogger.Component.RPC,
        context,
        "RESOLVE",
        "RPC_FOUNDATION_METHOD_RESOLVED",
        "RPCMetResol: resolved foundation method " + method);
  }

  private static String extractInitializeProtocolVersion(JsonNode params) {

    if (params == null || params.isNull() || !params.isObject()) {
      throw RPCMappedExcep.RPCMappedExcepInvalidParams(
          RPCMetName.INITIALIZE + " requires object params with protocolVersion");
    }

    JsonNode protocolVersionNode = params.get("protocolVersion");
    if (protocolVersionNode == null || !protocolVersionNode.isTextual()) {
      throw RPCMappedExcep.RPCMappedExcepInvalidParams(
          RPCMetName.INITIALIZE + " requires params.protocolVersion");
    }

    String protocolVersion = protocolVersionNode.asText();
    if (protocolVersion == null || protocolVersion.isBlank()) {
      throw RPCMappedExcep.RPCMappedExcepInvalidParams(
          RPCMetName.INITIALIZE + " requires non-blank params.protocolVersion");
    }

    return protocolVersion;
  }

  private ObservCtx copyCtxWithToolName(ObservCtx context, String toolName) {
    return obsCtxFactory.ObservCtxFactoryWithToolName(context, toolName);
  }

  private record PromptSurfaceDefaults(
      PromptReg promptReg,
      PromptService promptService) {

    private static PromptSurfaceDefaults create() {
      PromptReg registry = new PromptReg();
      return new PromptSurfaceDefaults(
          registry,
          new PromptService(registry, new PromptLoader()));
    }
  }
}
