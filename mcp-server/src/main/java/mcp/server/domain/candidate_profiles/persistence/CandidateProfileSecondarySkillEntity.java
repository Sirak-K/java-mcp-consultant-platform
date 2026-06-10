package mcp.server.domain.candidate_profiles.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "cand_profile_skills_secondary", schema = "marketplace", uniqueConstraints = {
                @UniqueConstraint(name = "uq_cand_profile_secondary_skill_number", columnNames = { "cand_profile_id",
                                "skill_number" }),
                @UniqueConstraint(name = "uq_cand_profile_secondary_skill_id", columnNames = { "cand_profile_id",
                                "secondary_skill_id" })
}, indexes = {
                @Index(name = "idx_cand_profile_skills_secondary_profile_id", columnList = "cand_profile_id"),
                @Index(name = "idx_cand_profile_skills_secondary_skill_id", columnList = "secondary_skill_id")
})
public class CandidateProfileSecondarySkillEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "cand_profile_secondary_skill_id")
        private Long id;

        @Column(name = "cand_profile_id", nullable = false, insertable = false, updatable = false)
        private Long candidateProfileId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "cand_profile_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cand_profile_secondary_skill_profile"))
        private CandidateProfileEntity candidateProfile;

        @Column(name = "skill_number", nullable = false)
        private Integer skillNumber;

        @Column(name = "secondary_skill_id", nullable = false)
        private Long skillId;

        @Column(name = "skill_title", nullable = false, length = 150)
        private String skillTitle;

        @Column(name = "competency_level_id", nullable = false)
        private Short skillLevelId;

        @Formula("(select cl.competency_level_name from marketplace.competency_level_lookup cl where cl.competency_level_lookup_id = competency_level_id)")
        private String skillLevelName;

        protected CandidateProfileSecondarySkillEntity() {
        }

        public CandidateProfileSecondarySkillEntity(
                        Long id,
                        Integer skillNumber,
                        Long skillId,
                        String skillTitle,
                        Short skillLevelId,
                        String skillLevelName) {
                this.id = id;
                this.skillNumber = skillNumber;
                this.skillId = skillId;
                this.skillTitle = skillTitle;
                this.skillLevelId = skillLevelId;
                this.skillLevelName = skillLevelName;
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

        public Integer getSkillNumber() {
                return skillNumber;
        }

        public void setSkillNumber(Integer skillNumber) {
                this.skillNumber = skillNumber;
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

        public Short getSkillLevelId() {
                return skillLevelId;
        }

        public void setSkillLevelId(Short skillLevelId) {
                this.skillLevelId = skillLevelId;
        }

        public String getSkillLevelName() {
                return skillLevelName;
        }

        public String getCompetencyLevelName() {
                return skillLevelName;
        }

        public void setSkillLevelName(String skillLevelName) {
                this.skillLevelName = skillLevelName;
        }
}
