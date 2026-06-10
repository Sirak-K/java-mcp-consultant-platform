package mcp.server.foundation.server_process.client_context.session.id;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class McpSessIdGen {

  public McpSessId generate() {
    return McpSessId.of(UUID.randomUUID());
  }
}
