package mcp.server.domain.reference_data.persistence;

import org.springframework.data.jpa.repository.JpaRepository;


public interface RoleJpaRepo extends JpaRepository<RoleEntity, Long> {
}
