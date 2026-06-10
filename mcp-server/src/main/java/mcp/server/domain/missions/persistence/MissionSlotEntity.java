package mcp.server.domain.missions.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;

import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupSupport;

@Entity
@Table(name = "mission_slot", schema = "marketplace")
public class MissionSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_slot_id")
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "required_role_experience_years", nullable = false)
    private Short requiredRoleExperienceYears;

    @Column(name = "req_competency_level_id", nullable = false)
    private Short reqCompetencyLevelId;

    @Column(name = "mission_slot_number", nullable = false)
    private Integer missionSlotNumber;

    @Column(name = "mission_slot_fill_status", nullable = false, length = 20)
    private String missionSlotFillStatus;

    @Column(name = "mission_slot_filled_by_profile_id")
    private Long missionSlotFilledByProfileId;

    @Column(name = "mission_slot_filled_at")
    private Instant missionSlotFilledAt;

    @OneToMany(mappedBy = "missionSlot", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<MissionSlotRequiredSkillEntity> requiredSkills = new ArrayList<>();

    @OneToMany(mappedBy = "missionSlot", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<MissionSlotSecondaryRequiredSkillEntity> secondaryRequiredSkills = new ArrayList<>();

    protected MissionSlotEntity() {
    }

    public MissionSlotEntity(
            Long id,
            Long missionId,
            Long roleId,
            Short requiredRoleExperienceYears,
            Integer missionSlotNumber,
            String missionSlotFillStatus,
            Long missionSlotFilledByProfileId,
            Instant missionSlotFilledAt,
            List<MissionSlotRequiredSkillEntity> requiredSkills) {
        this.id = id;
        this.missionId = missionId;
        this.roleId = roleId;
        this.requiredRoleExperienceYears = normalizeYears(requiredRoleExperienceYears);
        this.reqCompetencyLevelId = CompetencyLevelLookupSupport.lookupIdForYears(this.requiredRoleExperienceYears);
        this.missionSlotNumber = missionSlotNumber;
        this.missionSlotFillStatus = missionSlotFillStatus;
        this.missionSlotFilledByProfileId = missionSlotFilledByProfileId;
        this.missionSlotFilledAt = missionSlotFilledAt;
        this.requiredSkills = requiredSkills != null ? new ArrayList<>(requiredSkills) : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
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

    public Integer getMissionSlotNumber() {
        return missionSlotNumber;
    }

    public void setMissionSlotNumber(Integer missionSlotNumber) {
        this.missionSlotNumber = missionSlotNumber;
    }

    public String getMissionSlotFillStatus() {
        return missionSlotFillStatus;
    }

    public void setMissionSlotFillStatus(String missionSlotFillStatus) {
        this.missionSlotFillStatus = missionSlotFillStatus;
    }

    public Long getMissionSlotFilledByProfileId() {
        return missionSlotFilledByProfileId;
    }

    public void setMissionSlotFilledByProfileId(Long missionSlotFilledByProfileId) {
        this.missionSlotFilledByProfileId = missionSlotFilledByProfileId;
    }

    public Instant getMissionSlotFilledAt() {
        return missionSlotFilledAt;
    }

    public void setMissionSlotFilledAt(Instant missionSlotFilledAt) {
        this.missionSlotFilledAt = missionSlotFilledAt;
    }

    public List<MissionSlotRequiredSkillEntity> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<MissionSlotRequiredSkillEntity> requiredSkills) {
        this.requiredSkills = requiredSkills != null ? new ArrayList<>(requiredSkills) : new ArrayList<>();
        this.requiredSkills.forEach(requiredSkill -> requiredSkill.setMissionSlot(this));
    }

    public void replaceRequiredSkills(List<MissionSlotRequiredSkillEntity> requiredSkills) {
        this.requiredSkills.clear();
        if (requiredSkills != null) {
            requiredSkills.forEach(this::addRequiredSkill);
        }
    }

    public void addRequiredSkill(MissionSlotRequiredSkillEntity requiredSkill) {
        if (requiredSkill != null) {
            requiredSkill.setMissionSlot(this);
            this.requiredSkills.add(requiredSkill);
        }
    }

    public List<MissionSlotSecondaryRequiredSkillEntity> getSecondaryRequiredSkills() {
        return secondaryRequiredSkills;
    }

    public void setSecondaryRequiredSkills(List<MissionSlotSecondaryRequiredSkillEntity> secondaryRequiredSkills) {
        this.secondaryRequiredSkills = secondaryRequiredSkills != null ? new ArrayList<>(secondaryRequiredSkills)
                : new ArrayList<>();
        this.secondaryRequiredSkills.forEach(requiredSkill -> requiredSkill.setMissionSlot(this));
    }

    public void replaceSecondaryRequiredSkills(List<MissionSlotSecondaryRequiredSkillEntity> secondaryRequiredSkills) {
        this.secondaryRequiredSkills.clear();
        if (secondaryRequiredSkills != null) {
            secondaryRequiredSkills.forEach(this::addSecondaryRequiredSkill);
        }
    }

    public void addSecondaryRequiredSkill(MissionSlotSecondaryRequiredSkillEntity requiredSkill) {
        if (requiredSkill != null) {
            requiredSkill.setMissionSlot(this);
            this.secondaryRequiredSkills.add(requiredSkill);
        }
    }

    private static Short normalizeYears(Short requiredRoleExperienceYears) {
        return (short) Math.max(0, requiredRoleExperienceYears == null ? 0 : requiredRoleExperienceYears);
    }
}
