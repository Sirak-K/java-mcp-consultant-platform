package mcp.server.domain.missions.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import mcp.server.domain.shared_kernel.exception.DomainInvariantViolationException;
import mcp.server.domain.reference_data.model.RoleId;

public final class MissionSlot {

    private final MissionSlotId id;
    private final MissionId missionId;
    private final RoleId roleId;
    private final List<MissionSlotRequiredSkill> requiredSkills;
    private final int requiredRoleExperienceYears;
    private final int missionSlotNumber;
    private MissionSlotFillStatus fillStatus;
    private Long filledByCandidateProfileId; // nullable - null when NOT_FILLED
    private Instant filledAt; // nullable - null when NOT_FILLED

    public MissionSlot(
            MissionSlotId id,
            MissionId missionId,
            RoleId roleId,
            List<MissionSlotRequiredSkill> requiredSkills,
            int requiredRoleExperienceYears,
            int missionSlotNumber,
            MissionSlotFillStatus fillStatus,
            Long filledByCandidateProfileId,
            Instant filledAt) {
        if (id == null) {
            throw new DomainInvariantViolationException("MissionSlot id must not be null");
        }
        if (missionId == null) {
            throw new DomainInvariantViolationException("MissionSlot missionId must not be null");
        }
        if (roleId == null) {
            throw new DomainInvariantViolationException("MissionSlot roleId must not be null");
        }
        if (fillStatus == null) {
            throw new DomainInvariantViolationException("MissionSlot fillStatus must not be null");
        }
        if (requiredRoleExperienceYears <= 1) {
            throw new DomainInvariantViolationException("MissionSlot requiredRoleExperienceYears must be > 1");
        }
        this.id = id;
        this.missionId = missionId;
        this.roleId = roleId;
        this.requiredSkills = requiredSkills == null ? new ArrayList<>() : new ArrayList<>(requiredSkills);
        this.requiredRoleExperienceYears = requiredRoleExperienceYears;
        this.missionSlotNumber = missionSlotNumber;
        this.fillStatus = fillStatus;
        this.filledByCandidateProfileId = filledByCandidateProfileId;
        this.filledAt = filledAt;
    }

    public MissionSlotId getId() {
        return id;
    }

    public MissionId getMissionId() {
        return missionId;
    }

    public RoleId getRoleId() {
        return roleId;
    }

    public List<MissionSlotRequiredSkill> getRequiredSkills() {
        return Collections.unmodifiableList(requiredSkills);
    }

    public int getRequiredRoleExperienceYears() {
        return requiredRoleExperienceYears;
    }

    public int getMissionSlotNumber() {
        return missionSlotNumber;
    }

    public MissionSlotFillStatus getFillStatus() {
        return fillStatus;
    }

    public Long getFilledByCandidateProfileId() {
        return filledByCandidateProfileId;
    }

    public Instant getFilledAt() {
        return filledAt;
    }

    public void addRequiredSkill(MissionSlotRequiredSkill skill) {
        if (skill == null) {
            throw new DomainInvariantViolationException("MissionSlotRequiredSkill to add must not be null");
        }
        if (!skill.getMissionSlotId().equals(this.id)) {
            throw new DomainInvariantViolationException(
                    "MissionSlotRequiredSkill missionSlotId " + skill.getMissionSlotId()
                            + " does not match MissionSlot id " + this.id);
        }
        if (requiredSkills.size() >= 5) {
            throw new DomainInvariantViolationException("MissionSlot may have at most 5 required skills");
        }
        boolean duplicate = requiredSkills.stream().anyMatch(s -> s.getSkillId().equals(skill.getSkillId()));
        if (duplicate) {
            throw new DomainInvariantViolationException(
                    "MissionSlot " + id + " already has required skill " + skill.getSkillId());
        }
        requiredSkills.add(skill);
    }

    public void removeRequiredSkillById(MissionSlotRequiredSkillId requiredSkillId) {
        if (requiredSkillId == null) {
            throw new DomainInvariantViolationException("MissionSlotRequiredSkillId to remove must not be null");
        }
        boolean removed = requiredSkills.removeIf(s -> s.getId().equals(requiredSkillId));
        if (!removed) {
            throw new DomainInvariantViolationException(
                    "MissionSlot " + id + " has no required skill with id=" + requiredSkillId);
        }
    }

    public void fill(long candidateProfileId, Instant at) {
        if (candidateProfileId <= 0) {
            throw new DomainInvariantViolationException("MissionSlot fill: candidateProfileId must be positive");
        }
        if (fillStatus == MissionSlotFillStatus.FILLED) {
            throw new DomainInvariantViolationException("MissionSlot is already FILLED");
        }
        this.fillStatus = MissionSlotFillStatus.FILLED;
        this.filledByCandidateProfileId = candidateProfileId;
        this.filledAt = at != null ? at : Instant.now();
    }

    public void unfill() {
        this.fillStatus = MissionSlotFillStatus.NOT_FILLED;
        this.filledByCandidateProfileId = null;
        this.filledAt = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MissionSlot other))
            return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MissionSlot{id=" + id + ", missionId=" + missionId
                + ", roleId=" + roleId + ", requiredSkills=" + requiredSkills.size()
                + ", missionSlotNumber=" + missionSlotNumber + ", fillStatus=" + fillStatus + "}";
    }
}
