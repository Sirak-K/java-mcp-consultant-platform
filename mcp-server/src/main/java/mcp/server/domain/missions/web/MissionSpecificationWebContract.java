package mcp.server.domain.missions.web;

import java.util.List;

import mcp.server.domain.missions.application.MissionSpecification;

public final class MissionSpecificationWebContract {

    private MissionSpecificationWebContract() {
    }

    public record SkillRequirementInput(long skillId, short skillLevelId, String skillCategory) {
        public SkillRequirementInput(long skillId, short skillLevelId) {
            this(skillId, skillLevelId, "PRIMARY");
        }

        public SkillRequirementInput {
            skillCategory = normalizeSkillCategory(skillCategory);
        }
    }

    public record SlotInput(
            long roleId,
            int requiredRoleExperienceYears,
            List<SkillRequirementInput> requiredSkills) {
    }

    public record PresentationInput(
            String oneDayAtWork,
            String technicalLandscape,
            String whoWeAreLookingFor,
            String whatWeOffer,
            String aboutCustomer,
            String recruitmentProcess) {
    }

    public record SkillRequirementView(
            long skillId,
            String skillTitle,
            short skillLevelId,
            String skillLevelName,
            String skillCategory) {
        public SkillRequirementView(
                long skillId,
                String skillTitle,
                short skillLevelId,
                String skillLevelName) {
            this(skillId, skillTitle, skillLevelId, skillLevelName, "PRIMARY");
        }

        public SkillRequirementView {
            skillCategory = normalizeSkillCategory(skillCategory);
        }
    }

    public record SlotSpecificationView(
            int slotNumber,
            long roleId,
            String roleTitle,
            int requiredRoleExperienceYears,
            List<SkillRequirementView> requiredSkills) {
    }

    public record PresentationView(
            String oneDayAtWork,
            String technicalLandscape,
            String whoWeAreLookingFor,
            String whatWeOffer,
            String aboutCustomer,
            String recruitmentProcess) {
    }

    public record SpecificationView(
            String customerName,
            String customerEmail,
            String missionTitle,
            List<SlotSpecificationView> missionSlots,
            String startDate,
            String endDate,
            String workMode,
            PresentationView missionPresentation) {
    }

    public static MissionSpecification.SkillRequirementInput toApplication(SkillRequirementInput input) {
        if (input == null) {
            return null;
        }
        return new MissionSpecification.SkillRequirementInput(
                input.skillId(),
                input.skillLevelId(),
                input.skillCategory());
    }

    public static MissionSpecification.SlotInput toApplication(SlotInput input) {
        if (input == null) {
            return null;
        }
        return new MissionSpecification.SlotInput(
                input.roleId(),
                input.requiredRoleExperienceYears(),
                input.requiredSkills() == null
                        ? null
                        : input.requiredSkills().stream()
                                .map(MissionSpecificationWebContract::toApplication)
                                .toList());
    }

    public static MissionSpecification.PresentationInput toApplication(PresentationInput input) {
        if (input == null) {
            return null;
        }
        return new MissionSpecification.PresentationInput(
                input.oneDayAtWork(),
                input.technicalLandscape(),
                input.whoWeAreLookingFor(),
                input.whatWeOffer(),
                input.aboutCustomer(),
                input.recruitmentProcess());
    }

    public static SkillRequirementView fromApplication(MissionSpecification.SkillRequirementView view) {
        if (view == null) {
            return null;
        }
        return new SkillRequirementView(
                view.skillId(),
                view.skillTitle(),
                view.skillLevelId(),
                view.skillLevelName(),
                view.skillCategory());
    }

    public static SlotSpecificationView fromApplication(MissionSpecification.SlotSpecificationView view) {
        if (view == null) {
            return null;
        }
        return new SlotSpecificationView(
                view.slotNumber(),
                view.roleId(),
                view.roleTitle(),
                view.requiredRoleExperienceYears(),
                view.requiredSkills() == null
                        ? null
                        : view.requiredSkills().stream()
                                .map(MissionSpecificationWebContract::fromApplication)
                                .toList());
    }

    public static PresentationView fromApplication(MissionSpecification.PresentationView view) {
        if (view == null) {
            return null;
        }
        return new PresentationView(
                view.oneDayAtWork(),
                view.technicalLandscape(),
                view.whoWeAreLookingFor(),
                view.whatWeOffer(),
                view.aboutCustomer(),
                view.recruitmentProcess());
    }

    public static SpecificationView fromApplication(MissionSpecification.SpecificationView view) {
        if (view == null) {
            return null;
        }
        return new SpecificationView(
                view.customerName(),
                view.customerEmail(),
                view.missionTitle(),
                view.missionSlots() == null
                        ? null
                        : view.missionSlots().stream()
                                .map(MissionSpecificationWebContract::fromApplication)
                                .toList(),
                view.startDate(),
                view.endDate(),
                view.workMode(),
                fromApplication(view.missionPresentation()));
    }

    private static String normalizeSkillCategory(String rawCategory) {
        return "SECONDARY".equalsIgnoreCase(rawCategory) ? "SECONDARY" : "PRIMARY";
    }
}
