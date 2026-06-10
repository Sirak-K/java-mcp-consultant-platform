package mcp.server.foundation.server_process.client_context.session.persistence.repository;

import mcp.server.foundation.server_process.client_context.session.persistence.entity.McpSessRTEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository for durable runtime-session persistence.
 */
public interface McpSessRTRepo extends JpaRepository<McpSessRTEntity, String> {

  Optional<McpSessRTEntity> findBySessionId(String sessionId);

  void deleteBySessionId(String sessionId);

  boolean existsBySessionId(String sessionId);
}
