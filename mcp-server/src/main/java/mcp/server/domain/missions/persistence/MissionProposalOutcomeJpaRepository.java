package mcp.server.domain.missions.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MissionProposalOutcomeJpaRepository
                extends JpaRepository<MissionProposalOutcomeEntity, Long> {

        Optional<MissionProposalOutcomeEntity> findByMissionProposalId(
                        Long missionProposalId);
}
