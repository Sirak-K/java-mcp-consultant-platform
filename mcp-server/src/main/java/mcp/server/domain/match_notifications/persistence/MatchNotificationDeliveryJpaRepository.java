package mcp.server.domain.match_notifications.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchNotificationDeliveryJpaRepository extends JpaRepository<MatchNotificationDeliveryEntity, Long> {

  Optional<MatchNotificationDeliveryEntity> findFirstByDeliveryGroupKeyAndProviderAndRecipientEmail(
      String deliveryGroupKey,
      String provider,
      String recipientEmail);

  int deleteByCandidateProfileId(Long candidateProfileId);
}
