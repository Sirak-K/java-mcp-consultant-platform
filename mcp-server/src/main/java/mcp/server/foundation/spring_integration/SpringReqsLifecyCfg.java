package mcp.server.foundation.spring_integration;

import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import mcp.server.foundation.security.request_binding.ReqsBindingComplianceGuard;
import mcp.server.foundation.security.request_binding.ReqsBindingStage;
import mcp.server.foundation.security.request_binding.ReqsLifecyContract;
import mcp.server.foundation.security.request_binding.ReqsLifecyReg;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.orchestration.RuntimeContractDescriptions;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringReqsLifecyCfg {

  @Bean
  public ReqsLifecyReg requestLifecycleRegistry() {
    return new ReqsLifecyReg(List.of(
        new ReqsLifecyContract(
            OperatingSurface.MCP_DIRECT,
            true,
            false,
            false,
            ReqsBindingStage.PLATFORM_BOUND,
            RuntimeContractDescriptions.requestLifecycleSummary(OperatingSurface.MCP_DIRECT)),
        new ReqsLifecyContract(
            OperatingSurface.APP_ADAPTER,
            true,
            true,
            false,
            ReqsBindingStage.PRE_SESSION,
            RuntimeContractDescriptions.requestLifecycleSummary(OperatingSurface.APP_ADAPTER)),
        new ReqsLifecyContract(
            OperatingSurface.PLATFORM_OPS,
            true,
            false,
            true,
            ReqsBindingStage.PLATFORM_BOUND,
            RuntimeContractDescriptions.requestLifecycleSummary(OperatingSurface.PLATFORM_OPS))));
  }

  @Bean
  public ReqsAuthBindingPolicy requestAuthBindingPolicy(
      ReqsLifecyReg requestLifecycleRegistry) {

    return new ReqsAuthBindingPolicy(requestLifecycleRegistry);
  }

  @Bean
  public ReqsBindingComplianceGuard requestBindingComplianceGuard(
      ReqsLifecyReg requestLifecycleRegistry) {

    return new ReqsBindingComplianceGuard(requestLifecycleRegistry);
  }
}
