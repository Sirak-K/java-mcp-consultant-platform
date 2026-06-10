package mcp.server.domain.missions.persistence;

import org.springframework.data.jpa.repository.JpaRepository;


public interface MissionProposalRequiredSkillJpaRepository
        extends JpaRepository<MissionProposalRequiredSkillEntity, Long> {
}
