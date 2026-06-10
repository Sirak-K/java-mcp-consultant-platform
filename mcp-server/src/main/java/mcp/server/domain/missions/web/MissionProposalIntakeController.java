package mcp.server.domain.missions.web;

import java.util.Objects;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import mcp.server.domain.missions.application.MissionProposalSubmissionService;
import mcp.server.domain.missions.application.intake.MissionProposalWorkingCopyService;

@RestController
public final class MissionProposalIntakeController {

  private final MissionProposalSubmissionService missionProposalSubmissionService;
  private final MissionProposalWorkingCopyService missionProposalWorkingCopyService;

  public MissionProposalIntakeController(
      MissionProposalSubmissionService missionProposalSubmissionService,
      MissionProposalWorkingCopyService missionProposalWorkingCopyService) {
    this.missionProposalSubmissionService = Objects.requireNonNull(
        missionProposalSubmissionService,
        "missionProposalSubmissionService");
    this.missionProposalWorkingCopyService = Objects.requireNonNull(missionProposalWorkingCopyService,
        "missionProposalWorkingCopyService");
  }

  @PostMapping("/api/public/mission-proposals")
  public MissionProposalIntakeWebContract.SubmittedProposalView createMissionProposal(
      @RequestBody MissionProposalIntakeWebContract.ProposalInput request) {
    return MissionProposalIntakeWebContract.fromApplication(
        missionProposalSubmissionService.createMissionProposal(
            MissionProposalIntakeWebContract.toApplication(request)));
  }

  @PostMapping("/api/public/mission-proposal-preview")
  public MissionProposalIntakeWebContract.PreviewView previewMissionProposal(
      @RequestBody MissionProposalIntakeWebContract.PreviewInput request) {
    return MissionProposalIntakeWebContract.fromApplication(
        missionProposalWorkingCopyService.previewMissionProposal(
            MissionProposalIntakeWebContract.toApplication(request)));
  }
}
