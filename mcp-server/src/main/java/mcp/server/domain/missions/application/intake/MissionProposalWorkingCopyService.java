package mcp.server.domain.missions.application.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.missions.application.intake.MissionProposalWorkingCopyTextEvidenceDetector.WorkingCopyDetectionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Service
public class MissionProposalWorkingCopyService {

  private static final int MAX_CUST_TEXT_LENGTH = 10_000;

  private final MissionProposalWorkingCopyBuilder proposalWorkingCopyBuilder;
  private final MissionProposalWorkingCopyTextEvidenceDetector textEvidenceDetector;

  public MissionProposalWorkingCopyService(
      MissionProposalWorkingCopyBuilder proposalWorkingCopyBuilder,
      MissionProposalWorkingCopyTextEvidenceDetector textEvidenceDetector) {
    this.proposalWorkingCopyBuilder = Objects.requireNonNull(proposalWorkingCopyBuilder, "proposalWorkingCopyBuilder");
    this.textEvidenceDetector = Objects.requireNonNull(textEvidenceDetector, "textEvidenceDetector");
  }

  @Transactional(readOnly = true)
  public MissionProposalIntake.PreviewView previewMissionProposal(
      MissionProposalIntake.PreviewInput request) {

    String sourceText = normalizedSourceText(request);
    WorkingCopyDetectionResult detected = textEvidenceDetector.detect(sourceText);
    MissionProposalIntake.ProposalInput proposalWorkingCopy = proposalWorkingCopyBuilder.build(
        toProposalWorkingCopyInput(detected));

    return new MissionProposalIntake.PreviewView(
        proposalWorkingCopy,
        detected.evidence(),
        missingFields(detected.customerName(), detected));
  }

  private String normalizedSourceText(MissionProposalIntake.PreviewInput request) {
    String sourceText = safeText(request == null ? null : request.roleAndRequirementsText())
        .replace(' ', ' ')
        .trim();
    if (sourceText.isBlank()) {
      throw reject("roleAndRequirementsText is required");
    }
    if (sourceText.length() > MAX_CUST_TEXT_LENGTH) {
      throw reject("roleAndRequirementsText must be 10000 characters or fewer");
    }
    return sourceText;
  }

  private MissionProposalWorkingCopyBuilder.ProposalWorkingCopyInput toProposalWorkingCopyInput(
      WorkingCopyDetectionResult detected) {

    return new MissionProposalWorkingCopyBuilder.ProposalWorkingCopyInput(
        detected.customerName(),
        detected.customerEmail(),
        detected.missionTitle(),
        detected.startDate(),
        detected.endDate(),
        detected.workMode(),
        detected.roleId(),
        detected.roleDetected(),
        detected.roleExperienceYears(),
        detected.roleExperienceYearsDetected(),
        detected.skillLevelId(),
        detected.skillLevelDetected(),
        detected.slotCount(),
        detected.slotCountDetected(),
        detected.requiredSkills());
  }

  private List<String> missingFields(
      String customerName,
      WorkingCopyDetectionResult detected) {

    List<String> missingFields = new ArrayList<>();
    if (customerName.isBlank()) {
      missingFields.add("customerName");
    }
    if (detected.customerEmail().isBlank()) {
      missingFields.add("customerEmail");
    }
    if (detected.missionTitle().isBlank() || !detected.missionTitleConfident()) {
      missingFields.add("missionTitle");
    }
    if (!detected.roleDetected()) {
      missingFields.add("missionSlots[0].roleId");
    }
    if (detected.requiredSkills().isEmpty()) {
      missingFields.add("missionSlots[0].requiredSkills");
    }
    if (!detected.requiredSkills().isEmpty() && !detected.skillLevelDetected()) {
      missingFields.add("missionSlots[0].requiredSkills.skillLevelId");
    }
    if (detected.startDate().isBlank()) {
      missingFields.add("startDate");
    }
    if (detected.endDate().isBlank()) {
      missingFields.add("endDate");
    }
    if (detected.workMode().isBlank()) {
      missingFields.add("workMode");
    }
    return missingFields;
  }
}
