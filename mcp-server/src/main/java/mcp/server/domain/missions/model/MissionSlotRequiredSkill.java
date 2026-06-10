package mcp.server.domain.missions.model;

import java.util.Objects;

import mcp.server.domain.shared_kernel.exception.DomainInvariantViolationException;
import mcp.server.domain.reference_data.model.SkillId;
import mcp.server.domain.reference_data.model.SkillLevel;

public final class MissionSlotRequiredSkill {

    private final MissionSlotRequiredSkillId id;
    private final MissionSlotId missionSlotId;
    private final SkillId skillId;
    private final SkillLevel requiredSkillLevel;

    public MissionSlotRequiredSkill(
            MissionSlotRequiredSkillId id,
            MissionSlotId missionSlotId,
            SkillId skillId,
            SkillLevel requiredSkillLevel) {
        if (id == null) {
            throw new DomainInvariantViolationException("MissionSlotRequiredSkill id must not be null");
        }
        if (missionSlotId == null) {
            throw new DomainInvariantViolationException("MissionSlotRequiredSkill missionSlotId must not be null");
        }
        if (skillId == null) {
            throw new DomainInvariantViolationException("MissionSlotRequiredSkill skillId must not be null");
        }
        if (requiredSkillLevel == null) {
            throw new DomainInvariantViolationException("MissionSlotRequiredSkill requiredSkillLevel must not be null");
        }
        this.id = id;
        this.missionSlotId = missionSlotId;
        this.skillId = skillId;
        this.requiredSkillLevel = requiredSkillLevel;
    }

    public MissionSlotRequiredSkillId getId() {
        return id;
    }

    public MissionSlotId getMissionSlotId() {
        return missionSlotId;
    }

    public SkillId getSkillId() {
        return skillId;
    }

    public SkillLevel getRequiredSkillLevel() {
        return requiredSkillLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MissionSlotRequiredSkill other))
            return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
