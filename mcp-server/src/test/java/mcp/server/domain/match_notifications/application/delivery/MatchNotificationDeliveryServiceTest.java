package mcp.server.domain.match_notifications.application.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import mcp.server.domain.match_notifications.exception.MatchNotificationDeliveryException;
import mcp.server.domain.match_notifications.model.MatchNotificationSendSource;
import mcp.server.domain.match_notifications.persistence.MatchNotificationDeliveryEntity;
import mcp.server.domain.match_notifications.persistence.MatchNotificationDeliveryJpaRepository;
import mcp.server.domain.match_notifications.web.MatchNotificationWebContract;

class MatchNotificationDeliveryServiceTest {

  private static final String OPS_RECIPIENT_EMAIL = "platform-ops@example.com";
  private static final String OPS_RECIPIENT_NAME = "Platform Operator";
  private static final String MOCK_SENDER_EMAIL = "match-notifications@local.test";

  private final MatchNotificationDeliveryJpaRepository deliveryRepo =
      mock(MatchNotificationDeliveryJpaRepository.class);
  private final MatchNotificationMimeMessageBuilder mimeMessageBuilder =
      mock(MatchNotificationMimeMessageBuilder.class);
  private final MatchNotificationMockOutboxWriter mockOutboxWriter =
      mock(MatchNotificationMockOutboxWriter.class);
  private final BrevoMatchNotificationEmailAdapter brevoEmailAdapter =
      mock(BrevoMatchNotificationEmailAdapter.class);
  private final MatchNotificationDeliveryService deliveryService = new MatchNotificationDeliveryService(
      deliveryRepo,
      mimeMessageBuilder,
      mockOutboxWriter,
      brevoEmailAdapter,
      OPS_RECIPIENT_EMAIL,
      OPS_RECIPIENT_NAME,
      MOCK_SENDER_EMAIL);

  @Test
  void sendMockWritesRfc822OutboxWithoutDeliveryAuditReservation() {
    MatchNotificationWebContract.MatchNotificationPreviewView preview = preview();
    when(mimeMessageBuilder.build(any(), any(), any(), any(), any())).thenReturn("rfc822-message");
    when(mockOutboxWriter.write(any(Long.class), any(Instant.class), any())).thenReturn("target/outbox/mail.eml");

    MatchNotificationWebContract.MatchNotificationSendView result = deliveryService.sendMock(preview);

    assertThat(result.transport()).isEqualTo("LOCAL_RFC822_MOCK");
    assertThat(result.status()).isEqualTo("MOCK_SENT");
    assertThat(result.deliveryStatus()).isEqualTo("MOCK_SENT");
    assertThat(result.transportRef()).isEqualTo("target/outbox/mail.eml");
    assertThat(result.messageRfc822()).isEqualTo("rfc822-message");
    verify(deliveryRepo, never()).saveAndFlush(any());
    verify(deliveryRepo, never()).save(any());
  }

  @Test
  void sendRealEmailRequiresEnabledRealSendBeforeReservation() {
    when(brevoEmailAdapter.loadConfig()).thenReturn(config(false));

    assertThatThrownBy(() -> deliveryService.sendRealEmail(
        preview(),
        deliveryContext(),
        MatchNotificationSendSource.OPS_REST))
        .isInstanceOf(MatchNotificationDeliveryException.class)
        .extracting("reason")
        .isEqualTo(MatchNotificationDeliveryException.Reason.PRECONDITION_FAILED);
    verify(deliveryRepo, never()).findFirstByDeliveryGroupKeyAndProviderAndRecipientEmail(any(), any(), any());
    verify(brevoEmailAdapter, never()).send(any());
  }

