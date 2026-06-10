package mcp.server.domain.match_notifications.application.delivery;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.match_notifications.exception.MatchNotificationDeliveryException;
import mcp.server.domain.match_notifications.model.MatchNotificationSendSource;
import mcp.server.domain.match_notifications.persistence.MatchNotificationDeliveryEntity;
import mcp.server.domain.match_notifications.persistence.MatchNotificationDeliveryJpaRepository;
import mcp.server.domain.match_notifications.web.MatchNotificationWebContract;

@Service
public class MatchNotificationDeliveryService {

  public record RealEmailDeliveryContext(String deliveryGroupKey, Instant matchCreatedAt) {
  }

  private static final String MOCK_TRANSPORT = "LOCAL_RFC822_MOCK";
  private static final String BREVO_TRANSPORT = "BREVO_API";
  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

  private final MatchNotificationDeliveryJpaRepository deliveryRepo;
  private final MatchNotificationMimeMessageBuilder mimeMessageBuilder;
  private final MatchNotificationMockOutboxWriter mockOutboxWriter;
  private final BrevoMatchNotificationEmailAdapter brevoEmailAdapter;
  private final String opsRecipientEmail;
  private final String opsRecipientName;
  private final String mockSenderEmail;

  public MatchNotificationDeliveryService(
      MatchNotificationDeliveryJpaRepository deliveryRepo,
      MatchNotificationMimeMessageBuilder mimeMessageBuilder,
      MatchNotificationMockOutboxWriter mockOutboxWriter,
      BrevoMatchNotificationEmailAdapter brevoEmailAdapter,
      @Value("${mcp.match-notifications.delivery.ops-recipient.email}") String opsRecipientEmail,
      @Value("${mcp.match-notifications.delivery.ops-recipient.name}") String opsRecipientName,
      @Value("${mcp.match-notifications.delivery.mock-sender-email}") String mockSenderEmail) {
    this.deliveryRepo = Objects.requireNonNull(deliveryRepo, "deliveryRepo");
    this.mimeMessageBuilder = Objects.requireNonNull(mimeMessageBuilder, "mimeMessageBuilder");
    this.mockOutboxWriter = Objects.requireNonNull(mockOutboxWriter, "mockOutboxWriter");
    this.brevoEmailAdapter = Objects.requireNonNull(brevoEmailAdapter, "brevoEmailAdapter");
    this.opsRecipientEmail = requireText(opsRecipientEmail, "opsRecipientEmail");
    this.opsRecipientName = requireText(opsRecipientName, "opsRecipientName");
    this.mockSenderEmail = requireText(mockSenderEmail, "mockSenderEmail");
  }

  public MatchNotificationWebContract.MatchNotificationSendView sendMock(
      MatchNotificationWebContract.MatchNotificationPreviewView preview) {
    MatchNotificationWebContract.MatchNotificationPreviewView safePreview = Objects.requireNonNull(preview,
        "preview");
    Instant sentAtInstant = Instant.now();
    String messageRfc822 = mimeMessageBuilder.build(
        safePreview,
        sentAtInstant,
        mockSenderEmail,
        opsRecipientEmail,
        mockSenderEmail);
    String transportRef = mockOutboxWriter.write(safePreview.matchId(), sentAtInstant, messageRfc822);

    return new MatchNotificationWebContract.MatchNotificationSendView(
        safePreview.matchId(),
        safePreview.matchIds(),
        safePreview.candidateProfileId(),
        safePreview.missionId(),
        safePreview.groupedMatchCount(),
        opsRecipientEmail,
        mockSenderEmail,
        safePreview.subject(),
        MOCK_TRANSPORT,
        "MOCK_SENT",
        transportRef,
        formatInstant(sentAtInstant),
        null,
        "MOCK_SENT",
        messageRfc822);
  }

