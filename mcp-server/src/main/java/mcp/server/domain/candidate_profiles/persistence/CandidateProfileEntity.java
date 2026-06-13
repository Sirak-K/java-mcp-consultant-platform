package mcp.server.domain.candidate_profiles.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Live, editable and matchable candidate profile aggregate.
 */
@Entity
@Table(name = "cand_profile", schema = "consultant_platform", indexes = {
        @Index(name = "idx_cand_profile_created_at", columnList = "created_at")
})
public class CandidateProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cand_profile_id")
    private Long id;

    @Column(name = "contact_email", nullable = false, length = 150)
    private String contactEmail;

    @Column(name = "candidate_first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "candidate_last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "candidate_phone_number", nullable = false, length = 100)
    private String phoneNumber;

    @Column(name = "candidate_country", nullable = false, length = 100)
    private String country;

    @Column(name = "candidate_city", nullable = false, length = 100)
    private String city;

    @Column(name = "candidate_work_status", nullable = false)
    private String workStatus;

    @Column(name = "candidate_languages", nullable = false)
    private String languages;

    @Column(name = "cand_role_title", nullable = false, length = 150)
    private String roleTitle;

    @Column(name = "candidate_profile_summary", nullable = false)
    private String profileSummary;

    @Column(name = "candidate_years_of_experience", nullable = false, length = 20)
    private String yearsOfExperience;

    @Column(name = "candidate_expected_salary", nullable = false, length = 50)
    private String expectedSalary;

    @Column(name = "candidate_hourly_rate", nullable = false, length = 50)
    private String hourlyRate;

    @Column(name = "cand_skills", nullable = false)
    private String skills;

    @Column(name = "candidate_work_mode", nullable = false, length = 50)
    private String workMode;

    @Column(name = "candidate_location_flexibility", nullable = false, length = 100)
    private String locationFlexibility;

    @Column(name = "candidate_preferred_location", nullable = false, length = 150)
    private String preferredLocation;

    @Column(name = "candidate_willing_to_relocate", nullable = false)
    private Boolean willingToRelocate;

    @Column(name = "candidate_gdpr_consent", nullable = false)
    private Boolean gdprConsent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private CandidateProfileCvEntity cv = defaultCv();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CandidateProfileWorkExperienceEntity> workExperiences = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CandidateProfileEducationEntity> educations = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CandidateProfileRoleEntity> candidateRoles = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CandidateProfilePrimarySkillEntity> primarySkills = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CandidateProfileSecondarySkillEntity> secondarySkills = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CandidateProfileCertificationEntity> certifications = new ArrayList<>();

    protected CandidateProfileEntity() {
    }

    public CandidateProfileEntity(
            Long id,
            String contactEmail,
            String cvFileName,
            String cvContentType,
            Long cvSizeBytes,
            Boolean cvExtractionPending,
            String cvExtractionStatus,
            String cvExtractedText,
            String cvExtractionError,
            Instant cvExtractedAt,
            String firstName,
            String lastName,
            String phoneNumber,
            String country,
            String city,
            String workStatus,
            String languages,
            String roleTitle,
            String profileSummary,
            String yearsOfExperience,
            String expectedSalary,
            String hourlyRate,
            String skills,
            String workMode,
            String locationFlexibility,
            String preferredLocation,
            Boolean willingToRelocate,
            Boolean gdprConsent,
            String cvSummaryStatus,
            String cvSummaryCoreCompetenceOverview,
            String cvSummaryLocation,
            String cvSummaryOtherDetails,
            Instant cvSummaryGeneratedAt,
            Instant createdAt,
            Instant updatedAt,
            List<CandidateProfileWorkExperienceEntity> workExperiences,
            List<CandidateProfileEducationEntity> educations,
            List<CandidateProfileRoleEntity> candidateRoles,
            List<CandidateProfilePrimarySkillEntity> primarySkills,
            List<CandidateProfileSecondarySkillEntity> secondarySkills,
            List<CandidateProfileCertificationEntity> certifications) {
        this.id = id;
        this.contactEmail = contactEmail;
        this.cv = new CandidateProfileCvEntity(
                null,
                cvFileName,
                cvContentType,
                cvSizeBytes,
                cvExtractionPending,
                cvExtractionStatus,
                cvExtractedText,
                cvExtractionError,
                cvExtractedAt,
                cvSummaryStatus,
                cvSummaryCoreCompetenceOverview,
                cvSummaryLocation,
                cvSummaryOtherDetails,
                cvSummaryGeneratedAt);
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.country = country;
        this.city = city;
        this.workStatus = workStatus;
        this.languages = languages;
        this.roleTitle = roleTitle;
        this.profileSummary = profileSummary;
        this.yearsOfExperience = yearsOfExperience;
        this.expectedSalary = expectedSalary;
        this.hourlyRate = hourlyRate;
        this.skills = skills;
        this.workMode = workMode;
        this.locationFlexibility = locationFlexibility;
        this.preferredLocation = preferredLocation;
        this.willingToRelocate = willingToRelocate;
        this.gdprConsent = gdprConsent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.workExperiences = workExperiences != null ? new ArrayList<>(workExperiences) : new ArrayList<>();
        this.educations = educations != null ? new ArrayList<>(educations) : new ArrayList<>();
        this.candidateRoles = candidateRoles != null ? new ArrayList<>(candidateRoles) : new ArrayList<>();
        this.primarySkills = primarySkills != null ? new ArrayList<>(primarySkills) : new ArrayList<>();
        this.secondarySkills = secondarySkills != null ? new ArrayList<>(secondarySkills) : new ArrayList<>();
        this.certifications = certifications != null ? new ArrayList<>(certifications) : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public CandidateProfileCvEntity getCv() {
        return ensureCv();
    }

    public void setCv(CandidateProfileCvEntity cv) {
        this.cv = cv;
        if (this.cv != null) {
            this.cv.setCandidateProfile(this);
        }
    }

    public String getCvFileName() {
        return ensureCv().getCvFileName();
    }

    public void setCvFileName(String cvFileName) {
        ensureCv().setCvFileName(cvFileName);
    }

    public String getCvContentType() {
        return ensureCv().getCvContentType();
    }

    public void setCvContentType(String cvContentType) {
        ensureCv().setCvContentType(cvContentType);
    }

    public Long getCvSizeBytes() {
        return ensureCv().getCvSizeBytes();
    }

    public void setCvSizeBytes(Long cvSizeBytes) {
        ensureCv().setCvSizeBytes(cvSizeBytes);
    }

    public Boolean getCvExtractionPending() {
        return ensureCv().getCvExtractionPending();
    }

    public void setCvExtractionPending(Boolean cvExtractionPending) {
        ensureCv().setCvExtractionPending(cvExtractionPending);
    }

    public String getCvExtractionStatus() {
        return ensureCv().getCvExtractionStatus();
    }

    public void setCvExtractionStatus(String cvExtractionStatus) {
        ensureCv().setCvExtractionStatus(cvExtractionStatus);
    }

    public String getCvExtractedText() {
        return ensureCv().getCvExtractedText();
    }

    public void setCvExtractedText(String cvExtractedText) {
        ensureCv().setCvExtractedText(cvExtractedText);
    }

    public String getCvExtractionError() {
        return ensureCv().getCvExtractionError();
    }

    public void setCvExtractionError(String cvExtractionError) {
        ensureCv().setCvExtractionError(cvExtractionError);
    }

    public Instant getCvExtractedAt() {
        return ensureCv().getCvExtractedAt();
    }

    public void setCvExtractedAt(Instant cvExtractedAt) {
        ensureCv().setCvExtractedAt(cvExtractedAt);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(String workStatus) {
        this.workStatus = workStatus;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public String getProfileSummary() {
        return profileSummary;
    }

    public void setProfileSummary(String profileSummary) {
        this.profileSummary = profileSummary;
    }

    public String getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(String yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getExpectedSalary() {
        return expectedSalary;
    }

    public void setExpectedSalary(String expectedSalary) {
        this.expectedSalary = expectedSalary;
    }

    public String getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(String hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    public String getLocationFlexibility() {
        return locationFlexibility;
    }

    public void setLocationFlexibility(String locationFlexibility) {
        this.locationFlexibility = locationFlexibility;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public Boolean getWillingToRelocate() {
        return willingToRelocate;
    }

    public void setWillingToRelocate(Boolean willingToRelocate) {
        this.willingToRelocate = willingToRelocate;
    }

    public Boolean getGdprConsent() {
        return gdprConsent;
    }

    public void setGdprConsent(Boolean gdprConsent) {
        this.gdprConsent = gdprConsent;
    }

    public String getCvSummaryStatus() {
        return ensureCv().getCvSummaryStatus();
    }

    public void setCvSummaryStatus(String cvSummaryStatus) {
        ensureCv().setCvSummaryStatus(cvSummaryStatus);
    }

    public String getCvSummaryCoreCompetenceOverview() {
        return ensureCv().getCvSummaryCoreCompetenceOverview();
    }

    public void setCvSummaryCoreCompetenceOverview(String cvSummaryCoreCompetenceOverview) {
        ensureCv().setCvSummaryCoreCompetenceOverview(cvSummaryCoreCompetenceOverview);
    }

    public String getCvSummaryLocation() {
        return ensureCv().getCvSummaryLocation();
    }

    public void setCvSummaryLocation(String cvSummaryLocation) {
        ensureCv().setCvSummaryLocation(cvSummaryLocation);
    }

    public String getCvSummaryOtherDetails() {
        return ensureCv().getCvSummaryOtherDetails();
    }

    public void setCvSummaryOtherDetails(String cvSummaryOtherDetails) {
        ensureCv().setCvSummaryOtherDetails(cvSummaryOtherDetails);
    }

    public Instant getCvSummaryGeneratedAt() {
        return ensureCv().getCvSummaryGeneratedAt();
    }

    public void setCvSummaryGeneratedAt(Instant cvSummaryGeneratedAt) {
        ensureCv().setCvSummaryGeneratedAt(cvSummaryGeneratedAt);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CandidateProfileWorkExperienceEntity> getWorkExperiences() {
        return workExperiences;
    }

    public void setWorkExperiences(List<CandidateProfileWorkExperienceEntity> workExperiences) {
        this.workExperiences = workExperiences != null ? new ArrayList<>(workExperiences) : new ArrayList<>();
        this.workExperiences.forEach(workExperience -> workExperience.setCandidateProfile(this));
    }

    public void replaceWorkExperiences(List<CandidateProfileWorkExperienceEntity> workExperiences) {
        this.workExperiences.clear();
        if (workExperiences != null) {
            workExperiences.forEach(this::addWorkExperience);
        }
    }

    public void addWorkExperience(CandidateProfileWorkExperienceEntity workExperience) {
        if (workExperience != null) {
            workExperience.setCandidateProfile(this);
            this.workExperiences.add(workExperience);
        }
    }

    public List<CandidateProfileEducationEntity> getEducations() {
        return educations;
    }

    public void setEducations(List<CandidateProfileEducationEntity> educations) {
        this.educations = educations != null ? new ArrayList<>(educations) : new ArrayList<>();
        this.educations.forEach(education -> education.setCandidateProfile(this));
    }

    public void replaceEducations(List<CandidateProfileEducationEntity> educations) {
        this.educations.clear();
        if (educations != null) {
            educations.forEach(this::addEducation);
        }
    }

    public void addEducation(CandidateProfileEducationEntity education) {
        if (education != null) {
            education.setCandidateProfile(this);
            this.educations.add(education);
        }
    }

    public List<CandidateProfileRoleEntity> getCandidateRoles() {
        return candidateRoles;
    }

    public void setCandidateRoles(List<CandidateProfileRoleEntity> candidateRoles) {
        this.candidateRoles = candidateRoles != null ? new ArrayList<>(candidateRoles) : new ArrayList<>();
        this.candidateRoles.forEach(role -> role.setCandidateProfile(this));
    }

    public void replaceCandidateRoles(List<CandidateProfileRoleEntity> candidateRoles) {
        this.candidateRoles.clear();
        if (candidateRoles != null) {
            candidateRoles.forEach(this::addCandidateRole);
        }
    }

    public void addCandidateRole(CandidateProfileRoleEntity candidateRole) {
        if (candidateRole != null) {
            candidateRole.setCandidateProfile(this);
            this.candidateRoles.add(candidateRole);
        }
    }

    public List<CandidateProfilePrimarySkillEntity> getPrimarySkills() {
        return primarySkills;
    }

    public void setPrimarySkills(List<CandidateProfilePrimarySkillEntity> primarySkills) {
        this.primarySkills = primarySkills != null ? new ArrayList<>(primarySkills) : new ArrayList<>();
        this.primarySkills.forEach(skill -> skill.setCandidateProfile(this));
    }

    public void replacePrimarySkills(List<CandidateProfilePrimarySkillEntity> primarySkills) {
        this.primarySkills.clear();
        if (primarySkills != null) {
            primarySkills.forEach(this::addPrimarySkill);
        }
    }

    public void addPrimarySkill(CandidateProfilePrimarySkillEntity candidateSkill) {
        if (candidateSkill != null) {
            candidateSkill.setCandidateProfile(this);
            this.primarySkills.add(candidateSkill);
        }
    }

    public List<CandidateProfileSecondarySkillEntity> getSecondarySkills() {
        return secondarySkills;
    }

    public void setSecondarySkills(List<CandidateProfileSecondarySkillEntity> secondarySkills) {
        this.secondarySkills = secondarySkills != null ? new ArrayList<>(secondarySkills) : new ArrayList<>();
        this.secondarySkills.forEach(skill -> skill.setCandidateProfile(this));
    }

    public void replaceSecondarySkills(List<CandidateProfileSecondarySkillEntity> secondarySkills) {
        this.secondarySkills.clear();
        if (secondarySkills != null) {
            secondarySkills.forEach(this::addSecondarySkill);
        }
    }

    public void addSecondarySkill(CandidateProfileSecondarySkillEntity secondarySkill) {
        if (secondarySkill != null) {
            secondarySkill.setCandidateProfile(this);
            this.secondarySkills.add(secondarySkill);
        }
    }

    public List<CandidateProfileCertificationEntity> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<CandidateProfileCertificationEntity> certifications) {
        this.certifications = certifications != null ? new ArrayList<>(certifications) : new ArrayList<>();
        this.certifications.forEach(certification -> certification.setCandidateProfile(this));
    }

    public void replaceCertifications(List<CandidateProfileCertificationEntity> certifications) {
        this.certifications.clear();
        if (certifications != null) {
            certifications.forEach(this::addCertification);
        }
    }

    public void addCertification(CandidateProfileCertificationEntity certification) {
        if (certification != null) {
            certification.setCandidateProfile(this);
            this.certifications.add(certification);
        }
    }

    private CandidateProfileCvEntity ensureCv() {
        if (cv == null) {
            cv = defaultCv();
            cv.setCandidateProfile(this);
        }
        return cv;
    }

    private static CandidateProfileCvEntity defaultCv() {
        return new CandidateProfileCvEntity(
                null,
                "",
                "application/octet-stream",
                null,
                true,
                "METADATA_ONLY",
                "",
                "",
                null,
                "NOT_GENERATED",
                "",
                "",
                "",
                null);
    }

    @PrePersist
    @PreUpdate
    private void prepareForSave() {
        ensureCv().setCandidateProfile(this);
    }
}
