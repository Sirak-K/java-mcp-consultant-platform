package mcp.server.domain.match_notifications.api;

public interface MatchNotificationDeliveryCleanup {

  int deleteDeliveriesForCandidateProfile(long candidateProfileId);
}
