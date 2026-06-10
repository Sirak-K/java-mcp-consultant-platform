package mcp.server.domain.missions.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionSlotJpaRepository extends JpaRepository<MissionSlotEntity, Long> {

    List<MissionSlotEntity> findByMissionId(Long missionId);

    List<MissionSlotEntity> findAllByMissionIdIn(List<Long> missionIds);

    int countByMissionId(Long missionId);

    int countByRoleId(Long roleId);

    int countByMissionSlotFilledByProfileId(Long candidateProfileId);

    List<MissionSlotEntity> findByRoleIdAndMissionSlotFillStatus(Long roleId, String fillStatus);
}
