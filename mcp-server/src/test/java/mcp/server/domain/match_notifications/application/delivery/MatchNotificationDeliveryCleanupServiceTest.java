package mcp.server.domain.match_notifications.application.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import mcp.server.domain.match_notifications.persistence.MatchNotificationDeliveryJpaRepository;

class MatchNotificationDeliveryCleanupServiceTest {

  private final MatchNotificationDeliveryJpaRepository deliveryRepo =
      mock(MatchNotificationDeliveryJpaRepository.class);
  private final MatchNotificationDeliveryCleanupService cleanupService =
      new MatchNotificationDeliveryCleanupService(deliveryRepo);

  @Test
  void deleteDeliveriesForCandidateProfileIgnoresInvalidIds() {
    assertThat(cleanupService.deleteDeliveriesForCandidateProfile(0L)).isZero();
    assertThat(cleanupService.deleteDeliveriesForCandidateProfile(-1L)).isZero();

    verify(deliveryRepo, never()).deleteByCandidateProfileId(0L);
    verify(deliveryRepo, never()).deleteByCandidateProfileId(-1L);
  }

  @Test
  void deleteDeliveriesForCandidateProfileDelegatesToNotificationPersistence() {
    when(deliveryRepo.deleteByCandidateProfileId(77L)).thenReturn(3);

    assertThat(cleanupService.deleteDeliveriesForCandidateProfile(77L)).isEqualTo(3);

    verify(deliveryRepo).deleteByCandidateProfileId(77L);
  }
}