  @Transactional
  public MatchNotificationWebContract.MatchNotificationSendView sendRealEmail(
      MatchNotificationWebContract.MatchNotificationPreviewView preview,
      RealEmailDeliveryContext deliveryCtx,
      MatchNotificationSendSource realSendSource) {

    MatchNotificationWebContract.MatchNotificationPreviewView safePreview = Objects.requireNonNull(preview,
        "preview");
    RealEmailDeliveryContext safeDeliveryCtx = Objects.requireNonNull(deliveryCtx, "deliveryCtx");
    MatchNotificationSendSource safeSendSource = Objects.requireNonNull(realSendSource, "realSendSource");

    BrevoMatchNotificationEmailAdapter.BrevoEmailConfig config = brevoEmailAdapter.loadConfig();
    if (!config.realSendEnabled()) {
      throw MatchNotificationDeliveryException.preconditionFailed(
          "Match notification real send is disabled. Set MATCH_NOTIFICATION_REAL_SEND_ENABLED=true to send via Brevo.");
    }

    Instant startedAt = Instant.now();
    MatchNotificationDeliveryEntity delivery = reserveRealEmailDelivery(
        safeDeliveryCtx,
        safePreview,
        config.senderEmail(),
        safeSendSource,
        startedAt);
    String messageRfc822 = mimeMessageBuilder.build(
        safePreview,
        startedAt,
        config.senderEmail(),
        opsRecipientEmail,
        config.replyToEmail());
    BrevoMatchNotificationEmailAdapter.BrevoSendResult sendResult;
    Instant completedAt;
    try {
      sendResult = brevoEmailAdapter.send(new BrevoMatchNotificationEmailAdapter.BrevoSendRequest(
          safePreview,
          opsRecipientEmail,
          opsRecipientName,
          config));
      completedAt = Instant.now();
      delivery.markSent(sendResult.messageId(), sendResult.responseRef(), completedAt);
      deliveryRepo.save(delivery);
    } catch (MatchNotificationDeliveryException excep) {
      completedAt = Instant.now();
      String failureRef = compactExceptionMessage(excep);
      delivery.markFailed(failureRef, completedAt);
      deliveryRepo.save(delivery);
      return sendView(
          safePreview,
          config.senderEmail(),
          BREVO_TRANSPORT,
          MatchNotificationDeliveryEntity.STATUS_FAILED,
          failureRef,
          completedAt,
          delivery,
          messageRfc822);
    }

    return sendView(
        safePreview,
        config.senderEmail(),
        BREVO_TRANSPORT,
        MatchNotificationDeliveryEntity.STATUS_SENT,
        sendResult.messageId(),
        completedAt,
        delivery,
        messageRfc822);
  }

  private MatchNotificationDeliveryEntity reserveRealEmailDelivery(
      RealEmailDeliveryContext deliveryCtx,
      MatchNotificationWebContract.MatchNotificationPreviewView preview,
      String senderEmail,
      MatchNotificationSendSource sendSource,
      Instant now) {
    return deliveryRepo.findFirstByDeliveryGroupKeyAndProviderAndRecipientEmail(
        deliveryCtx.deliveryGroupKey(),
        BREVO_TRANSPORT,
        opsRecipientEmail)
        .map(existing -> {
          if (MatchNotificationDeliveryEntity.STATUS_SENT.equals(existing.getStatus())
              || MatchNotificationDeliveryEntity.STATUS_SEND_IN_PROGRESS.equals(existing.getStatus())) {
            throw MatchNotificationDeliveryException.conflict(
                "Match notification email already reserved or sent for this match group.");
          }
          existing.markSendInProgress(senderEmail, sendSource.name(), now);
          return existing;
        })
        .orElseGet(() -> {
          MatchNotificationDeliveryEntity delivery = MatchNotificationDeliveryEntity.reserve(
              deliveryCtx.deliveryGroupKey(),
              preview.matchId(),
              preview.matchIds().stream().map(String::valueOf).collect(Collectors.joining(",")),
              preview.candidateProfileId(),
              preview.missionId(),
              deliveryCtx.matchCreatedAt(),
              opsRecipientEmail,
              senderEmail,
              preview.subject(),
              BREVO_TRANSPORT,
              sendSource.name(),
              now);
          try {
            return deliveryRepo.saveAndFlush(delivery);
          } catch (DataIntegrityViolationException excep) {
            throw MatchNotificationDeliveryException.conflict(
                "Match notification email already reserved or sent for this match group.",
                excep);
          }
        });
  }

  private MatchNotificationWebContract.MatchNotificationSendView sendView(
      MatchNotificationWebContract.MatchNotificationPreviewView preview,
      String senderEmail,
      String transport,
      String status,
      String transportRef,
      Instant sentAt,
      MatchNotificationDeliveryEntity delivery,
      String messageRfc822) {
    return new MatchNotificationWebContract.MatchNotificationSendView(
        preview.matchId(),
        preview.matchIds(),
        preview.candidateProfileId(),
        preview.missionId(),
        preview.groupedMatchCount(),
        opsRecipientEmail,
        senderEmail,
        preview.subject(),
        transport,
        status,
        transportRef,
        formatInstant(sentAt),
        delivery.getId(),
        delivery.getStatus(),
        messageRfc822);
  }

  private String compactExceptionMessage(MatchNotificationDeliveryException excep) {
    return compactText(excep.getMessage(), 1000);
  }

  private static String compactText(String value, int maxLength) {
    String compacted = textOrFallback(value, "")
        .replace("\r", " ")
        .replace("\n", " ")
        .replaceAll("\\s+", " ")
        .trim();
    return compacted.length() <= maxLength ? compacted : compacted.substring(0, maxLength);
  }

  private static String formatInstant(Instant instant) {
    return instant == null
        ? null
        : TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
  }

  private static String textOrFallback(String value, String fallback) {
    return value == null || value.trim().isBlank() ? fallback : value.trim();
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.trim().isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.trim();
  }
}
