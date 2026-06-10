package mcp.server.domain.missions.application.intake;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
final class MissionProposalWorkingCopyTextEvidenceDetector {

  private final MissionProposalCustomerTextDetector customerTextDetector;
  private final MissionProposalTitleTextDetector titleTextDetector;
  private final MissionProposalScheduleTextDetector scheduleTextDetector;
  private final MissionProposalRequirementTextDetector requirementTextDetector;

  public MissionProposalWorkingCopyTextEvidenceDetector(
      MissionProposalCustomerTextDetector customerTextDetector,
      MissionProposalTitleTextDetector titleTextDetector,
      MissionProposalScheduleTextDetector scheduleTextDetector,
      MissionProposalRequirementTextDetector requirementTextDetector) {
    this.customerTextDetector = Objects.requireNonNull(customerTextDetector, "customerTextDetector");
    this.titleTextDetector = Objects.requireNonNull(titleTextDetector, "titleTextDetector");
    this.scheduleTextDetector = Objects.requireNonNull(scheduleTextDetector, "scheduleTextDetector");
    this.requirementTextDetector = Objects.requireNonNull(requirementTextDetector, "requirementTextDetector");
  }

  public WorkingCopyDetectionResult detect(String sourceText) {
    List<MissionProposalIntake.WorkingCopyEvidenceView> evidence = new ArrayList<>();

    String customerName = customerTextDetector.detectCustomerName(sourceText, evidence);
    String customerEmail = customerTextDetector.detectEmail(sourceText, evidence);
    MissionProposalRequirementTextDetector.MatchedRole role = requirementTextDetector.detectRole(sourceText, evidence);
    MissionProposalRequirementTextDetector.DetectedSlotCount slotCount = requirementTextDetector.detectMissionSlotCount(
        sourceText,
        evidence);
    MissionProposalTitleTextDetector.DetectedTitle missionTitle = titleTextDetector.detectMissionTitle(
        sourceText,
        role == null ? "" : role.role().getRoleTitle(),
        role == null ? "" : role.sourceText(),
        slotCount.count(),
        evidence);
    MissionProposalScheduleTextDetector.DetectedDateRange dateRange = scheduleTextDetector.detectDateRange(
        sourceText,
        evidence);
    String workMode = requirementTextDetector.detectWorkMode(sourceText, evidence);
    MissionProposalRequirementTextDetector.DetectedYears experienceYears = requirementTextDetector
        .detectExperienceYears(sourceText, evidence);
    List<MissionProposalRequirementTextDetector.MatchedSkill> skills = requirementTextDetector.detectSkills(
        sourceText,
        slotCount,
        evidence);
    MissionProposalRequirementTextDetector.DetectedSkillLevel skillLevel = requirementTextDetector.detectSkillLevel(
        sourceText,
        evidence);

    return new WorkingCopyDetectionResult(
        customerName,
        customerEmail,
        missionTitle.value(),
        missionTitle.confident(),
        dateRange.startDate(),
        dateRange.endDate(),
        workMode,
        role == null ? 0L : role.role().getId(),
        role != null,
        experienceYears.years(),
        experienceYears.detected(),
        skillLevel.skillLevelId(),
        skillLevel.detected(),
        slotCount.count(),
        slotCount.detected(),
        skills.stream()
            .map(skill -> new MissionProposalWorkingCopyBuilder.RequiredSkill(
                skill.skill().id(),
                skill.skillCategory(),
                skill.slotIndex()))
            .toList(),
        evidence);
  }

  public record WorkingCopyDetectionResult(
      String customerName,
      String customerEmail,
      String missionTitle,
      boolean missionTitleConfident,
      String startDate,
      String endDate,
      String workMode,
      long roleId,
      boolean roleDetected,
      int roleExperienceYears,
      boolean roleExperienceYearsDetected,
      short skillLevelId,
      boolean skillLevelDetected,
      int slotCount,
      boolean slotCountDetected,
      List<MissionProposalWorkingCopyBuilder.RequiredSkill> requiredSkills,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    public WorkingCopyDetectionResult {
      requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
      evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
  }
}
