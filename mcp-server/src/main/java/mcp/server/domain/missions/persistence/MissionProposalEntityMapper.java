package mcp.server.domain.missions.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import mcp.server.domain.missions.application.MissionSpecification;
import mcp.server.domain.missions.model.MissionProposalStatus;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.parseDate;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.toShort;

@Component
public class MissionProposalEntityMapper {
        public MissionProposalEntity toProposalEntity(
                        MissionSpecification.SpecificationView specification,
                        Long customerId) {

                Instant now = Instant.now();
                MissionProposalEntity entity = new MissionProposalEntity(
                                null,
                                customerId,
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
                                MissionProposalStatus.SUBMITTED.name(),
                                now,
                                now,
                                null);
                entity.replaceMissionSlots(toProposalSlotEntities(specification.missionSlots()));
                return entity;
        }

        public void applyProposalEdit(
                        MissionProposalEntity entity,
                        MissionSpecification.SpecificationView specification,
                        Long customerId,
                        MissionProposalStatus status) {

                entity.setCustomerId(customerId);
                entity.setCustomerName(specification.customerName());
                entity.setCustomerEmail(specification.customerEmail());
                entity.setMissionTitle(specification.missionTitle());
                entity.setStartDate(parseDate(specification.startDate(), "startDate"));
                entity.setEndDate(parseDate(specification.endDate(), "endDate"));
                entity.setWorkMode(specification.workMode());
                entity.setPresentationOneDayAtWork(specification.missionPresentation().oneDayAtWork());
                entity.setPresentationTechnicalLandscape(
                                specification.missionPresentation().technicalLandscape());
                entity.setPresentationWhoWeAreLookingFor(
                                specification.missionPresentation().whoWeAreLookingFor());
                entity.setPresentationWhatWeOffer(specification.missionPresentation().whatWeOffer());
                entity.setPresentationAboutCustomer(specification.missionPresentation().aboutCustomer());
                entity.setPresentationRecruitmentProcess(
                                specification.missionPresentation().recruitmentProcess());
                entity.setStatus(status.name());
                entity.setUpdatedAt(Instant.now());
        }

        public List<MissionProposalSlotEntity> toProposalSlotEntities(
                        List<MissionSpecification.SlotSpecificationView> missionSlots) {

                return missionSlots.stream()
                                .map(slot -> {
                                        MissionProposalSlotEntity entity = new MissionProposalSlotEntity(
                                                        null,
                                                        slot.slotNumber(),
                                                        slot.roleId(),
                                                        toShort(
                                                                        slot.requiredRoleExperienceYears(),
                                                                        "requiredRoleExperienceYears"),
                                                        null);
                                        entity.replaceRequiredSkills(toProposalPrimaryEntities(slot.requiredSkills()));
                                        entity.replaceSecondaryRequiredSkills(
                                                        toProposalSecondaryEntities(slot.requiredSkills()));
                                        return entity;
                                })
                                .toList();
        }

        private List<MissionProposalRequiredSkillEntity> toProposalPrimaryEntities(
                        List<MissionSpecification.SkillRequirementView> requiredSkills) {

                return requiredSkills.stream()
                                .filter(skill -> SkillCatalogLookup.CATEGORY_PRIMARY.equals(skill.skillCategory()))
                                .map(skill -> new MissionProposalRequiredSkillEntity(
                                                null,
                                                skill.skillId(),
                                                skill.skillLevelId()))
                                .toList();
        }

        private List<MissionProposalSecondaryRequiredSkillEntity> toProposalSecondaryEntities(
                        List<MissionSpecification.SkillRequirementView> requiredSkills) {

                return requiredSkills.stream()
                                .filter(skill -> SkillCatalogLookup.CATEGORY_SECONDARY.equals(skill.skillCategory()))
                                .map(skill -> new MissionProposalSecondaryRequiredSkillEntity(
                                                null,
                                                skill.skillId(),
                                                skill.skillLevelId()))
                                .toList();
        }
}
