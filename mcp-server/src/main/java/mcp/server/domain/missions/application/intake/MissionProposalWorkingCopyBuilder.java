package mcp.server.domain.missions.application.intake;

import org.springframework.stereotype.Component;
import mcp.server.domain.missions.application.MissionSpecification;

import java.util.List;
import java.util.stream.IntStream;

@Component
public final class MissionProposalWorkingCopyBuilder {

        public record ProposalWorkingCopyInput(
                        String customerName,
                        String customerEmail,
                        String missionTitle,
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
                        List<RequiredSkill> requiredSkills) {
        }

        public record RequiredSkill(long skillId, String skillCategory, Integer slotIndex) {
                public RequiredSkill(long skillId, String skillCategory) {
                        this(skillId, skillCategory, null);
                }

                private boolean appliesToSlot(int slotIndex) {
                        return this.slotIndex == null || this.slotIndex == slotIndex;
                }
        }

        public MissionProposalIntake.ProposalInput build(ProposalWorkingCopyInput input) {
                List<MissionSpecification.SlotInput> missionSlots = shouldCreateWorkingCopySlot(input)
                                ? IntStream.range(0, input.slotCount())
                                                .mapToObj(slotIndex -> toWorkingCopySlot(input, slotIndex))
                                                .toList()
                                : List.of();
                return new MissionProposalIntake.ProposalInput(
                                input.customerName(),
                                input.customerEmail(),
                                input.missionTitle(),
                                missionSlots,
                                input.startDate(),
                                input.endDate(),
                                input.workMode(),
                                MissionSpecification.defaultPresentation());
        }

        private MissionSpecification.SlotInput toWorkingCopySlot(
                        ProposalWorkingCopyInput input,
                        int slotIndex) {

                List<MissionSpecification.SkillRequirementInput> requiredSkills = input.requiredSkills().stream()
                                .filter(skill -> skill.appliesToSlot(slotIndex))
                                .map(skill -> new MissionSpecification.SkillRequirementInput(
                                                skill.skillId(),
                                                input.skillLevelId(),
                                                skill.skillCategory()))
                                .toList();
                return new MissionSpecification.SlotInput(
                                input.roleId(),
                                input.roleExperienceYears(),
                                requiredSkills);
        }

        private boolean shouldCreateWorkingCopySlot(ProposalWorkingCopyInput input) {
                return input.slotCountDetected()
                                || input.roleDetected()
                                || input.roleExperienceYearsDetected()
                                || !input.requiredSkills().isEmpty()
                                || input.skillLevelDetected();
        }
}
