package mcp.server.domain.candidate_profiles.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupSupport;

@Entity
@Table(name = "cand_profile_roles", schema = "marketplace", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cand_profile_role_number", columnNames = { "cand_profile_id", "role_number" })
})
public class CandidateProfileRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cand_profile_role_id")
    private Long id;

    @Column(name = "cand_profile_id", nullable = false, insertable = false, updatable = false)
    private Long candidateProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cand_profile_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cand_profile_role_profile"))
    private CandidateProfileEntity candidateProfile;

    @Column(name = "role_number", nullable = false)
    private Integer roleNumber;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_title", nullable = false, length = 150)
    private String roleTitle;

    @Column(name = "role_experience_years", nullable = false)
    private Short roleExperienceYears;

    @Column(name = "competency_level_id", nullable = false)
    private Short competencyLevelId;

    protected CandidateProfileRoleEntity() {
    }

    public CandidateProfileRoleEntity(
            Long id,
            Integer roleNumber,
            Long roleId,
            String roleTitle,
            Short roleExperienceYears) {
        this.id = id;
        this.roleNumber = roleNumber;
        this.roleId = roleId;
        this.roleTitle = roleTitle;
        this.roleExperienceYears = normalizeYears(roleExperienceYears);
        this.competencyLevelId = CompetencyLevelLookupSupport.lookupIdForYears(this.roleExperienceYears);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCandProfileId() {
        return candidateProfileId;
    }

    public void setCandProfileId(Long candidateProfileId) {
        this.candidateProfileId = candidateProfileId;
    }

    public CandidateProfileEntity getCandidateProfile() {
        return candidateProfile;
    }

    public void setCandidateProfile(CandidateProfileEntity candidateProfile) {
        this.candidateProfile = candidateProfile;
    }

    public Integer getRoleNumber() {
        return roleNumber;
    }

    public void setRoleNumber(Integer roleNumber) {
        this.roleNumber = roleNumber;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public Short getRoleExperienceYears() {
        return roleExperienceYears;
    }

    public void setRoleExperienceYears(Short roleExperienceYears) {
        this.roleExperienceYears = normalizeYears(roleExperienceYears);
        this.competencyLevelId = CompetencyLevelLookupSupport.lookupIdForYears(this.roleExperienceYears);
    }

    public Short getCompetencyLevelId() {
        return competencyLevelId;
    }

    public void setCompetencyLevelId(Short competencyLevelId) {
        this.competencyLevelId = competencyLevelId;
    }

    private static Short normalizeYears(Short roleExperienceYears) {
        return (short) Math.max(0, roleExperienceYears == null ? 0 : roleExperienceYears);
    }
}
