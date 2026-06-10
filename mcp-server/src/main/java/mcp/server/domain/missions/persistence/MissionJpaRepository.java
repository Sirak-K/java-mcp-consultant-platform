package mcp.server.domain.missions.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissionJpaRepository extends JpaRepository<MissionEntity, Long> {

    List<MissionEntity> findByMissionAvailability(String missionAvailability);

    List<MissionEntity> findAllByOrderByIdDesc();

    Optional<MissionEntity> findBySourceMissionProposalId(Long sourceMissionProposalId);
}
