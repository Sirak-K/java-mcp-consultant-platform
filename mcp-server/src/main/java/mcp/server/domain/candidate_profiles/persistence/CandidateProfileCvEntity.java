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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "cand_profile_cv", schema = "marketplace", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cand_profile_cv_profile", columnNames = "cand_profile_id")
}, indexes = {
        @Index(name = "idx_cand_profile_cv_profile_id", columnList = "cand_profile_id"),
        @Index(name = "idx_cand_profile_cv_extraction_status", columnList = "cv_extraction_status")
})
public class CandidateProfileCvEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cand_profile_cv_id")
    private Long id;

    @Column(name = "cand_profile_id", nullable = false, insertable = false, updatable = false)
    private Long candidateProfileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cand_profile_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cand_profile_cv_profile"))
    private CandidateProfileEntity candidateProfile;

    @Column(name = "cv_file_name", nullable = false, length = 255)
    private String cvFileName;

    @Column(name = "cv_content_type", nullable = false, length = 100)
    private String cvContentType;

    @Column(name = "cv_size_bytes")
    private Long cvSizeBytes;

    @Column(name = "cv_extraction_pending", nullable = false)
    private Boolean cvExtractionPending;

    @Column(name = "cv_extraction_status", nullable = false, length = 40)
    private String cvExtractionStatus;

    @Column(name = "cv_extracted_text", nullable = false)
    private String cvExtractedText;

    @Column(name = "cv_extraction_error", nullable = false)
    private String cvExtractionError;

    @Column(name = "cv_extracted_at")
    private Instant cvExtractedAt;

    @Column(name = "cv_summary_status", nullable = false, length = 20)
    private String cvSummaryStatus;

    @Column(name = "cv_summary_core_competence_overview", nullable = false)
    private String cvSummaryCoreCompetenceOverview;

    @Column(name = "cv_summary_location", nullable = false)
    private String cvSummaryLocation;

    @Column(name = "cv_summary_other_details", nullable = false)
    private String cvSummaryOtherDetails;

    @Column(name = "cv_summary_generated_at")
    private Instant cvSummaryGeneratedAt;

    protected CandidateProfileCvEntity() {
    }

    public CandidateProfileCvEntity(
            Long id,
            String cvFileName,
            String cvContentType,
            Long cvSizeBytes,
            Boolean cvExtractionPending,
            String cvExtractionStatus,
            String cvExtractedText,
            String cvExtractionError,
            Instant cvExtractedAt,
            String cvSummaryStatus,
            String cvSummaryCoreCompetenceOverview,
            String cvSummaryLocation,
            String cvSummaryOtherDetails,
            Instant cvSummaryGeneratedAt) {
        this.id = id;
        this.cvFileName = cvFileName;
        this.cvContentType = cvContentType;
        this.cvSizeBytes = cvSizeBytes;
        this.cvExtractionPending = cvExtractionPending;
        this.cvExtractionStatus = cvExtractionStatus;
        this.cvExtractedText = cvExtractedText;
        this.cvExtractionError = cvExtractionError;
        this.cvExtractedAt = cvExtractedAt;
        this.cvSummaryStatus = cvSummaryStatus;
        this.cvSummaryCoreCompetenceOverview = cvSummaryCoreCompetenceOverview;
        this.cvSummaryLocation = cvSummaryLocation;
        this.cvSummaryOtherDetails = cvSummaryOtherDetails;
        this.cvSummaryGeneratedAt = cvSummaryGeneratedAt;
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

    public String getCvFileName() {
        return cvFileName;
    }

    public void setCvFileName(String cvFileName) {
        this.cvFileName = cvFileName;
    }

    public String getCvContentType() {
        return cvContentType;
    }

    public void setCvContentType(String cvContentType) {
        this.cvContentType = cvContentType;
    }

    public Long getCvSizeBytes() {
        return cvSizeBytes;
    }

    public void setCvSizeBytes(Long cvSizeBytes) {
        this.cvSizeBytes = cvSizeBytes;
    }

    public Boolean getCvExtractionPending() {
        return cvExtractionPending;
    }

    public void setCvExtractionPending(Boolean cvExtractionPending) {
        this.cvExtractionPending = cvExtractionPending;
    }

    public String getCvExtractionStatus() {
        return cvExtractionStatus;
    }

    public void setCvExtractionStatus(String cvExtractionStatus) {
        this.cvExtractionStatus = cvExtractionStatus;
    }

    public String getCvExtractedText() {
        return cvExtractedText;
    }

    public void setCvExtractedText(String cvExtractedText) {
        this.cvExtractedText = cvExtractedText;
    }

    public String getCvExtractionError() {
        return cvExtractionError;
    }

    public void setCvExtractionError(String cvExtractionError) {
        this.cvExtractionError = cvExtractionError;
    }

    public Instant getCvExtractedAt() {
        return cvExtractedAt;
    }

    public void setCvExtractedAt(Instant cvExtractedAt) {
        this.cvExtractedAt = cvExtractedAt;
    }

    public String getCvSummaryStatus() {
        return cvSummaryStatus;
    }

    public void setCvSummaryStatus(String cvSummaryStatus) {
        this.cvSummaryStatus = cvSummaryStatus;
    }

    public String getCvSummaryCoreCompetenceOverview() {
        return cvSummaryCoreCompetenceOverview;
    }

    public void setCvSummaryCoreCompetenceOverview(String cvSummaryCoreCompetenceOverview) {
        this.cvSummaryCoreCompetenceOverview = cvSummaryCoreCompetenceOverview;
    }

    public String getCvSummaryLocation() {
        return cvSummaryLocation;
    }

    public void setCvSummaryLocation(String cvSummaryLocation) {
        this.cvSummaryLocation = cvSummaryLocation;
    }

    public String getCvSummaryOtherDetails() {
        return cvSummaryOtherDetails;
    }

    public void setCvSummaryOtherDetails(String cvSummaryOtherDetails) {
        this.cvSummaryOtherDetails = cvSummaryOtherDetails;
    }

    public Instant getCvSummaryGeneratedAt() {
        return cvSummaryGeneratedAt;
    }

    public void setCvSummaryGeneratedAt(Instant cvSummaryGeneratedAt) {
        this.cvSummaryGeneratedAt = cvSummaryGeneratedAt;
    }
}
