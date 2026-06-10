package mcp.server.domain.missions.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import mcp.server.domain.missions.persistence.MissionProposalSlotEntity;
import mcp.server.domain.missions.persistence.MissionSlotEntity;
import mcp.server.domain.reference_data.persistence.RoleEntity;
import mcp.server.domain.reference_data.persistence.RoleJpaRepo;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;

@Component
public class MissionSlotSpecificationAssembler {

        private final RoleJpaRepo roleRepo;
        private final MissionSkillRequirementSpecificationAssembler skillRequirementAssembler;

        public MissionSlotSpecificationAssembler(
                        RoleJpaRepo roleRepo,
                        MissionSkillRequirementSpecificationAssembler skillRequirementAssembler) {
                this.roleRepo = roleRepo;
                this.skillRequirementAssembler = skillRequirementAssembler;
        }

        public List<MissionSpecification.SlotSpecificationView> toSpecificationViewsFromInputSlots(
                        List<MissionSpecification.SlotInput> missionSlots) {

                Map<Long, RoleEntity> rolesById = rolesById();
                MissionSkillRequirementSpecificationAssembler.LookupContext skillLookupContext = skillRequirementAssembler
                                .lookupContext();
                return slotsWithNumbers(missionSlots).stream()
                                .map(slot -> toMissionSlotSpecificationView(slot, rolesById, skillLookupContext))
                                .toList();
        }

        public List<MissionSpecification.SlotSpecificationView> toSpecificationViewsFromProposalSlots(
                        List<MissionProposalSlotEntity> missionSlots) {

                Map<Long, RoleEntity> rolesById = rolesById();
                MissionSkillRequirementSpecificationAssembler.LookupContext skillLookupContext = skillRequirementAssembler
                                .lookupContext();
                return missionSlots.stream()
                                .sorted(Comparator.comparing(
                                                MissionProposalSlotEntity::getMissionSlotNumber))
                                .map(slot -> toMissionSlotSpecificationView(slot, rolesById, skillLookupContext))
                                .toList();
        }

        public List<MissionSpecification.SlotSpecificationView> toSpecificationViewsFromRegisteredSlots(
                        List<MissionSlotEntity> missionSlots) {

                Map<Long, RoleEntity> rolesById = rolesById();
                MissionSkillRequirementSpecificationAssembler.LookupContext skillLookupContext = skillRequirementAssembler
                                .lookupContext();
                return missionSlots.stream()
                                .sorted(Comparator.comparing(MissionSlotEntity::getMissionSlotNumber))
                                .map(slot -> toMissionSlotSpecificationView(slot, rolesById, skillLookupContext))
                                .toList();
        }

        private Map<Long, RoleEntity> rolesById() {
                return roleRepo.findAll().stream()
                                .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));
        }

        private List<MissionSpecification.SlotSpecificationView> slotsWithNumbers(
                        List<MissionSpecification.SlotInput> slots) {

                return IntStream.range(0, slots.size())
                                .mapToObj(index -> new MissionSpecification.SlotSpecificationView(
                                                index + 1,
                                                slots.get(index).roleId(),
                                                "",
                                                slots.get(index).requiredRoleExperienceYears(),
                                                slots.get(index).requiredSkills().stream()
                                                                .map(skill -> new MissionSpecification.SkillRequirementView(
                                                                                skill.skillId(),
                                                                                "",
                                                                                skill.skillLevelId(),
                                                                                "",
                                                                                skill.skillCategory()))
                                                                .toList()))
                                .toList();
        }

        private MissionSpecification.SlotSpecificationView toMissionSlotSpecificationView(
                        MissionSpecification.SlotSpecificationView slot,
                        Map<Long, RoleEntity> rolesById,
                        MissionSkillRequirementSpecificationAssembler.LookupContext skillLookupContext) {

                RoleEntity role = rolesById.get(slot.roleId());
                if (role == null) {
                        throw reject("missionSlots contains an unknown roleId");
                }
                List<MissionSpecification.SkillRequirementView> requiredSkills = slot.requiredSkills().stream()
                                .map(skill -> new MissionSpecification.SkillRequirementInput(
                                                skill.skillId(),
                                                skill.skillLevelId(),
                                                skill.skillCategory()))
                                .map(skill -> skillRequirementAssembler.toView(skill, skillLookupContext))
                                .toList();

                return new MissionSpecification.SlotSpecificationView(
                                slot.slotNumber(),
                                role.getId(),
                                role.getRoleTitle(),
                                slot.requiredRoleExperienceYears(),
                                requiredSkills);
        }

        private MissionSpecification.SlotSpecificationView toMissionSlotSpecificationView(
                        MissionProposalSlotEntity slot,
                        Map<Long, RoleEntity> rolesById,
                        MissionSkillRequirementSpecificationAssembler.LookupContext skillLookupContext) {

                RoleEntity role = rolesById.get(slot.getRoleId());
                if (role == null) {
                        throw new IllegalStateException("Stored slot roleId is unknown: " + slot.getRoleId());
                }
                return new MissionSpecification.SlotSpecificationView(
                                slot.getMissionSlotNumber(),
                                slot.getRoleId(),
                                role.getRoleTitle(),
                                slot.getRequiredRoleExperienceYears(),
                                Stream.concat(
                                                slot.getRequiredSkills().stream()
                                                                .map(skill -> skillRequirementAssembler.toView(skill,
                                                                                skillLookupContext)),
                                                slot.getSecondaryRequiredSkills().stream()
                                                                .map(skill -> skillRequirementAssembler.toView(skill,
                                                                                skillLookupContext)))
                                                .toList());
        }

        private MissionSpecification.SlotSpecificationView toMissionSlotSpecificationView(
                        MissionSlotEntity slot,
                        Map<Long, RoleEntity> rolesById,
                        MissionSkillRequirementSpecificationAssembler.LookupContext skillLookupContext) {

                RoleEntity role = rolesById.get(slot.getRoleId());
                if (role == null) {
                        throw new IllegalStateException("Stored slot roleId is unknown: " + slot.getRoleId());
                }
                return new MissionSpecification.SlotSpecificationView(
                                slot.getMissionSlotNumber(),
                                slot.getRoleId(),
                                role.getRoleTitle(),
                                slot.getRequiredRoleExperienceYears(),
                                Stream.concat(
                                                slot.getRequiredSkills().stream()
                                                                .map(skill -> skillRequirementAssembler.toView(skill,
                                                                                skillLookupContext)),
                                                slot.getSecondaryRequiredSkills().stream()
                                                                .map(skill -> skillRequirementAssembler.toView(skill,
                                                                                skillLookupContext)))
                                                .toList());
        }
}
