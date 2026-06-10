package mcp.server.foundation.server_process.client_context.session.event;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspSignalModel;
import mcp.server.foundation.rpc.RPCJsonSeria;
import mcp.server.foundation.rpc.RPCReqsPayl;
import mcp.server.foundation.rpc.RPCRouter;
import mcp.server.foundation.server_process.client_context.session.McpSessBindingReg;
import mcp.server.foundation.server_process.client_context.session.McpSessReg;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.TranspContractSupport;
import mcp.server.foundation.transport.TranspSess;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class SentinelNotificationSupport {

  private final McpSessReg registry;
  private final McpSessBindingReg bindingRegistry;
  private final TranspAdap transport;
  private final RPCJsonSeria serializer;
  private final ServerLogger logger;
  private final RTMetrics runtimeMetrics;
  private final ObservCtxFactory obsCtxFactory;

  public SentinelNotificationSupport(
      McpSessReg registry,
      McpSessBindingReg bindingRegistry,
      TranspAdap transport,
      RPCJsonSeria serializer,
      ServerLogger logger,
      RTMetrics runtimeMetrics,
      ObservCtxFactory obsCtxFactory) {

    this.registry = Objects.requireNonNull(registry, "registry");
    this.bindingRegistry = Objects.requireNonNull(bindingRegistry, "bindingRegistry");
    this.transport = Objects.requireNonNull(transport, "transport");
    this.serializer = Objects.requireNonNull(serializer, "serializer");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
  }

  public void SentinelNotificationSupportPublishToAll(
      String rpcMethod,
      Map<String, Object> params,
      ServerLogger.Component component,
      String logEventId,
      String logMessagePrefix,
      String failureMessagePrefix) {

    Set<McpSessId> sentinelIds = registry.SessRegGetSentinelMcpSessIds();
    if (sentinelIds.isEmpty()) {
      return;
    }

    String json = notificationJson(rpcMethod, params);
    String rpcCorrelaId = RPCRouter.RPCRouterResolveCorrelaId((RPCReqsPayl) null);

    for (McpSessId sentinelId : sentinelIds) {
      sendTo(
          sentinelId,
          rpcMethod,
          json,
          rpcCorrelaId,
          component,
          logEventId,
          logMessagePrefix,
          failureMessagePrefix);
    }
  }

  public void SentinelNotificationSupportPublishTo(
      McpSessId targetSentinelId,
      String rpcMethod,
      Map<String, Object> params,
      ServerLogger.Component component,
      String logEventId,
      String logMessagePrefix,
      String failureMessagePrefix) {

    Objects.requireNonNull(targetSentinelId, "targetSentinelId");

    String json = notificationJson(rpcMethod, params);
    String rpcCorrelaId = RPCRouter.RPCRouterResolveCorrelaId((RPCReqsPayl) null);

    sendTo(
        targetSentinelId,
        rpcMethod,
        json,
        rpcCorrelaId,
        component,
        logEventId,
        logMessagePrefix,
        failureMessagePrefix);
  }

  private String notificationJson(String rpcMethod, Map<String, Object> params) {
    Objects.requireNonNull(rpcMethod, "rpcMethod");
    Objects.requireNonNull(params, "params");

    Map<String, Object> notification = Map.of(
        "jsonrpc", "2.0",
        "method", rpcMethod,
        "params", params);

    return serializer.JsonRPCSerSerialize(notification);
  }

  private void sendTo(
      McpSessId sentinelId,
      String rpcMethod,
      String json,
      String rpcCorrelaId,
      ServerLogger.Component component,
      String logEventId,
      String logMessagePrefix,
      String failureMessagePrefix) {

    try {
      TranspSess sentinel = transport.TranspAdapGetSessionById(sentinelId.toString());

      if (sentinel == null || !sentinel.TranspSessIsActive()) {
        return;
      }

      String transportConnectionId = TranspContractSupport.TransContNormalizeConnId(
          bindingRegistry.getTranspConnId(sentinelId));
      String transportName = resolveTranspName(sentinelId);

      incrementOutbCounter(transportName);

      ObservCtx context = obsCtxFactory.ObservCtxFactoryWithRpcMetadata(
          obsCtxFactory.ObservCtxFactoryFromTranspCoordinates(
              transportName,
              transportConnectionId,
              sentinelId.toString()),
          rpcCorrelaId,
          rpcMethod);

      logger.ServerLogInfoObserved(
          component,
          context,
          "PUBLISH",
          logEventId,
          logMessagePrefix + " bytes=" + (json == null ? 0 : json.length()));

      transport.TranspAdapSendTo(sentinel, json);
    } catch (Exception ex) {
      incrementTranspErrCounter(resolveTranspName(sentinelId));
      logger.ServerLogErrorStructured(
          component,
          sentinelId.toString(),
          TranspContractSupport.TRANSPORT_UNBOUND,
          rpcCorrelaId,
          failureMessagePrefix + ex.getMessage(),
          ex);
    }
  }

  private String resolveTranspName(McpSessId mcpSessId) {
    String transportName = bindingRegistry.getTranspName(mcpSessId);

    return transportName == null || transportName.isBlank()
        ? transport.TranspAdapGetTranspName()
        : transportName;
  }

  private void incrementOutbCounter(String transportName) {
    runtimeMetrics.RTMetricsIncrementCounter(
        TranspSignalModel.TransSigOutbMessagesMetricName(transportName));
  }

  private void incrementTranspErrCounter(String transportName) {
    runtimeMetrics.RTMetricsIncrementCounter(
        TranspSignalModel.TransSigTranspErrorsMetricName(transportName));
  }
}
