package mcp.server.domain.candidate_profiles.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "cand_appli", schema = "marketplace", indexes = {
        @Index(name = "idx_cand_appli_created_at", columnList = "created_at")
})
public class CandidateApplicationSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cand_appli_id")
    private Long id;

    @Column(name = "submission_snapshot_json", nullable = false)
    private String submissionSnapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CandidateApplicationSnapshotEntity() {
    }

    public CandidateApplicationSnapshotEntity(Long id, String submissionSnapshotJson, Instant createdAt) {
        this.id = id;
        this.submissionSnapshotJson = submissionSnapshotJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubmissionSnapshotJson() {
        return submissionSnapshotJson;
    }

    public void setSubmissionSnapshotJson(String submissionSnapshotJson) {
        this.submissionSnapshotJson = submissionSnapshotJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
