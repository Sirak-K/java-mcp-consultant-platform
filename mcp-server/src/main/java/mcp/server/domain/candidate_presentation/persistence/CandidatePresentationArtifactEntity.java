package mcp.server.domain.candidate_presentation.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "candidate_presentation_artifact", schema = "consultant_platform", uniqueConstraints = {
    @UniqueConstraint(name = "uk_candidate_presentation_artifact_match", columnNames = {
        "source_candidate_to_slot_match_id" })
}, indexes = {
    @Index(name = "idx_candidate_presentation_artifact_profile", columnList = "cand_profile_id"),
    @Index(name = "idx_candidate_presentation_artifact_mission", columnList = "mission_id"),
    @Index(name = "idx_candidate_presentation_artifact_slot", columnList = "mission_slot_id"),
    @Index(name = "idx_candidate_presentation_artifact_status", columnList = "artifact_status"),
    @Index(name = "idx_candidate_presentation_artifact_updated_at", columnList = "updated_at")
})
public class CandidatePresentationArtifactEntity {

  public static final String STATUS_PENDING_GENERATION = "PENDING_GENERATION";
  public static final String STATUS_GENERATED = "GENERATED";
  public static final String STATUS_OPS_REVIEW = "OPS_REVIEW";
  public static final String STATUS_GENERATION_FAILED = "GENERATION_FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "candidate_presentation_artifact_id")
  private Long id;

  @Column(name = "source_candidate_to_slot_match_id", nullable = false)
  private Long sourceCandidateToSlotMatchId;

  @Column(name = "cand_profile_id", nullable = false)
  private Long candProfileId;

  @Column(name = "mission_id", nullable = false)
  private Long missionId;

  @Column(name = "mission_slot_id", nullable = false)
  private Long missionSlotId;

  @Column(name = "artifact_status", nullable = false, length = 60)
  private String artifactStatus;

  @Column(name = "presentation_title", nullable = false, length = 255)
  private String presentationTitle;

  @Column(name = "customer_facing_content_json", nullable = false, columnDefinition = "TEXT")
  private String customerFacingContentJson;

  @Column(name = "ops_review_content_json", nullable = false, columnDefinition = "TEXT")
  private String opsReviewContentJson;

  @Column(name = "evidence_trace_json", nullable = false, columnDefinition = "TEXT")
  private String evidenceTraceJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CandidatePresentationArtifactEntity() {
  }

  private CandidatePresentationArtifactEntity(
      Long sourceCandidateToSlotMatchId,
      Long candProfileId,
      Long missionId,
      Long missionSlotId,
      String artifactStatus,
      String presentationTitle,
      String customerFacingContentJson,
      String opsReviewContentJson,
      String evidenceTraceJson,
      Instant createdAt,
      Instant updatedAt) {
    this.sourceCandidateToSlotMatchId = sourceCandidateToSlotMatchId;
    this.candProfileId = candProfileId;
    this.missionId = missionId;
    this.missionSlotId = missionSlotId;
    this.artifactStatus = artifactStatus;
    this.presentationTitle = presentationTitle;
    this.customerFacingContentJson = customerFacingContentJson;
    this.opsReviewContentJson = opsReviewContentJson;
    this.evidenceTraceJson = evidenceTraceJson;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static CandidatePresentationArtifactEntity pendingGenerationDraft(
      Long sourceCandidateToSlotMatchId,
      Long candProfileId,
      Long missionId,
      Long missionSlotId,
      String presentationTitle,
      String customerFacingContentJson,
      String opsReviewContentJson,
      String evidenceTraceJson,
      Instant now) {
    return new CandidatePresentationArtifactEntity(
        sourceCandidateToSlotMatchId,
        candProfileId,
        missionId,
        missionSlotId,
        STATUS_PENDING_GENERATION,
        presentationTitle,
        customerFacingContentJson,
        opsReviewContentJson,
        evidenceTraceJson,
        now,
        now);
  }

  public Long getId() {
    return id;
  }

  public Long getSourceCandidateToSlotMatchId() {
    return sourceCandidateToSlotMatchId;
  }

  public Long getCandProfileId() {
    return candProfileId;
  }

  public Long getMissionId() {
    return missionId;
  }

  public Long getMissionSlotId() {
    return missionSlotId;
  }

  public String getArtifactStatus() {
    return artifactStatus;
  }

  public String getPresentationTitle() {
    return presentationTitle;
  }

  public String getCustomerFacingContentJson() {
    return customerFacingContentJson;
  }

  public String getOpsReviewContentJson() {
    return opsReviewContentJson;
  }

  public String getEvidenceTraceJson() {
    return evidenceTraceJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markGenerated(
      String presentationTitle,
      String customerFacingContentJson,
      String opsReviewContentJson,
      String evidenceTraceJson,
      Instant now) {
    this.artifactStatus = STATUS_GENERATED;
    updateContent(presentationTitle, customerFacingContentJson, opsReviewContentJson, evidenceTraceJson, now);
  }

  public void markGenerationFailed(
      String opsReviewContentJson,
      String evidenceTraceJson,
      Instant now) {
    this.artifactStatus = STATUS_GENERATION_FAILED;
    this.opsReviewContentJson = opsReviewContentJson;
    this.evidenceTraceJson = evidenceTraceJson;
    this.updatedAt = now;
  }

  public void markPendingGeneration(
      String presentationTitle,
      String customerFacingContentJson,
      String opsReviewContentJson,
      String evidenceTraceJson,
      Instant now) {
    this.artifactStatus = STATUS_PENDING_GENERATION;
    updateContent(presentationTitle, customerFacingContentJson, opsReviewContentJson, evidenceTraceJson, now);
  }

  public void markOpsReview(
      String presentationTitle,
      String customerFacingContentJson,
      String opsReviewContentJson,
      String evidenceTraceJson,
      Instant now) {
    this.artifactStatus = STATUS_OPS_REVIEW;
    updateContent(presentationTitle, customerFacingContentJson, opsReviewContentJson, evidenceTraceJson, now);
  }

  public void updateContent(
      String presentationTitle,
      String customerFacingContentJson,
      String opsReviewContentJson,
      String evidenceTraceJson,
      Instant now) {
    this.presentationTitle = presentationTitle;
    this.customerFacingContentJson = customerFacingContentJson;
    this.opsReviewContentJson = opsReviewContentJson;
    this.evidenceTraceJson = evidenceTraceJson;
    this.updatedAt = now;
  }
}
