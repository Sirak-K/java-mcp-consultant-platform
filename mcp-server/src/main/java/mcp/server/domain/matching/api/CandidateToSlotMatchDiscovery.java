package mcp.server.domain.matching.api;

import java.util.List;
import java.util.Map;

import mcp.server.domain.missions.application.MissionSpecification;

public interface CandidateToSlotMatchDiscovery {

    List<CandidateMatchDiscoveryResult> findCandidatesForMission(
            MissionSpecification.SpecificationView specification);

    List<CandidateMatchDiscoveryResult> findCandidatesForMission(
            MissionSpecification.SpecificationView specification,
            Map<Integer, Long> missionSlotIdsByNumber);

    List<MissionMatchDiscoveryResult> findMissionsForCandidate(long candidateProfileId);
}
