package mcp.server.domain.missions.application;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import mcp.server.domain.missions.persistence.MissionProposalRequiredSkillEntity;
import mcp.server.domain.missions.persistence.MissionProposalSecondaryRequiredSkillEntity;
import mcp.server.domain.missions.persistence.MissionSlotRequiredSkillEntity;
import mcp.server.domain.missions.persistence.MissionSlotSecondaryRequiredSkillEntity;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupEntity;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;

@Component
public class MissionSkillRequirementSpecificationAssembler {

        private final SkillCatalogLookup skillLookup;
        private final CompetencyLevelLookupJpaRepo skillLevelRepo;

        private record SkillLookupKey(String category, Long skillId) {
        }

        public record LookupContext(
                        Map<SkillLookupKey, SkillCatalogLookup.SkillRef> skillsByKey,
                        Map<Short, CompetencyLevelLookupEntity> skillLevelsById) {
        }

        public MissionSkillRequirementSpecificationAssembler(
                        SkillCatalogLookup skillLookup,
                        CompetencyLevelLookupJpaRepo skillLevelRepo) {
                this.skillLookup = skillLookup;
                this.skillLevelRepo = skillLevelRepo;
        }

        public LookupContext lookupContext() {
                return new LookupContext(
                                skillsByKey(),
                                skillLevelRepo.findAll().stream()
                                                .collect(Collectors.toMap(
                                                                CompetencyLevelLookupEntity::getCompetencyLevelLookupId,
                                                                Function.identity())));
        }

        public MissionSpecification.SkillRequirementView toView(
                        MissionSpecification.SkillRequirementInput input,
                        LookupContext lookupContext) {

                SkillCatalogLookup.SkillRef skill = lookupContext.skillsByKey()
                                .get(skillKey(input.skillCategory(), input.skillId()));
                if (skill == null) {
                        throw reject("requiredSkills contains an unknown skillId");
                }
                CompetencyLevelLookupEntity skillLevel = lookupContext.skillLevelsById().get(input.skillLevelId());
                if (skillLevel == null) {
                        throw reject("requiredSkills contains an unknown skillLevelId");
                }
                return new MissionSpecification.SkillRequirementView(
                                skill.id(),
                                skill.title(),
                                skillLevel.getCompetencyLevelLookupId(),
                                skillLevel.getCompetencyLevelName(),
                                input.skillCategory());
        }

        public MissionSpecification.SkillRequirementView toView(
                        MissionProposalRequiredSkillEntity input,
                        LookupContext lookupContext) {
                return storedSkillView(
                                SkillCatalogLookup.CATEGORY_PRIMARY,
                                input.getSkillId(),
                                input.getRequiredSkillLevelId(),
                                lookupContext);
        }

        public MissionSpecification.SkillRequirementView toView(
                        MissionProposalSecondaryRequiredSkillEntity input,
                        LookupContext lookupContext) {
                return storedSkillView(
                                SkillCatalogLookup.CATEGORY_SECONDARY,
                                input.getSkillId(),
                                input.getRequiredSkillLevelId(),
                                lookupContext);
        }

        public MissionSpecification.SkillRequirementView toView(
                        MissionSlotRequiredSkillEntity input,
                        LookupContext lookupContext) {
                return storedSkillView(
                                SkillCatalogLookup.CATEGORY_PRIMARY,
                                input.getSkillId(),
                                input.getRequiredSkillLevelId(),
                                lookupContext);
        }

        public MissionSpecification.SkillRequirementView toView(
                        MissionSlotSecondaryRequiredSkillEntity input,
                        LookupContext lookupContext) {
                return storedSkillView(
                                SkillCatalogLookup.CATEGORY_SECONDARY,
                                input.getSkillId(),
                                input.getRequiredSkillLevelId(),
                                lookupContext);
        }

        private MissionSpecification.SkillRequirementView storedSkillView(
                        String skillCategory,
                        Long skillId,
                        Short requiredSkillLevelId,
                        LookupContext lookupContext) {

                SkillCatalogLookup.SkillRef skill = lookupContext.skillsByKey().get(skillKey(skillCategory, skillId));
                CompetencyLevelLookupEntity skillLevel = lookupContext.skillLevelsById().get(requiredSkillLevelId);
                return new MissionSpecification.SkillRequirementView(
                                skillId,
                                skill == null ? "#" + skillId : skill.title(),
                                requiredSkillLevelId,
                                skillLevel == null ? "#" + requiredSkillLevelId : skillLevel.getCompetencyLevelName(),
                                skillCategory);
        }

        private Map<SkillLookupKey, SkillCatalogLookup.SkillRef> skillsByKey() {
                return Stream.concat(
                                skillLookup.findAllPrimarySkills().stream()
                                                .map(skill -> Map.entry(skillKey(SkillCatalogLookup.CATEGORY_PRIMARY,
                                                                skill.id()), skill)),
                                skillLookup.findAllSecondarySkills().stream()
                                                .map(skill -> Map.entry(skillKey(SkillCatalogLookup.CATEGORY_SECONDARY,
                                                                skill.id()), skill)))
                                .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                Map.Entry::getValue,
                                                (left, right) -> left));
        }

        private SkillLookupKey skillKey(String category, Long skillId) {
                String normalizedCategory = SkillCatalogLookup.CATEGORY_SECONDARY.equals(category)
                                ? SkillCatalogLookup.CATEGORY_SECONDARY
                                : SkillCatalogLookup.CATEGORY_PRIMARY;
                return new SkillLookupKey(normalizedCategory, skillId);
        }
}
