package mcp.server.domain.missions.application.intake;

import org.springframework.stereotype.Service;
import mcp.server.domain.missions.application.MissionSpecification;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.requireEmail;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.requireText;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.requireWorkMode;


@Service
public class MissionProposalValidationService {
  public void requireMissionProposal(MissionProposalIntake.ProposalInput request) {
    if (request == null) {
      throw reject("missionProposal is required");
    }
    requireText(request.customerName(), "customerName is required");
    requireEmail(request.customerEmail(), "customerEmail is required");
    requireText(request.missionTitle(), "missionTitle is required");
    if (request.missionSlots() == null || request.missionSlots().isEmpty()) {
      throw reject("at least one mission slot is required");
    }
    for (MissionSpecification.SlotInput slot : request.missionSlots()) {
      requireMissionSlot(slot);
    }
    requireText(request.startDate(), "startDate is required");
    requireText(request.endDate(), "endDate is required");
    requireWorkMode(request.workMode());
  }

  public void requireMissionSlot(MissionSpecification.SlotInput slot) {
    if (slot == null) {
      throw reject("missionSlots must not contain null entries");
    }
    if (slot.roleId() <= 0) {
      throw reject("missionSlots.roleId is required");
    }
    if (slot.requiredRoleExperienceYears() < 0) {
      throw reject("missionSlots.requiredRoleExperienceYears must be zero or positive");
    }
    if (slot.requiredSkills() == null || slot.requiredSkills().isEmpty()) {
      throw reject("each mission slot requires at least one required skill");
    }
  }
}
