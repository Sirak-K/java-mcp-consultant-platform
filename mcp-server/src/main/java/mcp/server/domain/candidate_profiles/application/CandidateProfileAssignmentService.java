package mcp.server.domain.candidate_profiles.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentCommand;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileJpaRepo;

@Service
public class CandidateProfileAssignmentService implements CandidateProfileAssignmentCommand {

    private static final int MAX_ACTIVE_ASSIGNMENTS = 5;
    private static final String WORK_STATUS_UNAVAILABLE = "UNAVAILABLE";

    private final CandidateProfileJpaRepo candidateProfileRepo;

    public CandidateProfileAssignmentService(CandidateProfileJpaRepo candidateProfileRepo) {
        this.candidateProfileRepo = candidateProfileRepo;
    }

    @Override
    @Transactional
    public void markUnavailableWhenAssignmentLimitExceeded(long candidateProfileId, int activeAssignmentCount) {
        if (candidateProfileId <= 0 || activeAssignmentCount <= MAX_ACTIVE_ASSIGNMENTS) {
            return;
        }

        CandidateProfileEntity candidateProfile = candidateProfileRepo.findById(candidateProfileId)
                .orElseThrow(() -> new IllegalArgumentException("candidateProfile not found: " + candidateProfileId));
        if (WORK_STATUS_UNAVAILABLE.equalsIgnoreCase(candidateProfile.getWorkStatus())) {
            return;
        }

        candidateProfile.setWorkStatus(WORK_STATUS_UNAVAILABLE);
        candidateProfileRepo.save(candidateProfile);
    }
}
