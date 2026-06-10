package mcp.server.domain.missions.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import mcp.server.domain.missions.application.MissionSpecification;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.parseDate;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.toShort;

@Component
public class MissionMaterializationEntityMapper {
        public MissionEntity toMissionEntity(
                        MissionProposalEntity proposal,
                        MissionSpecification.SpecificationView specification) {

                return new MissionEntity(
                                null,
                                proposal.getId(),
                                proposal.getCustomerId(),
                                specification.customerName(),
                                specification.customerEmail(),
                                specification.missionTitle(),
                                parseDate(specification.startDate(), "startDate"),
                                parseDate(specification.endDate(), "endDate"),
                                specification.workMode(),
                                specification.missionPresentation().oneDayAtWork(),
                                specification.missionPresentation().technicalLandscape(),
                                specification.missionPresentation().whoWeAreLookingFor(),
                                specification.missionPresentation().whatWeOffer(),
                                specification.missionPresentation().aboutCustomer(),
                                specification.missionPresentation().recruitmentProcess(),
                                "OPEN");
        }

        public void applyMission(
                        MissionEntity mission,
                        MissionSpecification.SpecificationView specification,
                        Long sourceMissionProposalId,
                        Long customerId) {

                mission.setSourceMissionProposalId(sourceMissionProposalId);
                mission.setCustomerId(customerId);
                mission.setCustomerName(specification.customerName());
                mission.setCustomerEmail(specification.customerEmail());
                mission.setMissionTitle(specification.missionTitle());
                mission.setStartDate(parseDate(specification.startDate(), "startDate"));
                mission.setEndDate(parseDate(specification.endDate(), "endDate"));
                mission.setWorkMode(specification.workMode());
                mission.setPresentationOneDayAtWork(specification.missionPresentation().oneDayAtWork());
                mission.setPresentationTechnicalLandscape(
                                specification.missionPresentation().technicalLandscape());
                mission.setPresentationWhoWeAreLookingFor(
                                specification.missionPresentation().whoWeAreLookingFor());
                mission.setPresentationWhatWeOffer(specification.missionPresentation().whatWeOffer());
                mission.setPresentationAboutCustomer(specification.missionPresentation().aboutCustomer());
                mission.setPresentationRecruitmentProcess(
                                specification.missionPresentation().recruitmentProcess());
                mission.setMissionAvailability("OPEN");
        }

        public List<MissionSlotEntity> toRegisteredSlotEntities(
                        Long missionId,
                        List<MissionSpecification.SlotSpecificationView> missionSlots) {

                return missionSlots.stream()
                                .map(slot -> {
                                        MissionSlotEntity entity = new MissionSlotEntity(
                                                        null,
                                                        missionId,
                                                        slot.roleId(),
                                                        toShort(
                                                                        slot.requiredRoleExperienceYears(),
                                                                        "requiredRoleExperienceYears"),
                                                        slot.slotNumber(),
                                                        "NOT_FILLED",
                                                        null,
                                                        null,
                                                        null);
                                        entity.replaceRequiredSkills(toMissionPrimaryEntities(slot.requiredSkills()));
                                        entity.replaceSecondaryRequiredSkills(
                                                        toMissionSecondaryEntities(slot.requiredSkills()));
                                        return entity;
                                })
                                .toList();
        }

        private List<MissionSlotRequiredSkillEntity> toMissionPrimaryEntities(
                        List<MissionSpecification.SkillRequirementView> requiredSkills) {

                return requiredSkills.stream()
                                .filter(skill -> SkillCatalogLookup.CATEGORY_PRIMARY.equals(skill.skillCategory()))
                                .map(skill -> new MissionSlotRequiredSkillEntity(
                                                null,
                                                skill.skillId(),
                                                skill.skillTitle(),
                                                skill.skillLevelId()))
                                .toList();
        }

        private List<MissionSlotSecondaryRequiredSkillEntity> toMissionSecondaryEntities(
                        List<MissionSpecification.SkillRequirementView> requiredSkills) {

                return requiredSkills.stream()
                                .filter(skill -> SkillCatalogLookup.CATEGORY_SECONDARY.equals(skill.skillCategory()))
                                .map(skill -> new MissionSlotSecondaryRequiredSkillEntity(
                                                null,
                                                skill.skillId(),
                                                skill.skillTitle(),
                                                skill.skillLevelId()))
                                .toList();
        }
}
