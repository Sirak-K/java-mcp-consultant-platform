package mcp.server.foundation.spring_integration;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.tracing.McpObservationSupport;
import mcp.server.foundation.prompt_interface.PromptReg;
import mcp.server.foundation.prompt_interface.PromptService;
import mcp.server.foundation.resource_interface.ResrcReg;
import mcp.server.foundation.rpc.RPCCapaDscr;
import mcp.server.foundation.rpc.RPCJsonEntry;
import mcp.server.foundation.rpc.RPCJsonSeria;
import mcp.server.foundation.rpc.RPCRouter;
import mcp.server.foundation.rpc.error.ErrClassifier;
import mcp.server.foundation.security.request_binding.ReqsLifecyReg;
import mcp.server.foundation.server_process.client_context.session.McpSessBindingReg;
import mcp.server.foundation.server_process.client_context.session.McpSessReg;
import mcp.server.foundation.server_process.client_context.session.McpSessRTOrch;
import mcp.server.foundation.server_process.client_context.session.event.McpSessEventPubl;
import mcp.server.foundation.server_process.client_context.session.event.SentinelNotificationSupport;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.server_process.client_context.session.persistence.service.McpSessRTStore;
import mcp.server.foundation.server_process.orchestration.McpRTOrch;
import mcp.server.foundation.server_process.orchestration.OperatingModelReg;
import mcp.server.foundation.server_process.orchestration.RTMcpSessModelReg;
import mcp.server.foundation.server_process.orchestration.RTWiring;
import mcp.server.foundation.server_process.orchestration.ServerLifecyStateStore;
import mcp.server.foundation.server_process.orchestration.ShutdownMan;
import mcp.server.foundation.server_process.orchestration.StartupMan;
import mcp.server.foundation.server_process.status.RTStatus;
import mcp.server.foundation.server_process.status.event.RTStatusEventPubl;
import mcp.server.foundation.tool_interface.ToolInvocEngine;
import mcp.server.foundation.transport.TranspAdap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringRTLifecyCfg {

  @Bean
  public ServerLifecyStateStore serverLifecyStateStore() {
    return new ServerLifecyStateStore(
        java.nio.file.Path.of("target", "runtime", "mcp-server-lifecycle-state.properties"));
  }

  @Bean
  public McpSessReg mcpSessReg() {
    return new McpSessReg();
  }

  @Bean
  public McpSessBindingReg mcpSessBindingReg() {
    return new McpSessBindingReg();
  }

  @Bean
  public SentinelNotificationSupport sentinelNotificationSupport(
      McpSessReg registry,
      McpSessBindingReg bindingRegistry,
      TranspAdap transport,
      RPCJsonSeria serializer,
      ServerLogger logger,
      RTMetrics runtimeMetrics,
      ObservCtxFactory obsCtxFactory) {

    return new SentinelNotificationSupport(
        registry,
        bindingRegistry,
        transport,
        serializer,
        logger,
        runtimeMetrics,
        obsCtxFactory);
  }

  @Bean
  public McpSessEventPubl mcpSessEventPublisher(
      SentinelNotificationSupport notificationSupport) {

    return new McpSessEventPubl(notificationSupport);
  }

  @Bean
  public McpSessRTOrch mcpSessRTOrch(
      McpSessReg registry,
      McpSessBindingReg bindingRegistry,
      McpSessEventPubl publisher,
      TranspAdap transportAdapter,
      ServerLogger logger,
      RTMetrics runtimeMetrics,
      McpTelemMetrics mcpTelemMetrics,
      ObservCtxFactory obsCtxFactory,
      McpSessRTMetaFactory runtimeMetaFactory,
      McpSessRTStore runtimeSessionStore) {

    return new McpSessRTOrch(
        registry,
        bindingRegistry,
        publisher,
        transportAdapter,
        logger,
        runtimeMetrics,
        mcpTelemMetrics,
        obsCtxFactory,
        runtimeMetaFactory,
        runtimeSessionStore);
  }

  @Bean
  public RPCRouter rpcRouter(
      McpSessReg sessionRegistry,
      ToolInvocEngine toolEngine,
      PromptReg promptReg,
      PromptService promptService,
      ResrcReg resourceReg,
      RPCCapaDscr descriptor,
      RPCJsonSeria rpcJsonSeria,
      McpSessRTOrch sessionRTOrch,
      ObservCtxFactory obsCtxFactory,
      ErrClassifier errorClassifier,
      ServerLogger serverLogger,
      McpObservationSupport mcpObservationSupport) {

    return new RPCRouter(
        sessionRTOrch,
        sessionRegistry,
        toolEngine,
        promptReg,
        promptService,
        resourceReg,
        descriptor,
        rpcJsonSeria,
        obsCtxFactory,
        errorClassifier,
        serverLogger,
        mcpObservationSupport);
  }

  @Bean
  public RTWiring runtimeWiring(
      TranspAdap transportAdapter,
      RPCJsonEntry rpcJsonEntrypoint,
      RPCRouter rpcRouter,
      ServerLogger logger,
      ObservCtxFactory obsCtxFactory,
      ErrClassifier errorClassifier,
      RTMetrics runtimeMetrics,
      McpTelemMetrics mcpTelemMetrics,
      @Value("${mcp.transport.inbound.max-in-flight:64}") int maxInboundInFlight) {

    return new RTWiring(
        transportAdapter,
        rpcJsonEntrypoint,
        rpcRouter,
        logger,
        obsCtxFactory,
        errorClassifier,
        runtimeMetrics,
        mcpTelemMetrics,
        maxInboundInFlight);
  }

  @Bean
  public RTStatus runtimeStatus() {
    return new RTStatus();
  }

  @Bean
  public RTStatusEventPubl runtimeStatusEventPubl(
      SentinelNotificationSupport notificationSupport) {

    return new RTStatusEventPubl(notificationSupport);
  }

  @Bean
  public StartupMan startupManager(
      TranspAdap transportAdapter,
      RTWiring wiring,
      McpSessRTOrch sessionRTOrch,
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      ServerLifecyStateStore lifecycleStateStore) {

    return new StartupMan(
        transportAdapter,
        wiring,
        sessionRTOrch,
        serverLogger,
        obsCtxFactory,
        runtimeMetrics,
        lifecycleStateStore);
  }

  @Bean
  public ShutdownMan shutdownManager(
      TranspAdap transportAdapter,
      McpSessRTOrch sessionRTOrch,
      ServerLogger serverLogger,
      RTMetrics runtimeMetrics,
      ServerLifecyStateStore lifecycleStateStore) {

    return new ShutdownMan(
        transportAdapter,
        sessionRTOrch,
        serverLogger,
        runtimeMetrics,
        lifecycleStateStore);
  }

  @Bean
  public McpRTOrch mcpRTOrch(
      RTStatus rtStatus,
      RTStatusEventPubl rtStatusEventPublisher,
      StartupMan startupManager,
      ShutdownMan shutdownManager,
      OperatingModelReg operatingModelRegistry,
      ReqsLifecyReg requestLifecycleRegistry,
      RTMcpSessModelReg runtimeSessionModelRegistry,
      ServerLogger serverLogger) {

    return new McpRTOrch(
        rtStatus,
        rtStatusEventPublisher,
        startupManager,
        shutdownManager,
        operatingModelRegistry,
        requestLifecycleRegistry,
        runtimeSessionModelRegistry,
        serverLogger);
  }
}
