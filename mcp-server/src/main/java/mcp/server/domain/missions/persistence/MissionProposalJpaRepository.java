package mcp.server.domain.missions.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissionProposalJpaRepository extends JpaRepository<MissionProposalEntity, Long> {

    List<MissionProposalEntity> findAllByOrderByCreatedAtDesc();

    List<MissionProposalEntity> findAllByStatusOrderByCreatedAtDesc(String status);

    Optional<MissionProposalEntity> findByMissionSlots_Id(Long missionSlotId);
}
