package mcp.server.domain.missions.persistence;

import java.util.List;

import mcp.server.domain.missions.model.MissionSlotId;
import mcp.server.domain.missions.model.MissionSlotRequiredSkill;
import mcp.server.domain.missions.model.MissionSlotRequiredSkillId;
import mcp.server.domain.reference_data.model.SkillId;

public interface MissionSlotRequiredSkillRepository {

    MissionSlotRequiredSkill save(MissionSlotRequiredSkill skill);

    List<MissionSlotRequiredSkill> findByMissionSlotId(MissionSlotId missionSlotId);

    void deleteById(MissionSlotRequiredSkillId id);

    int countByMissionSlotId(MissionSlotId missionSlotId);

    int countBySkillId(SkillId skillId);
}
