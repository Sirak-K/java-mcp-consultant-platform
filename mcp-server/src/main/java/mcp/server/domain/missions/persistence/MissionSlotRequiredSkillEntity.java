package mcp.server.domain.missions.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "mission_slot_required_skill", schema = "marketplace", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "mission_slot_id", "primary_skill_id" })
})
public class MissionSlotRequiredSkillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_slot_required_skill_id")
    private Long missionSlotRequiredSkillId;

    @Column(name = "mission_slot_id", nullable = false, insertable = false, updatable = false)
    private Long missionSlotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_slot_id", nullable = false)
    private MissionSlotEntity missionSlot;

    @Column(name = "primary_skill_id", nullable = false)
    private Long skillId;

    @Column(name = "skill_title", nullable = false, length = 150)
    private String skillTitle;

    @Column(name = "req_competency_level_id", nullable = false)
    private Short requiredSkillLevelId;

    protected MissionSlotRequiredSkillEntity() {
    }

    public MissionSlotRequiredSkillEntity(
            Long missionSlotRequiredSkillId,
            Long skillId,
            String skillTitle,
            Short requiredSkillLevelId) {
        this(missionSlotRequiredSkillId, null, skillId, skillTitle, requiredSkillLevelId);
    }

    public MissionSlotRequiredSkillEntity(
            Long missionSlotRequiredSkillId,
            Long missionSlotId,
            Long skillId,
            String skillTitle,
            Short requiredSkillLevelId) {
        this.missionSlotRequiredSkillId = missionSlotRequiredSkillId;
        this.missionSlotId = missionSlotId;
        this.skillId = skillId;
        this.skillTitle = skillTitle;
        this.requiredSkillLevelId = requiredSkillLevelId;
    }

    public Long getMissionSlotRequiredSkillId() {
        return missionSlotRequiredSkillId;
    }

    public void setMissionSlotRequiredSkillId(Long missionSlotRequiredSkillId) {
        this.missionSlotRequiredSkillId = missionSlotRequiredSkillId;
    }

    public Long getMissionSlotId() {
        return missionSlotId;
    }

    public void setMissionSlotId(Long missionSlotId) {
        this.missionSlotId = missionSlotId;
    }

    public MissionSlotEntity getMissionSlot() {
        return missionSlot;
    }

    public void setMissionSlot(MissionSlotEntity missionSlot) {
        this.missionSlot = missionSlot;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public String getSkillTitle() {
        return skillTitle;
    }

    public void setSkillTitle(String skillTitle) {
        this.skillTitle = skillTitle;
    }

    public Short getRequiredSkillLevelId() {
        return requiredSkillLevelId;
    }

    public void setRequiredSkillLevelId(Short requiredSkillLevelId) {
        this.requiredSkillLevelId = requiredSkillLevelId;
    }

}
