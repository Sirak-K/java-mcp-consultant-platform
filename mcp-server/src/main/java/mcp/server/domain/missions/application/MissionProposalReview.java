package mcp.server.domain.missions.application;

import java.util.List;

import mcp.server.domain.matching.api.CandidateMatchDiscoveryResult;

public final class MissionProposalReview {

    private MissionProposalReview() {
    }

    public record ProposalEditInput(
            String customerName,
            String customerEmail,
            String missionTitle,
            List<MissionSpecification.SlotInput> missionSlots,
            String startDate,
            String endDate,
            String workMode,
            MissionSpecification.PresentationInput missionPresentation,
            String outcome) {
    }

    public record ProposalView(
            long id,
            String status,
            MissionSpecification.SpecificationView specification,
            List<CandidateMatchDiscoveryResult> findCandidateResults,
            String outcome,
            String createdAt,
            String updatedAt) {
    }
}
