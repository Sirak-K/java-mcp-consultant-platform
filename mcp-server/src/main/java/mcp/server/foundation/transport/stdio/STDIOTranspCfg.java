package mcp.server.foundation.transport.stdio;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.server_process.client_context.session.id.McpSessIdGen;
import mcp.server.foundation.server_process.client_context.session.metadata.McpSessRTMetaFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import mcp.server.foundation.transport.TranspActivationCondition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.io.PrintStream;

@Configuration
@Conditional(TranspActivationCondition.StdioCondition.class)
public class STDIOTranspCfg {

  @Bean
  @ConditionalOnMissingBean(name = "stdioInputStream")
  public InputStream stdioInputStream() {
    return System.in;
  }

  @Bean
  @ConditionalOnMissingBean(name = "stdioOutputStream")
  public PrintStream stdioOutputStream() {
    return System.out;
  }

  @Bean
  public STDIOTranspChannels stdioTranspChannels(
      InputStream stdioInputStream,
      PrintStream stdioOutputStream) {

    return new STDIOTranspChannels(stdioInputStream, stdioOutputStream);
  }

  @Bean
  public STDIORTOrch stdioRTOrch(
      STDIOTranspChannels stdioTranspChannels,
      McpSessIdGen mcpSessIdGenerator,
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics,
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      McpSessRTMetaFactory runtimeMetaFactory) {

    return new STDIORTOrch(
        stdioTranspChannels.inputStream(),
        stdioTranspChannels.outputStream(),
        mcpSessIdGenerator,
        serverLogger,
        obsCtxFactory,
        runtimeMetrics,
        telemetryMetrics,
        requestAuthBindingPolicy,
        runtimeMetaFactory);
  }

  @Bean
  public STDIOTranspAdap stdioTranspAdap(STDIORTOrch stdioRTOrch) {
    return new STDIOTranspAdap(stdioRTOrch);
  }
}
