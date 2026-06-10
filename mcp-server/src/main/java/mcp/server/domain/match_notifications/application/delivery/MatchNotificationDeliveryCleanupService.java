package mcp.server.domain.match_notifications.application.delivery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.match_notifications.api.MatchNotificationDeliveryCleanup;
import mcp.server.domain.match_notifications.persistence.MatchNotificationDeliveryJpaRepository;

@Service
public class MatchNotificationDeliveryCleanupService implements MatchNotificationDeliveryCleanup {

  private final MatchNotificationDeliveryJpaRepository deliveryRepo;

  public MatchNotificationDeliveryCleanupService(MatchNotificationDeliveryJpaRepository deliveryRepo) {
    this.deliveryRepo = deliveryRepo;
  }

  @Override
  @Transactional
  public int deleteDeliveriesForCandidateProfile(long candidateProfileId) {
    if (candidateProfileId <= 0) {
      return 0;
    }
    return deliveryRepo.deleteByCandidateProfileId(candidateProfileId);
  }
}
