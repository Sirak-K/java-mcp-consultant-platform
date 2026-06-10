package mcp.server.domain.missions.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupSupport;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mission_proposal_slot", schema = "marketplace", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "mission_proposal_id", "mission_slot_number" })
}, indexes = {
        @Index(name = "idx_mission_proposal_slot_proposal_id", columnList = "mission_proposal_id"),
        @Index(name = "idx_mission_proposal_slot_role_id", columnList = "role_id")
})
public class MissionProposalSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_proposal_slot_id")
    private Long id;

    @Column(name = "mission_proposal_id", nullable = false, insertable = false, updatable = false)
    private Long missionProposalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_proposal_id", nullable = false)
    private MissionProposalEntity missionProposal;

    @Column(name = "mission_slot_number", nullable = false)
    private Integer missionSlotNumber;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "required_role_experience_years", nullable = false)
    private Short requiredRoleExperienceYears;

    @Column(name = "req_competency_level_id", nullable = false)
    private Short reqCompetencyLevelId;

    @OneToMany(mappedBy = "missionProposalSlot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MissionProposalRequiredSkillEntity> requiredSkills = new ArrayList<>();

    @OneToMany(mappedBy = "missionProposalSlot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MissionProposalSecondaryRequiredSkillEntity> secondaryRequiredSkills = new ArrayList<>();

    protected MissionProposalSlotEntity() {
    }

    public MissionProposalSlotEntity(
            Long id,
            Integer missionSlotNumber,
            Long roleId,
            Short requiredRoleExperienceYears,
            List<MissionProposalRequiredSkillEntity> requiredSkills) {
        this.id = id;
        this.missionSlotNumber = missionSlotNumber;
        this.roleId = roleId;
        this.requiredRoleExperienceYears = normalizeYears(requiredRoleExperienceYears);
        this.reqCompetencyLevelId = CompetencyLevelLookupSupport.lookupIdForYears(this.requiredRoleExperienceYears);
        this.requiredSkills = requiredSkills != null ? new ArrayList<>(requiredSkills) : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMissionProposalId() {
        return missionProposalId;
    }

    public void setMissionProposalId(Long missionProposalId) {
        this.missionProposalId = missionProposalId;
    }

    public MissionProposalEntity getMissionProposal() {
        return missionProposal;
    }

    public void setMissionProposal(MissionProposalEntity missionProposal) {
        this.missionProposal = missionProposal;
    }

    public Integer getMissionSlotNumber() {
        return missionSlotNumber;
    }

    public void setMissionSlotNumber(Integer missionSlotNumber) {
        this.missionSlotNumber = missionSlotNumber;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Short getRequiredRoleExperienceYears() {
        return requiredRoleExperienceYears;
    }

    public void setRequiredRoleExperienceYears(Short requiredRoleExperienceYears) {
        this.requiredRoleExperienceYears = normalizeYears(requiredRoleExperienceYears);
        this.reqCompetencyLevelId = CompetencyLevelLookupSupport.lookupIdForYears(this.requiredRoleExperienceYears);
    }

    public Short getReqCompetencyLevelId() {
        return reqCompetencyLevelId;
    }

    public void setReqCompetencyLevelId(Short reqCompetencyLevelId) {
        this.reqCompetencyLevelId = reqCompetencyLevelId;
    }

    public List<MissionProposalRequiredSkillEntity> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<MissionProposalRequiredSkillEntity> requiredSkills) {
        this.requiredSkills = requiredSkills != null ? new ArrayList<>(requiredSkills) : new ArrayList<>();
        this.requiredSkills.forEach(requiredSkill -> requiredSkill.setMissionProposalSlot(this));
    }

    public void replaceRequiredSkills(List<MissionProposalRequiredSkillEntity> requiredSkills) {
        this.requiredSkills.clear();
        if (requiredSkills != null) {
            requiredSkills.forEach(this::addRequiredSkill);
        }
    }

    public void addRequiredSkill(MissionProposalRequiredSkillEntity requiredSkill) {
        if (requiredSkill != null) {
            requiredSkill.setMissionProposalSlot(this);
            this.requiredSkills.add(requiredSkill);
        }
    }

    public List<MissionProposalSecondaryRequiredSkillEntity> getSecondaryRequiredSkills() {
        return secondaryRequiredSkills;
    }

    public void setSecondaryRequiredSkills(
            List<MissionProposalSecondaryRequiredSkillEntity> secondaryRequiredSkills) {
        this.secondaryRequiredSkills = secondaryRequiredSkills != null
                ? new ArrayList<>(secondaryRequiredSkills)
                : new ArrayList<>();
        this.secondaryRequiredSkills.forEach(requiredSkill -> requiredSkill.setMissionProposalSlot(this));
    }

    public void replaceSecondaryRequiredSkills(
            List<MissionProposalSecondaryRequiredSkillEntity> secondaryRequiredSkills) {
        this.secondaryRequiredSkills.clear();
        if (secondaryRequiredSkills != null) {
            secondaryRequiredSkills.forEach(this::addSecondaryRequiredSkill);
        }
    }

    public void addSecondaryRequiredSkill(MissionProposalSecondaryRequiredSkillEntity requiredSkill) {
        if (requiredSkill != null) {
            requiredSkill.setMissionProposalSlot(this);
            this.secondaryRequiredSkills.add(requiredSkill);
        }
    }

    private static Short normalizeYears(Short requiredRoleExperienceYears) {
        return (short) Math.max(0, requiredRoleExperienceYears == null ? 0 : requiredRoleExperienceYears);
    }
}
