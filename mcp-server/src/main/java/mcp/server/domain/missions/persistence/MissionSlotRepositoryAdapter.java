package mcp.server.domain.missions.persistence;

import org.springframework.stereotype.Repository;

import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupEntity;
import mcp.server.domain.reference_data.persistence.PrimarySkillEntity;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.PrimarySkillJpaRepo;
import mcp.server.domain.missions.model.MissionSlot;
import mcp.server.domain.missions.model.MissionSlotFillStatus;
import mcp.server.domain.missions.model.MissionSlotId;
import mcp.server.domain.missions.model.MissionSlotRequiredSkill;
import mcp.server.domain.missions.model.MissionSlotRequiredSkillId;
import mcp.server.domain.reference_data.model.RoleId;
import mcp.server.domain.reference_data.model.SkillId;
import mcp.server.domain.reference_data.model.SkillLevel;
import mcp.server.domain.missions.model.MissionId;

import java.util.List;
import java.util.Optional;

@Repository
public class MissionSlotRepositoryAdapter implements MissionSlotRepository {

    private final MissionSlotJpaRepository jpaRepo;
    private final PrimarySkillJpaRepo primarySkillJpaRepo;
    private final CompetencyLevelLookupJpaRepo competencyLevelLookupJpaRepo;

    public MissionSlotRepositoryAdapter(
            MissionSlotJpaRepository jpaRepo,
            PrimarySkillJpaRepo primarySkillJpaRepo,
            CompetencyLevelLookupJpaRepo competencyLevelLookupJpaRepo) {
        this.jpaRepo = jpaRepo;
        this.primarySkillJpaRepo = primarySkillJpaRepo;
        this.competencyLevelLookupJpaRepo = competencyLevelLookupJpaRepo;
    }

    @Override
    public MissionSlot save(MissionSlot missionSlot) {
        MissionSlotEntity entity = toEntity(missionSlot);
        MissionSlotEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<MissionSlot> findById(MissionSlotId id) {
        return jpaRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<MissionSlot> findByMission(MissionId missionId) {
        return jpaRepo.findByMissionId(missionId.value())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(MissionSlotId id) {
        jpaRepo.deleteById(id.value());
    }

    @Override
    public boolean existsById(MissionSlotId id) {
        return jpaRepo.existsById(id.value());
    }

    @Override
    public int countByMission(MissionId missionId) {
        return jpaRepo.countByMissionId(missionId.value());
    }

    @Override
    public int countByRoleId(RoleId roleId) {
        return jpaRepo.countByRoleId(roleId.value());
    }

    @Override
    public int countByFilledByCandidateProfileId(long candidateProfileId) {
        return jpaRepo.countByMissionSlotFilledByProfileId(candidateProfileId);
    }

    public List<MissionSlot> findByRoleIdAndNotFilled(RoleId roleId) {
        return jpaRepo.findByRoleIdAndMissionSlotFillStatus(
                roleId.value(), MissionSlotFillStatus.NOT_FILLED.name())
                .stream().map(this::toDomain).toList();
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private MissionSlotEntity toEntity(MissionSlot domain) {
        Long entityId = domain.getId().value() == 0 ? null : domain.getId().value();
        MissionSlotEntity entity = new MissionSlotEntity(
                entityId,
                domain.getMissionId().value(),
                domain.getRoleId().value(),
                toSmallint(domain.getRequiredRoleExperienceYears()),
                domain.getMissionSlotNumber(),
                domain.getFillStatus().name(),
                domain.getFilledByCandidateProfileId(),
                domain.getFilledAt(),
                List.of());
        entity.setRequiredSkills(domain.getRequiredSkills().stream()
                .map(requiredSkill -> requiredSkillToEntity(entity, requiredSkill))
                .toList());
        return entity;
    }

    private MissionSlot toDomain(MissionSlotEntity entity) {
        return new MissionSlot(
                new MissionSlotId(entity.getId()),
                new MissionId(entity.getMissionId()),
                new RoleId(entity.getRoleId()),
                entity.getRequiredSkills().stream()
                        .map(requiredSkill -> new MissionSlotRequiredSkill(
                                new MissionSlotRequiredSkillId(requiredSkill.getMissionSlotRequiredSkillId()),
                                new MissionSlotId(entity.getId()),
                                new SkillId(requiredSkill.getSkillId()),
                                resolveSkillLevel(requiredSkill.getRequiredSkillLevelId())))
                        .toList(),
                entity.getRequiredRoleExperienceYears() != null
                        ? entity.getRequiredRoleExperienceYears().intValue()
                        : 0,
                entity.getMissionSlotNumber(),
                MissionSlotFillStatus.valueOf(entity.getMissionSlotFillStatus()),
                entity.getMissionSlotFilledByProfileId(),
                entity.getMissionSlotFilledAt());
    }

    private MissionSlotRequiredSkillEntity requiredSkillToEntity(
            MissionSlotEntity parent,
            MissionSlotRequiredSkill requiredSkill) {
        Long entityId = requiredSkill.getId().value() == 0 ? null : requiredSkill.getId().value();
        MissionSlotRequiredSkillEntity entity = new MissionSlotRequiredSkillEntity(
                entityId,
                null,
                requiredSkill.getSkillId().value(),
                resolveSkillTitle(requiredSkill.getSkillId()),
                resolveCompetencyLevelId(requiredSkill.getRequiredSkillLevel()));
        entity.setMissionSlot(parent);
        return entity;
    }

    private short toSmallint(int value) {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Value is out of SMALLINT range: " + value);
        }
        return (short) value;
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
