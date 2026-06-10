package mcp.server.domain.missions.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionSlotRequiredSkillJpaRepository extends JpaRepository<MissionSlotRequiredSkillEntity, Long> {

    List<MissionSlotRequiredSkillEntity> findByMissionSlotId(Long missionSlotId);

    int countByMissionSlotId(Long missionSlotId);

    int countBySkillId(Long skillId);
}