  @Test
  void sendRealEmailRejectsExistingSentReservationBeforeExternalSend() {
    MatchNotificationDeliveryEntity existingSent = reservedDelivery();
    existingSent.markSent("already-sent", "accepted", Instant.parse("2026-01-01T10:01:00Z"));
    when(brevoEmailAdapter.loadConfig()).thenReturn(config(true));
    when(deliveryRepo.findFirstByDeliveryGroupKeyAndProviderAndRecipientEmail(
        "delivery-group-1",
        "BREVO_API",
        OPS_RECIPIENT_EMAIL)).thenReturn(Optional.of(existingSent));

    assertThatThrownBy(() -> deliveryService.sendRealEmail(
        preview(),
        deliveryContext(),
        MatchNotificationSendSource.OPS_REST))
        .isInstanceOf(MatchNotificationDeliveryException.class)
        .extracting("reason")
        .isEqualTo(MatchNotificationDeliveryException.Reason.CONFLICT);
    verify(brevoEmailAdapter, never()).send(any());
  }

  @Test
  void sendRealEmailReservesDeliveryAndMarksSentAfterProviderSuccess() {
    MatchNotificationDeliveryEntity reservedDelivery = reservedDelivery();
    when(brevoEmailAdapter.loadConfig()).thenReturn(config(true));
    when(deliveryRepo.findFirstByDeliveryGroupKeyAndProviderAndRecipientEmail(
        "delivery-group-1",
        "BREVO_API",
        OPS_RECIPIENT_EMAIL)).thenReturn(Optional.empty());
    when(deliveryRepo.saveAndFlush(any(MatchNotificationDeliveryEntity.class))).thenReturn(reservedDelivery);
    when(mimeMessageBuilder.build(any(), any(), any(), any(), any())).thenReturn("rfc822-message");
    when(brevoEmailAdapter.send(any())).thenReturn(new BrevoMatchNotificationEmailAdapter.BrevoSendResult(
        "brevo-message-1",
        "{\"messageId\":\"brevo-message-1\"}"));

    MatchNotificationWebContract.MatchNotificationSendView result = deliveryService.sendRealEmail(
        preview(),
        deliveryContext(),
        MatchNotificationSendSource.PLATFORM_OPS_TOOL);

    assertThat(result.transport()).isEqualTo("BREVO_API");
    assertThat(result.status()).isEqualTo(MatchNotificationDeliveryEntity.STATUS_SENT);
    assertThat(result.deliveryStatus()).isEqualTo(MatchNotificationDeliveryEntity.STATUS_SENT);
    assertThat(result.transportRef()).isEqualTo("brevo-message-1");
    assertThat(result.messageRfc822()).isEqualTo("rfc822-message");
    assertThat(reservedDelivery.getStatus()).isEqualTo(MatchNotificationDeliveryEntity.STATUS_SENT);
    assertThat(reservedDelivery.getProviderMessageId()).isEqualTo("brevo-message-1");
    verify(deliveryRepo).save(reservedDelivery);
  }

  private static MatchNotificationWebContract.MatchNotificationPreviewView preview() {
    return new MatchNotificationWebContract.MatchNotificationPreviewView(
        1001L,
        List.of(1001L, 1002L),
        501L,
        301L,
        2,
        "Match notification ready",
        "Ada is a strong match for Backend Engineer.",
        "Plain text body",
        "<p>Html body</p>",
        "2026-01-01 | 10:00:00");
  }

  private static MatchNotificationDeliveryService.RealEmailDeliveryContext deliveryContext() {
    return new MatchNotificationDeliveryService.RealEmailDeliveryContext(
        "delivery-group-1",
        Instant.parse("2026-01-01T10:00:00Z"));
  }

  private static BrevoMatchNotificationEmailAdapter.BrevoEmailConfig config(boolean realSendEnabled) {
    return new BrevoMatchNotificationEmailAdapter.BrevoEmailConfig(
        "api-key",
        "match@example.com",
        "Consultant Match Ops",
        "reply@example.com",
        "Consultant Match Ops",
        realSendEnabled);
  }

  private static MatchNotificationDeliveryEntity reservedDelivery() {
    return MatchNotificationDeliveryEntity.reserve(
        "delivery-group-1",
        1001L,
        "1001,1002",
        501L,
        301L,
        Instant.parse("2026-01-01T10:00:00Z"),
        OPS_RECIPIENT_EMAIL,
        "match@example.com",
        "Match notification ready",
        "BREVO_API",
        MatchNotificationSendSource.PLATFORM_OPS_TOOL.name(),
        Instant.parse("2026-01-01T10:00:01Z"));
  }
}
