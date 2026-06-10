package mcp.server.domain.missions.web;

import java.util.List;

import mcp.server.domain.missions.application.MissionProposalReview;
import mcp.server.domain.missions.application.intake.MissionProposalIntake;

public final class MissionProposalIntakeWebContract {

    private MissionProposalIntakeWebContract() {
    }

    public record ProposalInput(
            String customerName,
            String customerEmail,
            String missionTitle,
            List<MissionSpecificationWebContract.SlotInput> missionSlots,
            String startDate,
            String endDate,
            String workMode,
            MissionSpecificationWebContract.PresentationInput missionPresentation) {
    }

    public record PreviewInput(String roleAndRequirementsText) {
    }

    public record WorkingCopyEvidenceView(
            String field,
            String value,
            String sourceText,
            Double confidence) {
    }

    public record PreviewView(
            ProposalInput proposalWorkingCopy,
            List<WorkingCopyEvidenceView> evidence,
            List<String> missingFields) {
    }

    public record SubmittedProposalView(
            long id,
            String status,
            MissionSpecificationWebContract.SpecificationView specification,
            List<?> findCandidateResults,
            String outcome,
            String createdAt,
            String updatedAt) {
    }

    public static MissionProposalIntake.ProposalInput toApplication(ProposalInput input) {
        if (input == null) {
            return null;
        }
        return new MissionProposalIntake.ProposalInput(
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
                MissionSpecificationWebContract.toApplication(input.missionPresentation()));
    }

    public static MissionProposalIntake.PreviewInput toApplication(PreviewInput input) {
        if (input == null) {
            return null;
        }
        return new MissionProposalIntake.PreviewInput(input.roleAndRequirementsText());
    }

    public static ProposalInput fromApplication(MissionProposalIntake.ProposalInput input) {
        if (input == null) {
            return null;
        }
        return new ProposalInput(
                input.customerName(),
                input.customerEmail(),
                input.missionTitle(),
                input.missionSlots() == null
                        ? null
                        : input.missionSlots().stream()
                                .map(slot -> new MissionSpecificationWebContract.SlotInput(
                                        slot.roleId(),
                                        slot.requiredRoleExperienceYears(),
                                        slot.requiredSkills() == null
                                                ? null
                                                : slot.requiredSkills().stream()
                                                        .map(skill -> new MissionSpecificationWebContract.SkillRequirementInput(
                                                                skill.skillId(),
                                                                skill.skillLevelId(),
                                                                skill.skillCategory()))
                                                        .toList()))
                                .toList(),
                input.startDate(),
                input.endDate(),
                input.workMode(),
                new MissionSpecificationWebContract.PresentationInput(
                        input.missionPresentation().oneDayAtWork(),
                        input.missionPresentation().technicalLandscape(),
                        input.missionPresentation().whoWeAreLookingFor(),
                        input.missionPresentation().whatWeOffer(),
                        input.missionPresentation().aboutCustomer(),
                        input.missionPresentation().recruitmentProcess()));
    }

    public static WorkingCopyEvidenceView fromApplication(MissionProposalIntake.WorkingCopyEvidenceView view) {
        if (view == null) {
            return null;
        }
        return new WorkingCopyEvidenceView(
                view.field(),
                view.value(),
                view.sourceText(),
                view.confidence());
    }

    public static PreviewView fromApplication(MissionProposalIntake.PreviewView view) {
        if (view == null) {
            return null;
        }
        return new PreviewView(
                fromApplication(view.proposalWorkingCopy()),
                view.evidence() == null
                        ? null
                        : view.evidence().stream()
                                .map(MissionProposalIntakeWebContract::fromApplication)
                                .toList(),
                view.missingFields());
    }

    public static SubmittedProposalView fromApplication(MissionProposalReview.ProposalView view) {
        if (view == null) {
            return null;
        }
        return new SubmittedProposalView(
                view.id(),
                view.status(),
                MissionSpecificationWebContract.fromApplication(view.specification()),
                view.findCandidateResults(),
                view.outcome(),
                view.createdAt(),
                view.updatedAt());
    }
}
