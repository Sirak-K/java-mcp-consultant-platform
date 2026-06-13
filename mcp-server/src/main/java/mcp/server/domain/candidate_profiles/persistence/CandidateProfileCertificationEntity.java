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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "cand_profile_certifications", schema = "consultant_platform", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cand_profile_certification", columnNames = { "cand_profile_id",
                "certification_number" })
}, indexes = {
        @Index(name = "idx_cand_profile_certifications_profile_id", columnList = "cand_profile_id")
})
public class CandidateProfileCertificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cand_profile_certification_id")
    private Long id;

    @Column(name = "cand_profile_id", nullable = false, insertable = false, updatable = false)
    private Long candidateProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cand_profile_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cand_profile_certification_profile"))
    private CandidateProfileEntity candidateProfile;

    @Column(name = "certification_number", nullable = false)
    private Integer certificationNumber;

    @Column(name = "certification_name", nullable = false, length = 150)
    private String certificationName;

    @Column(name = "document_file_name", nullable = false, length = 255)
    private String documentFileName;

    @Column(name = "document_content_type", nullable = false, length = 120)
    private String documentContentType;

    @Column(name = "document_size_bytes")
    private Long documentSizeBytes;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "document_bytes")
    private byte[] documentBytes;

    @Column(name = "document_uploaded_at")
    private Instant documentUploadedAt;

    protected CandidateProfileCertificationEntity() {
    }

    public CandidateProfileCertificationEntity(
            Long id,
            Integer certificationNumber,
            String certificationName,
            String documentFileName,
            String documentContentType,
            Long documentSizeBytes,
            byte[] documentBytes,
            Instant documentUploadedAt) {
        this.id = id;
        this.certificationNumber = certificationNumber;
        this.certificationName = certificationName;
        this.documentFileName = documentFileName;
        this.documentContentType = documentContentType;
        this.documentSizeBytes = documentSizeBytes;
        this.documentBytes = documentBytes;
        this.documentUploadedAt = documentUploadedAt;
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

    public Integer getCertificationNumber() {
        return certificationNumber;
    }

    public void setCertificationNumber(Integer certificationNumber) {
        this.certificationNumber = certificationNumber;
    }

    public String getCertificationName() {
        return certificationName;
    }

    public void setCertificationName(String certificationName) {
        this.certificationName = certificationName;
    }

    public String getDocumentFileName() {
        return documentFileName;
    }

    public void setDocumentFileName(String documentFileName) {
        this.documentFileName = documentFileName;
    }

    public String getDocumentContentType() {
        return documentContentType;
    }

    public void setDocumentContentType(String documentContentType) {
        this.documentContentType = documentContentType;
    }

    public Long getDocumentSizeBytes() {
        return documentSizeBytes;
    }

    public void setDocumentSizeBytes(Long documentSizeBytes) {
        this.documentSizeBytes = documentSizeBytes;
    }

    public byte[] getDocumentBytes() {
        return documentBytes;
    }

    public void setDocumentBytes(byte[] documentBytes) {
        this.documentBytes = documentBytes;
    }

    public Instant getDocumentUploadedAt() {
        return documentUploadedAt;
    }

    public void setDocumentUploadedAt(Instant documentUploadedAt) {
        this.documentUploadedAt = documentUploadedAt;
    }
}
