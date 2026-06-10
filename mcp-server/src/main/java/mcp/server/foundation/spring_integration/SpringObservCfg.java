package mcp.server.foundation.spring_integration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import mcp.server.foundation.logging.CanonicalLogPaths;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.diagnostics.RTDiagnService;
import mcp.server.foundation.observability.diagnostics.RuntimeSessionDiagnosticsService;
import mcp.server.foundation.observability.health.RTHealthService;
import mcp.server.foundation.observability.metrics.McpMetricCatal;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.persistence.McpPersistenceTelem;
import mcp.server.foundation.observability.readiness.RTReadinessService;
import mcp.server.foundation.observability.runtime.RTVisibilityLogRequirementPolicy;
import mcp.server.foundation.observability.runtime.RTVisibilityService;
import mcp.server.foundation.observability.tracing.McpObservationSupport;
import mcp.server.foundation.observability.triage.RTTriageService;
import mcp.server.foundation.observability.triage.RuntimeTriageSymptomCatalogService;
import mcp.server.foundation.rpc.error.ErrClassifier;
import mcp.server.foundation.server_process.client_context.session.McpSessBindingReg;
import mcp.server.foundation.server_process.client_context.session.McpSessReg;
import mcp.server.foundation.server_process.client_context.session.persistence.repository.McpSessRTRepo;
import mcp.server.foundation.server_process.status.RTStatus;
import mcp.server.foundation.tool_interface.ToolInvocEngine;
import mcp.server.foundation.tool_interface.ToolReg;
import mcp.server.foundation.transport.TranspAdap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.time.Duration;

import javax.sql.DataSource;

@Configuration
public class SpringObservCfg {

  @Bean
  public ObservCtxFactory obsCtxFactory() {
    return new ObservCtxFactory();
  }

