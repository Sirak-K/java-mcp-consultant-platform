package mcp.server.domain.candidate_profiles.application.registered_profiles;

import mcp.server.domain.candidate_presentation.api.CandidatePresentationArtifactCleanup;
import mcp.server.domain.candidate_profiles.application.CandidateProfileEntityAssembler;
import mcp.server.domain.candidate_profiles.application.CandidateProfileWebViewAssembler;
import mcp.server.domain.candidate_profiles.application.intake.CandApplicationSnapshotService;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileJpaRepo;
import mcp.server.domain.candidate_profiles.web.CandidateApplicationWebContract;
import mcp.server.domain.candidate_profiles.web.RegisteredCandidateProfileWebContract;
import mcp.server.domain.match_notifications.api.MatchNotificationDeliveryCleanup;
import mcp.server.domain.matching.api.MissionMatchDiscoveryResult;
import mcp.server.domain.matching.api.CandidateToSlotMatchCleanup;
import mcp.server.domain.matching.api.CandidateToSlotMatchDiscovery;
import mcp.server.foundation.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RegisteredCandidateProfileService {

  private static final String OPS_TENANT = "ops";
  private static final String CANDIDATE_RECORD_TYPE = "candidateProfile";
  private static final String SUBMITTED_STATUS = "SUBMITTED";

  private final CandidateProfileJpaRepo candidateProfileRepo;
  private final CandApplicationSnapshotService applicationSnapshotService;
  private final CandidatePresentationArtifactCleanup candidatePresentationArtifactCleanup;
  private final MatchNotificationDeliveryCleanup matchNotificationDeliveryCleanup;
  private final CandidateProfileEntityAssembler entityAssembler;
  private final CandidateProfileWebViewAssembler webViewAssembler;
  private final CandidateToSlotMatchDiscovery matchDiscoveryService;
  private final CandidateToSlotMatchCleanup matchCleanup;
  private final AuditService auditService;

  public RegisteredCandidateProfileService(
      CandidateProfileJpaRepo candidateProfileRepo,
      CandApplicationSnapshotService applicationSnapshotService,
      CandidatePresentationArtifactCleanup candidatePresentationArtifactCleanup,
      MatchNotificationDeliveryCleanup matchNotificationDeliveryCleanup,
      CandidateProfileEntityAssembler entityAssembler,
      CandidateProfileWebViewAssembler webViewAssembler,
      CandidateToSlotMatchDiscovery matchDiscoveryService,
      CandidateToSlotMatchCleanup matchCleanup,
      AuditService auditService) {
    this.candidateProfileRepo = candidateProfileRepo;
    this.applicationSnapshotService = applicationSnapshotService;
    this.candidatePresentationArtifactCleanup = candidatePresentationArtifactCleanup;
    this.matchNotificationDeliveryCleanup = matchNotificationDeliveryCleanup;
    this.entityAssembler = entityAssembler;
    this.webViewAssembler = webViewAssembler;
    this.matchDiscoveryService = matchDiscoveryService;
    this.matchCleanup = matchCleanup;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<CandidateApplicationWebContract.CandidateApplicationView> registeredCandidateProfiles() {
    return candidateProfileRepo.findAllByOrderByCreatedAtDesc().stream()
        .map(candidateProfile -> webViewAssembler.toApplicationView(candidateProfile, List.of()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<RegisteredCandidateProfileWebContract.RegisteredCandidateProfileCardView> registeredCandidateProfileCards() {
    return candidateProfileRepo.findAllByOrderByCreatedAtDesc().stream()
        .map(webViewAssembler::toRegisteredProfileCardView)
        .toList();
  }

  @Transactional
  public CandidateApplicationWebContract.CandidateApplicationView editRegisteredCandidateProfile(
      long id,
      RegisteredCandidateProfileWebContract.RegisteredCandidateProfileEditInput request) {

    CandidateProfileEntity entity = requireCandidateProfile(id);
    entityAssembler.applyRegisteredProfileEdit(entity, request);
    CandidateProfileEntity saved = candidateProfileRepo.save(entity);
    recordBusinessEvent(
        "registeredCandidateProfile.edited",
        SUBMITTED_STATUS,
        OPS_TENANT,
        saved.getId());
    List<MissionMatchDiscoveryResult> findMissionResults = matchDiscoveryService.findMissionsForCandidate(saved.getId());
    return webViewAssembler.toApplicationView(saved, findMissionResults);
  }

  @Transactional
  public void deleteRegisteredCandidateProfile(long id) {
    CandidateProfileEntity entity = requireCandidateProfile(id);
    int deletedApplicationSnapshots = applicationSnapshotService.deleteSnapshotsForCandidateProfile(id);
    int deletedPresentationArtifacts = candidatePresentationArtifactCleanup.deleteArtifactsForCandidateProfile(id);
    int deletedMailDeliveries = matchNotificationDeliveryCleanup.deleteDeliveriesForCandidateProfile(id);
    int deletedMatches = matchCleanup.removeMatchesForCandidateProfile(id);
    candidateProfileRepo.delete(entity);
    recordBusinessEvent(
        "registeredCandidateProfile.deleted",
        "DELETED",
        OPS_TENANT,
        id,
        "applicationSnapshots=" + deletedApplicationSnapshots
            + ", presentationArtifacts=" + deletedPresentationArtifacts
            + ", mailDeliveries=" + deletedMailDeliveries
            + ", matches=" + deletedMatches);
  }

  private CandidateProfileEntity requireCandidateProfile(long id) {
    return candidateProfileRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "candidateProfile not found"));
  }

  private void recordBusinessEvent(
      String eventType,
      String status,
      String tenantId,
      Long recordId) {

    recordBusinessEvent(eventType, status, tenantId, recordId, "status=" + status);
  }

  private void recordBusinessEvent(
      String eventType,
      String status,
      String tenantId,
      Long recordId,
      String details) {

    auditService.recordBusinessEvent(
        eventType,
        status,
        tenantId,
        CANDIDATE_RECORD_TYPE,
        recordId,
        details);
  }
}
