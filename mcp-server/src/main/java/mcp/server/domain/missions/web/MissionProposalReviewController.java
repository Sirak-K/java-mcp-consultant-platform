package mcp.server.domain.missions.web;

import java.util.List;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import mcp.server.domain.missions.application.MissionProposalReviewService;

@RestController
public final class MissionProposalReviewController {

  private final MissionProposalReviewService missionProposalReviewService;

  public MissionProposalReviewController(MissionProposalReviewService missionProposalReviewService) {
    this.missionProposalReviewService = Objects.requireNonNull(
        missionProposalReviewService,
        "missionProposalReviewService");
  }

  @GetMapping("/api/ops/mission-proposals")
  public List<MissionProposalReviewWebContract.ProposalView> missionProposalsForReview() {
    return missionProposalReviewService.missionProposalsForReview().stream()
        .map(MissionProposalReviewWebContract::fromApplication)
        .toList();
  }

  @PutMapping("/api/ops/mission-proposals/{id}")
  public MissionProposalReviewWebContract.ProposalView editMissionProposal(
      @PathVariable("id") long id,
      @RequestBody MissionProposalReviewWebContract.ProposalEditInput request) {
    return MissionProposalReviewWebContract.fromApplication(
        missionProposalReviewService.editMissionProposal(
            id,
            MissionProposalReviewWebContract.toApplication(request)));
  }

  @PutMapping("/api/ops/mission-proposals/{id}/approve")
  public MissionProposalReviewWebContract.ProposalView approveMissionProposal(
      @PathVariable("id") long id) {
    return MissionProposalReviewWebContract.fromApplication(
        missionProposalReviewService.approveMissionProposal(id));
  }

  @PutMapping("/api/ops/mission-proposals/{id}/reject")
  public MissionProposalReviewWebContract.ProposalView rejectMissionProposal(
      @PathVariable("id") long id) {
    return MissionProposalReviewWebContract.fromApplication(
        missionProposalReviewService.rejectMissionProposal(id));
  }
}
