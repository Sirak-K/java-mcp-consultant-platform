package mcp.server.domain.missions.web;

import java.util.List;

import mcp.server.domain.matching.api.CandidateMatchDiscoveryResult;
import mcp.server.domain.missions.application.MissionProposalReview;

public final class MissionProposalReviewWebContract {

    private MissionProposalReviewWebContract() {
    }

    public record ProposalEditInput(
            String customerName,
            String customerEmail,
            String missionTitle,
            List<MissionSpecificationWebContract.SlotInput> missionSlots,
            String startDate,
            String endDate,
            String workMode,
            MissionSpecificationWebContract.PresentationInput missionPresentation,
            String outcome) {
    }

    public record ProposalView(
            long id,
            String status,
            MissionSpecificationWebContract.SpecificationView specification,
            List<CandidateMatchDiscoveryResult> findCandidateResults,
            String outcome,
            String createdAt,
            String updatedAt) {
    }

    public static MissionProposalReview.ProposalEditInput toApplication(ProposalEditInput input) {
        if (input == null) {
            return null;
        }
        return new MissionProposalReview.ProposalEditInput(
                input.customerName(),
                input.customerEmail(),
                input.missionTitle(),
                input.missionSlots() == null
                        ? null
                        : input.missionSlots().stream()
                                .map(MissionSpecificationWebContract::toApplication)
                                .toList(),
                input.startDate(),
                input.endDate(),
                input.workMode(),
                MissionSpecificationWebContract.toApplication(input.missionPresentation()),
                input.outcome());
    }

    public static ProposalView fromApplication(MissionProposalReview.ProposalView view) {
        if (view == null) {
            return null;
        }
        return new ProposalView(
                view.id(),
                view.status(),
                MissionSpecificationWebContract.fromApplication(view.specification()),
                view.findCandidateResults(),
                view.outcome(),
                view.createdAt(),
                view.updatedAt());
    }
}
