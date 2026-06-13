package mcp.server.domain.matching.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "candidate_to_slot_match", schema = "consultant_platform", uniqueConstraints = {
    @UniqueConstraint(name = "uk_candidate_to_slot_match_profile_slot", columnNames = { "cand_profile_id",
        "mission_slot_id" })
}, indexes = {
    @Index(name = "idx_candidate_to_slot_match_profile_id", columnList = "cand_profile_id"),
    @Index(name = "idx_candidate_to_slot_match_slot_id", columnList = "mission_slot_id"),
    @Index(name = "idx_candidate_to_slot_match_score", columnList = "match_score"),
    @Index(name = "idx_candidate_to_slot_match_created_at", columnList = "created_at")
})
public class CandidateToSlotMatchEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "candidate_to_slot_match_id")
  private Long id;

  @Column(name = "cand_profile_id", nullable = false)
  private Long candidateProfileId;

  @Column(name = "mission_slot_id", nullable = false)
  private Long missionSlotId;

  @Column(name = "match_score", nullable = false)
  private Integer matchScore;

  @Column(name = "match_label", nullable = false, length = 40)
  private String matchLabel;

  @Column(name = "role_matched", nullable = false)
  private Boolean roleMatched;

  @Column(name = "work_mode_matched", nullable = false)
  private Boolean workModeMatched;

  @Column(name = "matched_skill_count", nullable = false)
  private Integer matchedSkillCount;

  @Column(name = "matched_skill_ids", nullable = false)
  private String matchedSkillIds;

  @Column(name = "matched_skill_titles", nullable = false)
  private String matchedSkillTitles;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected CandidateToSlotMatchEntity() {
  }

  public CandidateToSlotMatchEntity(
      Long id,
      Long candidateProfileId,
      Long missionSlotId,
      Integer matchScore,
      String matchLabel,
      Boolean roleMatched,
      Boolean workModeMatched,
      Integer matchedSkillCount,
      String matchedSkillIds,
      String matchedSkillTitles,
      Instant createdAt) {
    this.id = id;
    this.candidateProfileId = candidateProfileId;
    this.missionSlotId = missionSlotId;
    this.matchScore = matchScore;
    this.matchLabel = matchLabel;
    this.roleMatched = roleMatched;
    this.workModeMatched = workModeMatched;
    this.matchedSkillCount = matchedSkillCount;
    this.matchedSkillIds = matchedSkillIds;
    this.matchedSkillTitles = matchedSkillTitles;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getCandidateProfileId() {
    return candidateProfileId;
  }

  public Long getMissionSlotId() {
    return missionSlotId;
  }

  public Integer getMatchScore() {
    return matchScore;
  }

  public String getMatchLabel() {
    return matchLabel;
  }

  public Boolean getRoleMatched() {
    return roleMatched;
  }

  public Boolean getWorkModeMatched() {
    return workModeMatched;
  }

  public Integer getMatchedSkillCount() {
    return matchedSkillCount;
  }

  public String getMatchedSkillIds() {
    return matchedSkillIds;
  }

  public String getMatchedSkillTitles() {
    return matchedSkillTitles;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
