package mcp.server.domain.missions.application.intake;

import java.util.List;

import mcp.server.domain.missions.application.MissionSpecification;

public final class MissionProposalIntake {

    private MissionProposalIntake() {
    }

    public record ProposalInput(
            String customerName,
            String customerEmail,
            String missionTitle,
            List<MissionSpecification.SlotInput> missionSlots,
            String startDate,
            String endDate,
            String workMode,
            MissionSpecification.PresentationInput missionPresentation) {
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
}
