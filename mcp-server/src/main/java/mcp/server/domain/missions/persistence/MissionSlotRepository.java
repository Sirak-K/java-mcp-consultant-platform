package mcp.server.domain.missions.persistence;

import java.util.List;
import java.util.Optional;

import mcp.server.domain.missions.model.MissionSlot;
import mcp.server.domain.missions.model.MissionSlotId;
import mcp.server.domain.reference_data.model.RoleId;
import mcp.server.domain.missions.model.MissionId;

public interface MissionSlotRepository {

    MissionSlot save(MissionSlot missionSlot);

    Optional<MissionSlot> findById(MissionSlotId id);

    List<MissionSlot> findByMission(MissionId missionId);

    void delete(MissionSlotId id);

    boolean existsById(MissionSlotId id);

    int countByMission(MissionId missionId);

    int countByRoleId(RoleId roleId);

    int countByFilledByCandidateProfileId(long candidateProfileId);

    List<MissionSlot> findByRoleIdAndNotFilled(RoleId roleId);
}
