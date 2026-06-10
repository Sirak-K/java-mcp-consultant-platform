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

import java.time.LocalDate;

@Entity
@Table(name = "cand_profile_educations", schema = "marketplace", uniqueConstraints = {
                @UniqueConstraint(name = "uk_cand_profile_education", columnNames = { "cand_profile_id",
                                "education_number" })
}, indexes = {
                @Index(name = "idx_cand_profile_educations_profile_id", columnList = "cand_profile_id")
})
public class CandidateProfileEducationEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "cand_profile_education_id")
        private Long id;

        @Column(name = "cand_profile_id", nullable = false, insertable = false, updatable = false)
        private Long candidateProfileId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "cand_profile_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cand_profile_education_profile"))
        private CandidateProfileEntity candidateProfile;

        @Column(name = "education_number", nullable = false)
        private Integer educationNumber;

        @Column(name = "institution", nullable = false, length = 150)
        private String institution;

        @Column(name = "field_of_study", nullable = false, length = 150)
        private String fieldOfStudy;

        @Column(name = "start_date")
        private LocalDate startDate;

        @Column(name = "end_date")
        private LocalDate endDate;

        @Column(name = "currently_studying", nullable = false)
        private Boolean currentlyStudying;

        protected CandidateProfileEducationEntity() {
        }

        public CandidateProfileEducationEntity(
                        Long id,
                        Integer educationNumber,
                        String institution,
                        String fieldOfStudy,
                        LocalDate startDate,
                        LocalDate endDate,
                        Boolean currentlyStudying) {
                this.id = id;
                this.educationNumber = educationNumber;
                this.institution = institution;
                this.fieldOfStudy = fieldOfStudy;
                this.startDate = startDate;
                this.endDate = endDate;
                this.currentlyStudying = currentlyStudying;
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

        public Integer getEducationNumber() {
                return educationNumber;
        }

        public void setEducationNumber(Integer educationNumber) {
                this.educationNumber = educationNumber;
        }

        public String getInstitution() {
                return institution;
        }

        public void setInstitution(String institution) {
                this.institution = institution;
        }

        public String getFieldOfStudy() {
                return fieldOfStudy;
        }

        public void setFieldOfStudy(String fieldOfStudy) {
                this.fieldOfStudy = fieldOfStudy;
        }

        public LocalDate getStartDate() {
                return startDate;
        }

        public void setStartDate(LocalDate startDate) {
                this.startDate = startDate;
        }

        public LocalDate getEndDate() {
                return endDate;
        }

        public void setEndDate(LocalDate endDate) {
                this.endDate = endDate;
        }

        public Boolean getCurrentlyStudying() {
                return currentlyStudying;
        }

        public void setCurrentlyStudying(Boolean currentlyStudying) {
                this.currentlyStudying = currentlyStudying;
        }
}
