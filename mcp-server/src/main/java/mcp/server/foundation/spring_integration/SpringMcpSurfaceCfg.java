package mcp.server.foundation.spring_integration;

import mcp.server.foundation.control_plane.PlatformControlPlaneStatusView;
import mcp.server.foundation.control_plane.PlatformControlPlaneStore;
import mcp.server.foundation.server_process.orchestration.OperatingModelReg;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.server_process.orchestration.OperatingSurfaceContract;
import mcp.server.foundation.server_process.orchestration.RuntimeContractDescriptionCatalogService;
import mcp.server.foundation.prompt_interface.PromptLoader;
import mcp.server.foundation.prompt_interface.PromptReg;
import mcp.server.foundation.prompt_interface.PromptService;
import mcp.server.foundation.resource_interface.ResrcReg;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * MCP surface wiring — declares the registries and service beans required by
 * the MCP protocol surface (tools, resources, prompts) and the control plane.
 *
 * <p>
 * These beans are consumed by:
 * <ul>
 * <li>{@link SpringRTLifecyCfg} — takes {@link ResrcReg},
 * {@link PromptReg}, and {@link PromptService} as parameters.</li>
 * <li>{@link SpringMcpFoundToolCfg} — takes
 * {@link PlatformControlPlaneStore} as a parameter.</li>
 * <li>{@link SpringMcpResrcCfg} — takes {@link ResrcReg} and
 * {@link PlatformControlPlaneStore} to register resource providers.</li>
 * </ul>
 */

@Configuration
public class SpringMcpSurfaceCfg {

    @Bean
    public OperatingModelReg operatingModelRegistry(
            RuntimeContractDescriptionCatalogService descriptionCatalog) {
        return new OperatingModelReg(List.of(
                new OperatingSurfaceContract(
                        OperatingSurface.MCP_DIRECT,
                        descriptionCatalog.operatingSurfaceDescription(OperatingSurface.MCP_DIRECT),
                        true,
                        true,
                        false,
                        false,
                        descriptionCatalog.operatingSurfaceContractSummary(OperatingSurface.MCP_DIRECT)),
                new OperatingSurfaceContract(
                        OperatingSurface.APP_ADAPTER,
                        descriptionCatalog.operatingSurfaceDescription(OperatingSurface.APP_ADAPTER),
                        true,
                        true,
                        false,
                        false,
                        descriptionCatalog.operatingSurfaceContractSummary(OperatingSurface.APP_ADAPTER)),
                new OperatingSurfaceContract(
                        OperatingSurface.PLATFORM_OPS,
                        descriptionCatalog.operatingSurfaceDescription(OperatingSurface.PLATFORM_OPS),
                        true,
                        true,
                        false,
                        true,
                        descriptionCatalog.operatingSurfaceContractSummary(OperatingSurface.PLATFORM_OPS))));
    }

    @Bean
    public ResrcReg resrcReg() {
        return new ResrcReg();
    }

    @Bean
    public PromptReg promptReg() {
        return new PromptReg();
    }

    @Bean
    public PromptLoader promptLoader() {
        return new PromptLoader();
    }

    @Bean
    public PromptService promptService(PromptReg promptReg, PromptLoader promptLoader) {
        return new PromptService(promptReg, promptLoader);
    }

    @Bean
    public PlatformControlPlaneStore platformControlPlaneStore() {
        return new PlatformControlPlaneStore(
                new PlatformControlPlaneStatusView(true, Map.of()));
    }
}
