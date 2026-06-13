package mcp.server.domain.match_notifications.persistence;

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
@Table(name = "match_notification_delivery", schema = "consultant_platform", uniqueConstraints = {
    @UniqueConstraint(name = "uk_match_notification_delivery_group_provider_recipient", columnNames = { "delivery_group_key",
        "provider", "recipient_email" })
}, indexes = {
    @Index(name = "idx_match_notification_delivery_group_key", columnList = "delivery_group_key"),
    @Index(name = "idx_match_notification_delivery_primary_match", columnList = "primary_candidate_to_slot_match_id"),
    @Index(name = "idx_match_notification_delivery_status", columnList = "status"),
    @Index(name = "idx_match_notification_delivery_completed_at", columnList = "send_completed_at")
})
public class MatchNotificationDeliveryEntity {

  public static final String STATUS_SEND_IN_PROGRESS = "SEND_IN_PROGRESS";
  public static final String STATUS_SENT = "SENT";
  public static final String STATUS_FAILED = "FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "match_notification_delivery_id")
  private Long id;

  @Column(name = "delivery_group_key", nullable = false, length = 220)
  private String deliveryGroupKey;

  @Column(name = "primary_candidate_to_slot_match_id", nullable = false)
  private Long primaryCandidateToSlotMatchId;

  @Column(name = "candidate_to_slot_match_ids", nullable = false)
  private String candidateToSlotMatchIds;

  @Column(name = "cand_profile_id", nullable = false)
  private Long candidateProfileId;

  @Column(name = "mission_id", nullable = false)
  private Long missionId;

  @Column(name = "match_recorded_at", nullable = false)
  private Instant matchRecordedAt;

  @Column(name = "recipient_email", nullable = false, length = 320)
  private String recipientEmail;

  @Column(name = "sender_email", nullable = false, length = 320)
  private String senderEmail;

  @Column(name = "subject", nullable = false, length = 255)
  private String subject;

  @Column(name = "provider", nullable = false, length = 60)
  private String provider;

  @Column(name = "send_source", nullable = false, length = 60)
  private String sendSource;

  @Column(name = "provider_message_id", length = 255)
  private String providerMessageId;

  @Column(name = "provider_response_ref", length = 1000)
  private String providerResponseRef;

  @Column(name = "status", nullable = false, length = 40)
  private String status;

  @Column(name = "send_started_at", nullable = false)
  private Instant sendStartedAt;

  @Column(name = "send_completed_at")
  private Instant sendCompletedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MatchNotificationDeliveryEntity() {
  }

  private MatchNotificationDeliveryEntity(
      String deliveryGroupKey,
      Long primaryCandidateToSlotMatchId,
      String candidateToSlotMatchIds,
      Long candidateProfileId,
      Long missionId,
      Instant matchRecordedAt,
      String recipientEmail,
      String senderEmail,
      String subject,
      String provider,
      String sendSource,
      String providerMessageId,
      String providerResponseRef,
      String status,
      Instant sendStartedAt,
      Instant sendCompletedAt,
      Instant createdAt,
      Instant updatedAt) {
    this.deliveryGroupKey = deliveryGroupKey;
    this.primaryCandidateToSlotMatchId = primaryCandidateToSlotMatchId;
    this.candidateToSlotMatchIds = candidateToSlotMatchIds;
    this.candidateProfileId = candidateProfileId;
    this.missionId = missionId;
    this.matchRecordedAt = matchRecordedAt;
    this.recipientEmail = recipientEmail;
    this.senderEmail = senderEmail;
    this.subject = subject;
    this.provider = provider;
    this.sendSource = sendSource;
    this.providerMessageId = providerMessageId;
    this.providerResponseRef = providerResponseRef;
    this.status = status;
    this.sendStartedAt = sendStartedAt;
    this.sendCompletedAt = sendCompletedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static MatchNotificationDeliveryEntity reserve(
      String deliveryGroupKey,
      Long primaryCandidateToSlotMatchId,
      String candidateToSlotMatchIds,
      Long candidateProfileId,
      Long missionId,
      Instant matchRecordedAt,
      String recipientEmail,
      String senderEmail,
      String subject,
      String provider,
      String sendSource,
      Instant now) {
    return new MatchNotificationDeliveryEntity(
        deliveryGroupKey,
        primaryCandidateToSlotMatchId,
        candidateToSlotMatchIds,
        candidateProfileId,
        missionId,
        matchRecordedAt,
        recipientEmail,
        senderEmail,
        subject,
        provider,
        sendSource,
        null,
        null,
        STATUS_SEND_IN_PROGRESS,
        now,
        null,
        now,
        now);
  }

  public Long getId() {
    return id;
  }

  public String getDeliveryGroupKey() {
    return deliveryGroupKey;
  }

  public String getProviderMessageId() {
    return providerMessageId;
  }

  public String getProviderResponseRef() {
    return providerResponseRef;
  }

  public String getStatus() {
    return status;
  }

  public Instant getSendStartedAt() {
    return sendStartedAt;
  }

  public Instant getSendCompletedAt() {
    return sendCompletedAt;
  }

  public void markSendInProgress(String senderEmail, String sendSource, Instant now) {
    this.senderEmail = senderEmail;
    this.sendSource = sendSource;
    this.providerMessageId = null;
    this.providerResponseRef = null;
    this.status = STATUS_SEND_IN_PROGRESS;
    this.sendStartedAt = now;
    this.sendCompletedAt = null;
    this.updatedAt = now;
  }

  public void markSent(String providerMessageId, String providerResponseRef, Instant now) {
    this.providerMessageId = providerMessageId;
    this.providerResponseRef = providerResponseRef;
    this.status = STATUS_SENT;
    this.sendCompletedAt = now;
    this.updatedAt = now;
  }

  public void markFailed(String providerResponseRef, Instant now) {
    this.providerResponseRef = providerResponseRef;
    this.status = STATUS_FAILED;
    this.sendCompletedAt = now;
    this.updatedAt = now;
  }
}
