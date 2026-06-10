package mcp.server.domain.reference_data.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrimarySkillJpaRepo extends JpaRepository<PrimarySkillEntity, Long> {

    Optional<PrimarySkillEntity> findBySkillTitle(String skillTitle);
}
