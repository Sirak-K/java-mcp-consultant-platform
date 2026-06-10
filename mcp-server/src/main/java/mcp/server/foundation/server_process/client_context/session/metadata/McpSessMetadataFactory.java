package mcp.server.foundation.server_process.client_context.session.metadata;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import mcp.server.foundation.server_process.client_context.session.McpSessBindingReg;
import mcp.server.foundation.server_process.client_context.session.McpSessReg;
import mcp.server.foundation.server_process.client_context.session.id.McpSessId;
import mcp.server.foundation.transport.websocket.WsConnId;

@Component
public final class McpSessMetadataFactory {

  private final McpSessReg registry;
  private final McpSessBindingReg bindingRegistry;

  public McpSessMetadataFactory(
      McpSessReg registry,
      McpSessBindingReg bindingRegistry) {

    this.registry = Objects.requireNonNull(registry, "registry");
    this.bindingRegistry = Objects.requireNonNull(bindingRegistry, "bindingRegistry");
  }

  public List<McpSessMetadata> metadataView() {

    return registry.SessRegGetAllMetadata()
        .stream()
        .map(meta -> {

          McpSessId sessionId = meta.sessionId();

          // Hard rule:
          // TranspLost får aldrig exponera en connectionId i view, även om
          // binding-registry råkar ha stale mapping kvar.
          WsConnId connectionId = null;
          if (sessionId != null && !registry.SessRegIsTranspLost(sessionId)) {
            connectionId = bindingRegistry.getConnId(sessionId);
          }

          return new McpSessMetadata(
              sessionId,
              connectionId,
              meta.state(),
              meta.createdAt(),
              meta.runtimeMeta());
        })
        .collect(Collectors.toList());
  }

}
