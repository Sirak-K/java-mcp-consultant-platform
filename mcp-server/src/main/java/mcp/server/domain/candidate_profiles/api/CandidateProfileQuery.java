package mcp.server.domain.candidate_profiles.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CandidateProfileQuery {

    long countRegisteredCandidateProfiles();

    List<CandidateProfileMatchingView> matchableCandidateProfiles();

    Optional<CandidateProfileMatchingView> findMatchingProfile(long candidateProfileId);

    Map<Long, CandidateProfileCardView> candidateProfileCardsById(Collection<Long> candidateProfileIds);

    Optional<CandidateProfileEvidence> findEvidenceProfile(long candidateProfileId);

    CandidateProfileAssignmentEligibility evaluateAssignmentEligibility(
            CandidateProfileAssignmentRequirement requirement);
}
