package mcp.server.domain.matching.api;

import java.util.Collection;

public interface CandidateToSlotMatchCleanup {

    int removeMatchesForCandidateProfile(long candidateProfileId);

    void removeMatchesForMissionSlots(Collection<Long> missionSlotIds);
}
