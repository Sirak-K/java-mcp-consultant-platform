package mcp.server.domain.missions.persistence;

import java.util.List;
import java.util.Optional;

import mcp.server.domain.missions.model.Mission;
import mcp.server.domain.missions.model.MissionAvailability;
import mcp.server.domain.missions.model.MissionId;

public interface MissionRepository {

    Mission save(Mission mission);

    Optional<Mission> findById(MissionId id);

    List<Mission> findByAvailability(MissionAvailability availability);

    List<Mission> findAll();

    void delete(MissionId id);

    boolean existsById(MissionId id);
}
