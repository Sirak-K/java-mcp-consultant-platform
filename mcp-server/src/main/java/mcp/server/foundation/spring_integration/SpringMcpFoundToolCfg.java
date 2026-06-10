package mcp.server.foundation.spring_integration;

import mcp.server.foundation.control_plane.PlatformControlPlaneStore;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.tracing.McpObservationSupport;
import mcp.server.foundation.rpc.RPCJsonSeria;
import mcp.server.foundation.rpc.error.ErrClassifier;
import mcp.server.foundation.security.request_binding.ReqsBindingComplianceGuard;
import mcp.server.foundation.tool_interface.ToolDefinition;
import mcp.server.foundation.tool_interface.ToolExecPolicy;
import mcp.server.foundation.tool_interface.ToolExecProperties;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolInvocEngine;
import mcp.server.foundation.tool_interface.ToolProgrPubl;
import mcp.server.foundation.tool_interface.ToolReg;
import mcp.server.foundation.tool_interface.OpsHealthcheckTool;
import mcp.server.foundation.tool_interface.TranspToolProgrPubl;
import mcp.server.foundation.transport.TranspAdap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foundation-level tool wiring.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Declares {@link ToolReg} as a singleton Spring bean.</li>
 *   <li>Declares {@link ToolInvocEngine} as a Spring bean with full observability wiring.</li>
 *   <li>Registers the only foundation tool: {@code ops.healthcheck}.</li>
 *   <li>Enables {@link ToolExecProperties} binding from application config.</li>
 * </ul>
 *
 * <p>Domain tools are not registered here; they belong in
 * {@link SpringMcpDomainToolCfg}.
 */
@Configuration
@EnableConfigurationProperties(ToolExecProperties.class)
public class SpringMcpFoundToolCfg {

    @Bean
    public ToolReg toolReg() {
        return new ToolReg();
    }

    @Bean
    public ToolInvocEngine toolInvocEngine(
            OpsHealthcheckTool opsHealthcheckTool,
            ToolReg toolReg,
            ToolExecProperties toolExecProperties,
            ErrClassifier errorClassifier,
            ServerLogger serverLogger,
            TranspAdap transpAdap,
            RPCJsonSeria rpcJsonSeria,
            McpObservationSupport mcpObservationSupport,
            McpTelemMetrics mcpTelemMetrics,
            RTMetrics runtimeMetrics,
            PlatformControlPlaneStore platformControlPlaneStore,
            ReqsBindingComplianceGuard requestBindingComplianceGuard) {

        registerFoundationToolGroup(toolReg, toolExecProperties, "ops", opsHealthcheckTool);

        int concurrency = toolExecProperties.getGlobalMaxConcurr();
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        ToolProgrPubl progressPublisher = new TranspToolProgrPubl(
                transpAdap, rpcJsonSeria, serverLogger);

        return new ToolInvocEngine(
                toolReg,
                executor,
                concurrency,
                errorClassifier,
                serverLogger,
                progressPublisher,
                mcpObservationSupport,
                mcpTelemMetrics,
                runtimeMetrics,
                platformControlPlaneStore,
                requestBindingComplianceGuard);
    }

    private void registerFoundationToolGroup(
            ToolReg toolReg,
            ToolExecProperties toolExecProperties,
            String groupName,
            ToolInterface... tools) {

        Objects.requireNonNull(groupName, "groupName");
        if (groupName.isBlank()) {
            throw new IllegalArgumentException("Foundation tool group name cannot be blank");
        }
        if (tools == null || tools.length == 0) {
            throw new IllegalStateException("Foundation tool group has no tools: " + groupName);
        }
        for (ToolInterface tool : tools) {
            registerFoundationTool(toolReg, toolExecProperties, groupName, tool);
        }
    }

    private void registerFoundationTool(
            ToolReg toolReg,
            ToolExecProperties toolExecProperties,
            String groupName,
            ToolInterface tool) {

        ToolReg requiredToolReg = Objects.requireNonNull(toolReg, "toolReg");
        ToolExecProperties requiredToolExecProperties =
                Objects.requireNonNull(toolExecProperties, "toolExecProperties");
        ToolInterface requiredTool = Objects.requireNonNull(tool, groupName + " tool");
        ToolExecPolicy policy = requiredToolExecProperties.ToolExecPropertiesResolve(requiredTool.getName());
        requiredToolReg.ToolRegRegister(ToolDefinition.ToolDefFromToolInterface(requiredTool, policy));
    }
}