  @Bean
  public McpObservationSupport mcpObservationSupport(ObservationRegistry observationRegistry) {
    return new McpObservationSupport(observationRegistry);
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(prefix = "mcp.observability.otlp-log-sink", name = "enabled", havingValue = "true")
  public SdkLoggerProvider otlpSdkLoggerProvider(
      Environment environment,
      @Value("${spring.application.name:mcp-server}") String applicationName,
      @Value("${mcp.observability.otlp-log-sink.endpoint:http://localhost:4318/v1/logs}") String endpoint,
      @Value("${mcp.observability.otlp-log-sink.timeout:5s}") Duration timeout,
      @Value("${mcp.observability.otlp-log-sink.compression:none}") String compression) {

    var exporterBuilder = OtlpHttpLogRecordExporter.builder()
        .setEndpoint(endpoint)
        .setTimeout(timeout);
    if (compression != null && !compression.isBlank() && !"none".equalsIgnoreCase(compression)) {
      exporterBuilder.setCompression(compression);
    }

    Resource resource = Resource.getDefault().merge(Resource.create(
        Attributes.builder()
            .put("service.name", applicationName)
            .put("deployment.environment", deploymentEnvironment(environment))
            .build()));

    return SdkLoggerProvider.builder()
        .setResource(resource)
        .addLogRecordProcessor(BatchLogRecordProcessor.builder(exporterBuilder.build()).build())
        .build();
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(prefix = "mcp.observability.tracing.otlp", name = "enabled", havingValue = "true")
  public SpanExporter otlpSpanExporter(
      @Value("${mcp.observability.tracing.otlp.endpoint:http://localhost:4318/v1/traces}") String endpoint,
      @Value("${mcp.observability.tracing.otlp.timeout:5s}") Duration timeout,
      @Value("${mcp.observability.tracing.otlp.compression:none}") String compression) {

    var builder = OtlpHttpSpanExporter.builder()
        .setEndpoint(endpoint)
        .setTimeout(timeout);
    if (compression != null && !compression.isBlank() && !"none".equalsIgnoreCase(compression)) {
      builder.setCompression(compression);
    }
    return builder.build();
  }

  @Bean
  public ErrClassifier errorClassifier() {
    return new ErrClassifier();
  }

  @Bean
  public McpTelemMetrics mcpTelemMetrics(MeterRegistry meterRegistry) {
    return new McpTelemMetrics(meterRegistry);
  }

  @Bean
  public RTMetrics runtimeMetrics(MeterRegistry meterRegistry) {
    return new RTMetrics(meterRegistry);
  }

  @Bean
  public Object operationalStateGauges(
      MeterRegistry meterRegistry,
      RTHealthService runtimeHealthService,
      McpSessReg sessionRegistry,
      McpSessBindingReg bindingRegistry) {

    meterRegistry.gauge(
        McpMetricCatal.MCP_RUNTIME_READINESS_STATE,
        runtimeHealthService,
        service -> service.RTHealthSvcIsReady() ? 1.0d : 0.0d);
    meterRegistry.gauge(
        McpMetricCatal.MCP_RUNTIME_LIVENESS_STATE,
        runtimeHealthService,
        service -> service.RTHealthSvcGetLiveness().live() ? 1.0d : 0.0d);
    meterRegistry.gauge(
        McpMetricCatal.MCP_SESSIONS_LOGICAL_ACTIVE,
        sessionRegistry,
        registry -> (double) registry.SessRegGetActiveSessCount());
    meterRegistry.gauge(
        McpMetricCatal.MCP_BINDINGS_ACTIVE,
        bindingRegistry,
        registry -> (double) registry.getActiveBindingCount());
    meterRegistry.gauge(
        McpMetricCatal.MCP_SENTINEL_SUBSCRIBERS_ACTIVE,
        sessionRegistry,
        registry -> (double) registry.SessRegGetSentinelMcpSessIds().size());
    return new Object();
  }

  @Bean
  public McpPersistenceTelem mcpPersistenceTelemetry(
      McpTelemMetrics mcpTelemMetrics,
      McpObservationSupport mcpObservationSupport,
      ServerLogger serverLogger,
      ErrClassifier errorClassifier,
      RTMetrics runtimeMetrics,
      ObservCtxFactory obsCtxFactory) {

    return new McpPersistenceTelem(
        mcpTelemMetrics,
        mcpObservationSupport,
        serverLogger,
        errorClassifier,
        runtimeMetrics,
        obsCtxFactory);
  }

  @Bean
  public RTHealthService runtimeHealthService(
      RTStatus runtimeStatus,
      TranspAdap transportAdapter,
      ObjectProvider<DataSource> dataSourceProvider) {

    return new RTHealthService(
        runtimeStatus,
        transportAdapter,
        dataSourceProvider.getIfAvailable());
  }

  @Bean
  public RTVisibilityService runtimeVisibilityService(
      RTMetrics runtimeMetrics,
      McpTelemMetrics mcpTelemMetrics,
      RTStatus runtimeStatus,
      TranspAdap transportAdapter,
      McpSessReg sessionRegistry,
      McpSessBindingReg bindingRegistry,
      RTHealthService runtimeHealthService,
      ToolInvocEngine toolInvocationEngine,
      CanonicalLogPaths canonicalLogPaths,
      Environment environment,
      @Value("${mcp.observability.file-sink.enabled:true}") boolean fileSinkEnabled,
      @Value("${mcp.observability.file-sink.all-path:logs/mcp-server.log}") String allPath,
      @Value("${mcp.observability.file-sink.error-path:logs/mcp-server-errors.log}") String errorPath,
      @Value("${mcp.observability.audit-sink.enabled:false}") boolean auditSinkEnabled,
      @Value("${mcp.observability.audit-sink.path:logs/mcp-server-audit.log}") String auditPath,
      @Value("${mcp.observability.test-sink.enabled:false}") boolean testSinkEnabled,
      @Value("${mcp.observability.test-sink.path:logs/mcp-server.log}") String testPath) {

    RTVisibilityLogRequirementPolicy logRequirementPolicy =
        RTVisibilityLogRequirementPolicy.RTVisibilityLogRequirementPolicyResolve(
            environment.acceptsProfiles(Profiles.of("dev")),
            environment.acceptsProfiles(Profiles.of("prod")),
            environment.acceptsProfiles(Profiles.of("test")),
            auditSinkEnabled,
            testSinkEnabled);

    return new RTVisibilityService(
        runtimeMetrics,
        mcpTelemMetrics,
        runtimeStatus,
        transportAdapter,
        sessionRegistry,
        bindingRegistry,
        runtimeHealthService,
        toolInvocationEngine,
        canonicalLogPaths,
        fileSinkEnabled,
        allPath,
        errorPath,
        auditSinkEnabled,
        auditPath,
        testSinkEnabled,
        testPath,
        logRequirementPolicy.fileSinkRequired(),
        logRequirementPolicy.auditSinkRequired(),
        logRequirementPolicy.testSinkRequired());
  }

  @Bean
  public RTTriageService runtimeTriageService(
      RTHealthService runtimeHealthService,
      RTVisibilityService runtimeVisibilityService,
      RuntimeTriageSymptomCatalogService symptomCatalogService) {

    return new RTTriageService(runtimeHealthService, runtimeVisibilityService, symptomCatalogService);
  }

  @Bean
  public RTDiagnService runtimeDiagnosticsService(
      RTVisibilityService runtimeVisibilityService,
      McpSessReg sessionRegistry,
      McpSessBindingReg bindingRegistry,
      ToolReg toolRegistry,
      ToolInvocEngine toolInvocationEngine) {

    return new RTDiagnService(
        runtimeVisibilityService,
        sessionRegistry,
        bindingRegistry,
        toolRegistry,
        toolInvocationEngine);
  }

  @Bean
  public RuntimeSessionDiagnosticsService runtimeSessionDiagnosticsService(
      McpSessRTRepo runtimeSessionRepository) {

    return new RuntimeSessionDiagnosticsService(runtimeSessionRepository);
  }

  @Bean
  public RTReadinessService runtimeReadinessService(
      RTHealthService runtimeHealthService,
      RTVisibilityService runtimeVisibilityService,
      RTTriageService runtimeTriageService,
      RTDiagnService runtimeDiagnosticsService) {

    return new RTReadinessService(
        runtimeHealthService,
        runtimeVisibilityService,
        runtimeTriageService,
        runtimeDiagnosticsService);
  }

  private static String deploymentEnvironment(Environment environment) {
    if (environment.acceptsProfiles(Profiles.of("prod"))) {
      return "prod";
    }
    if (environment.acceptsProfiles(Profiles.of("test"))) {
      return "test";
    }
    if (environment.acceptsProfiles(Profiles.of("dev"))) {
      return "dev";
    }
    return "default";
  }
}
