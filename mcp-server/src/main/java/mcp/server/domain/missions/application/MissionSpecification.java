package mcp.server.domain.missions.application;

import java.util.List;

public final class MissionSpecification {

    public static final int PRESENTATION_MAX_WORDS = 100;

    private static final PresentationInput DEFAULT_PRESENTATION = new PresentationInput(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer vitae sem at arcu luctus facilisis.",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur sed nibh ac justo tristique luctus.",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent vitae risus eget mi posuere facilisis.",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean vitae augue id mauris volutpat gravida.",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec non erat sed ipsum gravida consequat.",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed porta nulla vitae libero cursus hendrerit.");

    private MissionSpecification() {
    }

    public static PresentationInput defaultPresentation() {
        return DEFAULT_PRESENTATION;
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

    private static String normalizeSkillCategory(String rawCategory) {
        return "SECONDARY".equalsIgnoreCase(rawCategory) ? "SECONDARY" : "PRIMARY";
    }
}
