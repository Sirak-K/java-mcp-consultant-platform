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
@Table(name = "cand_profile_work_experiences", schema = "consultant_platform", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cand_profile_work_experience", columnNames = { "cand_profile_id",
                "work_experience_number" })
}, indexes = {
        @Index(name = "idx_cand_profile_work_experiences_profile_id", columnList = "cand_profile_id")
})
public class CandidateProfileWorkExperienceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cand_profile_work_experience_id")
    private Long id;

    @Column(name = "cand_profile_id", nullable = false, insertable = false, updatable = false)
    private Long candidateProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cand_profile_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cand_profile_work_experience_profile"))
    private CandidateProfileEntity candidateProfile;

    @Column(name = "work_experience_number", nullable = false)
    private Integer workExperienceNumber;

    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    @Column(name = "work_exp_company", nullable = false, length = 200)
    private String workExpCompany;

    @Column(name = "work_exp_company_org_nr", nullable = false, length = 30)
    private String workExpCompanyOrgNr;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "currently_here", nullable = false)
    private Boolean currentlyHere;

    protected CandidateProfileWorkExperienceEntity() {
    }

    public CandidateProfileWorkExperienceEntity(
            Long id,
            Integer workExperienceNumber,
            String jobTitle,
            String workExpCompany,
            String workExpCompanyOrgNr,
            String city,
            String country,
            LocalDate startDate,
            LocalDate endDate,
            Boolean currentlyHere) {
        this.id = id;
        this.workExperienceNumber = workExperienceNumber;
        this.jobTitle = jobTitle;
        this.workExpCompany = workExpCompany;
        this.workExpCompanyOrgNr = workExpCompanyOrgNr;
        this.city = city;
        this.country = country;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentlyHere = currentlyHere;
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

    public Integer getWorkExperienceNumber() {
        return workExperienceNumber;
    }

    public void setWorkExperienceNumber(Integer workExperienceNumber) {
        this.workExperienceNumber = workExperienceNumber;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getWorkExpCompany() {
        return workExpCompany;
    }

    public void setWorkExpCompany(String workExpCompany) {
        this.workExpCompany = workExpCompany;
    }

    public String getWorkExpCompanyOrgNr() {
        return workExpCompanyOrgNr;
    }

    public void setWorkExpCompanyOrgNr(String workExpCompanyOrgNr) {
        this.workExpCompanyOrgNr = workExpCompanyOrgNr;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public Boolean getCurrentlyHere() {
        return currentlyHere;
    }

    public void setCurrentlyHere(Boolean currentlyHere) {
        this.currentlyHere = currentlyHere;
    }
}
