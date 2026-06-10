package mcp.server.foundation.server_process.client_context.session.metadata;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * McpSessMetadataCtrl
 *
 * Observability endpoint.
 *
 * Exponerar:
 * - MCP Session ID (String)
 * - WS Connection ID (String, nullable)
 * - State (String)
 * - CreatedAt (Instant)
 *
 * Endast read-model.
 * Ingen lifecycle-logik.
 */
@RestController
public final class McpSessMetadataCtrl {

  private final McpSessMetadataFactory factory;

  public McpSessMetadataCtrl(
      McpSessMetadataFactory factory) {

    this.factory = Objects.requireNonNull(factory, "factory");
  }

  @GetMapping("/runtime/sessions")
  public List<McpSessMetadataView> SessMetaCtrlGetAll() {

    return factory.metadataView()
        .stream()
        .map(meta -> new McpSessMetadataView(
            meta.sessionId() == null ? null : meta.sessionId().toString(),
            meta.connectionId() == null ? null : meta.connectionId().toString(),
            meta.state() == null ? null : meta.state().name(),
            meta.createdAt(),
            meta.runtimeMeta() == null ? null : meta.runtimeMeta().McpSessRTMetaGetSessionType().RTMcpSessTypeGetId(),
            meta.runtimeMeta() == null ? null : meta.runtimeMeta().McpSessRTMetaGetSessionPhase().RTMcpSessPhaseGetId(),
            meta.runtimeMeta() == null ? null : meta.runtimeMeta().McpSessRTMetaGetSessionVersion(),
            meta.runtimeMeta() == null ? null : meta.runtimeMeta().McpSessRTMetaGetInactivityTtlSeconds(),
            meta.runtimeMeta() == null ? null : meta.runtimeMeta().resumeCapabilityId(),
            meta.runtimeMeta() == null ? null : meta.runtimeMeta().activeTenantId(),
            meta.runtimeMeta() == null ? null : meta.runtimeMeta().McpSessRTMetaIsDurableTarget(),
            meta.runtimeMeta() == null ? null : meta.runtimeMeta().McpSessRTMetaIsResumeSupported(),
            meta.runtimeMeta() == null ? null : meta.runtimeMeta().McpSessRTMetaRequiresActiveTenant()))
        .collect(Collectors.toList());
  }
}
