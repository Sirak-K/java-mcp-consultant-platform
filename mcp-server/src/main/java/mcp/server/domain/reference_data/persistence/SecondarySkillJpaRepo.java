package mcp.server.domain.reference_data.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecondarySkillJpaRepo extends JpaRepository<SecondarySkillEntity, Long> {

    Optional<SecondarySkillEntity> findBySkillTitle(String skillTitle);
}
