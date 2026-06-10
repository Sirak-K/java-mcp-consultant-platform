package mcp.server.domain.missions.persistence;

import jakarta.persistence.EntityManager;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupEntity;
import mcp.server.domain.reference_data.persistence.PrimarySkillEntity;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.PrimarySkillJpaRepo;
import mcp.server.domain.missions.model.MissionSlotId;
import mcp.server.domain.missions.model.MissionSlotRequiredSkill;
import mcp.server.domain.missions.model.MissionSlotRequiredSkillId;
import mcp.server.domain.reference_data.model.SkillId;
import mcp.server.domain.reference_data.model.SkillLevel;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MissionSlotRequiredSkillRepositoryAdapter implements MissionSlotRequiredSkillRepository {

    private final MissionSlotRequiredSkillJpaRepository jpaRepo;
    private final EntityManager entityManager;
    private final PrimarySkillJpaRepo primarySkillJpaRepo;
    private final CompetencyLevelLookupJpaRepo competencyLevelLookupJpaRepo;

    public MissionSlotRequiredSkillRepositoryAdapter(
            MissionSlotRequiredSkillJpaRepository jpaRepo,
            EntityManager entityManager,
            PrimarySkillJpaRepo primarySkillJpaRepo,
            CompetencyLevelLookupJpaRepo competencyLevelLookupJpaRepo) {
        this.jpaRepo = jpaRepo;
        this.entityManager = entityManager;
        this.primarySkillJpaRepo = primarySkillJpaRepo;
        this.competencyLevelLookupJpaRepo = competencyLevelLookupJpaRepo;
    }

    @Override
    public MissionSlotRequiredSkill save(MissionSlotRequiredSkill skill) {
        Long entityId = skill.getId().isAssigned() ? skill.getId().value() : null;
        MissionSlotRequiredSkillEntity entity = new MissionSlotRequiredSkillEntity(
                entityId,
                skill.getMissionSlotId().value(),
                skill.getSkillId().value(),
                resolveSkillTitle(skill.getSkillId()),
                resolveCompetencyLevelId(skill.getRequiredSkillLevel()));
        entity.setMissionSlot(entityManager.getReference(MissionSlotEntity.class, skill.getMissionSlotId().value()));
        MissionSlotRequiredSkillEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<MissionSlotRequiredSkill> findByMissionSlotId(MissionSlotId missionSlotId) {
        return jpaRepo.findByMissionSlotId(missionSlotId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(MissionSlotRequiredSkillId id) {
        jpaRepo.deleteById(id.value());
    }

    @Override
    public int countByMissionSlotId(MissionSlotId missionSlotId) {
        return jpaRepo.countByMissionSlotId(missionSlotId.value());
    }

    @Override
    public int countBySkillId(SkillId skillId) {
        return jpaRepo.countBySkillId(skillId.value());
    }

    private MissionSlotRequiredSkill toDomain(MissionSlotRequiredSkillEntity entity) {
        Long missionSlotId = entity.getMissionSlotId() != null
                ? entity.getMissionSlotId()
                : entity.getMissionSlot() != null ? entity.getMissionSlot().getId() : null;
        if (missionSlotId == null) {
            throw new IllegalStateException("MissionSlotRequiredSkillEntity mission slot id is missing");
        }
        return new MissionSlotRequiredSkill(
                new MissionSlotRequiredSkillId(entity.getMissionSlotRequiredSkillId()),
                new MissionSlotId(missionSlotId),
                new SkillId(entity.getSkillId()),
                resolveSkillLevel(entity.getRequiredSkillLevelId()));
    }

    private Short resolveCompetencyLevelId(SkillLevel skillLevel) {
        return competencyLevelLookupJpaRepo.findByCompetencyLevelName(skillLevel.name())
                .map(CompetencyLevelLookupEntity::getCompetencyLevelLookupId)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing competency_level_lookup row for skill level name: " + skillLevel.name()));
    }

    private SkillLevel resolveSkillLevel(Short competencyLevelId) {
        return competencyLevelLookupJpaRepo.findById(competencyLevelId)
                .map(CompetencyLevelLookupEntity::getCompetencyLevelName)
                .map(SkillLevel::valueOf)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing competency_level_lookup row for id: " + competencyLevelId));
    }

    private String resolveSkillTitle(SkillId skillId) {
        return primarySkillJpaRepo.findById(skillId.value())
                .map(PrimarySkillEntity::getSkillTitle)
                .orElseThrow(() -> new IllegalStateException("Missing primary skill row for id: " + skillId.value()));
    }
}
