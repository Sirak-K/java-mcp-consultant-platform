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
@Table(name = "mission_proposal_slot_secondary_required_skill", schema = "consultant_platform", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "mission_proposal_slot_id", "secondary_skill_id" })
})
public class MissionProposalSecondaryRequiredSkillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_proposal_slot_secondary_required_skill_id")
    private Long id;

    @Column(name = "mission_proposal_slot_id", nullable = false, insertable = false, updatable = false)
    private Long missionProposalSlotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_proposal_slot_id", nullable = false)
    private MissionProposalSlotEntity missionProposalSlot;

    @Column(name = "secondary_skill_id", nullable = false)
    private Long skillId;

    @Column(name = "req_competency_level_id", nullable = false)
    private Short requiredSkillLevelId;

    protected MissionProposalSecondaryRequiredSkillEntity() {
    }

    public MissionProposalSecondaryRequiredSkillEntity(
            Long id,
            Long skillId,
            Short requiredSkillLevelId) {
        this.id = id;
        this.skillId = skillId;
        this.requiredSkillLevelId = requiredSkillLevelId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMissionProposalSlotId() {
        return missionProposalSlotId;
    }

    public void setMissionProposalSlotId(Long missionProposalSlotId) {
        this.missionProposalSlotId = missionProposalSlotId;
    }

    public MissionProposalSlotEntity getMissionProposalSlot() {
        return missionProposalSlot;
    }

    public void setMissionProposalSlot(MissionProposalSlotEntity missionProposalSlot) {
        this.missionProposalSlot = missionProposalSlot;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public Short getRequiredSkillLevelId() {
        return requiredSkillLevelId;
    }

    public void setRequiredSkillLevelId(Short requiredSkillLevelId) {
        this.requiredSkillLevelId = requiredSkillLevelId;
    }
}
