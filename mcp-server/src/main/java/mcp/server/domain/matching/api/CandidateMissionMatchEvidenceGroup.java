package mcp.server.domain.matching.api;

import java.time.Instant;
import java.util.List;

public record CandidateMissionMatchEvidenceGroup(
        CandidateToSlotMatchEvidence primaryMatch,
        long missionId,
        String deliveryGroupKey,
        Instant discoveryCreatedAt,
        List<CandidateToSlotMatchEvidence> matches) {

    public CandidateMissionMatchEvidenceGroup {
        if (primaryMatch == null) {
            throw new IllegalArgumentException("primaryMatch cannot be null");
        }
        matches = matches == null || matches.isEmpty()
                ? List.of(primaryMatch)
                : List.copyOf(matches);
    }
}
