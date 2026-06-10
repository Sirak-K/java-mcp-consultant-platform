package mcp.server.domain.matching.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CandidateToSlotMatchQuery {

    Optional<CandidateToSlotMatchEvidence> findMatchEvidence(long matchId);

    Optional<CandidateMissionMatchEvidenceGroup> findCandidateMissionMatchEvidenceGroup(long matchId);

    List<CandidateMissionMatchEvidenceGroup> findRecentCandidateMissionMatchEvidenceGroups();

    Optional<Long> findMatchId(long candidateProfileId, long missionSlotId);

    List<Long> findMatchIdsForCandidateAndMissionSlots(
            long candidateProfileId,
            Collection<Long> missionSlotIds);

    MatchScoreBreakdownView inspectScoreBreakdown(long matchId);
}
