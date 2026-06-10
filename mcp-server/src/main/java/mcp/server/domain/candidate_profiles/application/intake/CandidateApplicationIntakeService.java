package mcp.server.domain.candidate_profiles.application.intake;

import mcp.server.domain.candidate_profiles.application.CandidateProfileEntityAssembler;
import mcp.server.domain.candidate_profiles.application.CandidateProfileWebViewAssembler;
import mcp.server.domain.candidate_profiles.application.cv.CandCvPreviewService;
import mcp.server.domain.candidate_profiles.application.cv.CandCvTextExtractionService;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileJpaRepo;
import mcp.server.domain.candidate_profiles.web.CandidateApplicationWebContract;
import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import mcp.server.domain.matching.api.CandidateToSlotMatchDiscovery;
import mcp.server.domain.matching.api.MissionMatchDiscoveryResult;
import mcp.server.foundation.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.requireEmail;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Service
public class CandidateApplicationIntakeService {

  private static final String PUBLIC_TENANT = "public";
  private static final String CANDIDATE_RECORD_TYPE = "candidateProfile";
  private static final String SUBMITTED_STATUS = "SUBMITTED";

  private final CandidateProfileJpaRepo candidateProfileRepo;
  private final CandApplicationSnapshotService applicationSnapshotService;
  private final CandCvPreviewService cvPreviewService;
  private final CandidateProfileEntityAssembler entityAssembler;
  private final CandidateProfileWebViewAssembler webViewAssembler;
  private final CandidateToSlotMatchDiscovery matchDiscoveryService;
  private final AuditService auditService;

  public CandidateApplicationIntakeService(
      CandidateProfileJpaRepo candidateProfileRepo,
      CandApplicationSnapshotService applicationSnapshotService,
      CandCvPreviewService cvPreviewService,
      CandidateProfileEntityAssembler entityAssembler,
      CandidateProfileWebViewAssembler webViewAssembler,
      CandidateToSlotMatchDiscovery matchDiscoveryService,
      AuditService auditService) {
    this.candidateProfileRepo = candidateProfileRepo;
    this.applicationSnapshotService = applicationSnapshotService;
    this.cvPreviewService = cvPreviewService;
    this.entityAssembler = entityAssembler;
    this.webViewAssembler = webViewAssembler;
    this.matchDiscoveryService = matchDiscoveryService;
    this.auditService = auditService;
  }

  @Transactional
  public CandidateApplicationWebContract.CandidateApplicationView createCandidateApplication(
      CandidateApplicationWebContract.CandidateApplicationInput request) {

    CandidateProfileEntity entity = entityAssembler.toEntity(
        request,
        cvPreviewService.metadataOnlyExtraction(),
        List.of());
    return saveCandidateApplication(entity);
  }

  @Transactional
  public CandidateApplicationWebContract.CandidateApplicationView createCandidateApplicationFromCvFile(
      String contactEmail,
      MultipartFile cvFile,
      List<MultipartFile> certificateFiles,
      CandidateCvWebContract.CandidateCvProfileWorkingCopyInput profileWorkingCopy,
      CandidateApplicationWebContract.CandidateProfileSummaryInput generatedSummary) {

    requireEmail(contactEmail, "contactEmail is required");
    if (cvFile == null || cvFile.isEmpty()) {
      throw reject("cvFile is required");
    }

    CandCvTextExtractionService.ExtractionResult extraction = cvPreviewService.extract(cvFile);
    CandidateApplicationWebContract.CandidateApplicationInput request =
        new CandidateApplicationWebContract.CandidateApplicationInput(
            contactEmail,
            safeText(cvFile.getOriginalFilename()),
            safeText(cvFile.getContentType()).isBlank()
                ? "application/octet-stream"
                : safeText(cvFile.getContentType()),
            cvFile.getSize(),
            profileWorkingCopy,
            generatedSummary);
    CandidateProfileEntity entity = entityAssembler.toEntity(request, extraction, certificateFiles);
    return saveCandidateApplication(entity);
  }

  @Transactional(readOnly = true)
  public CandidateCvWebContract.CandidateCvPreviewView previewCandidateCv(MultipartFile cvFile) {
    return cvPreviewService.previewCandidateCv(cvFile);
  }

  @Transactional(readOnly = true)
  public List<CandidateApplicationWebContract.CandidateApplicationView> candidateApplicationsArchive() {
    return applicationSnapshotService.findAllArchivedViews();
  }

  private CandidateApplicationWebContract.CandidateApplicationView saveCandidateApplication(
      CandidateProfileEntity entity) {

    CandidateProfileEntity saved = candidateProfileRepo.save(entity);
    recordBusinessEvent(
        "registeredCandidateProfile.registered",
        SUBMITTED_STATUS,
        PUBLIC_TENANT,
        saved.getId());
    List<MissionMatchDiscoveryResult> findMissionResults = matchDiscoveryService.findMissionsForCandidate(saved.getId());
    CandidateApplicationWebContract.CandidateApplicationView view =
        webViewAssembler.toApplicationView(saved, findMissionResults);
    applicationSnapshotService.saveInitialSubmissionSnapshot(view);
    return view;
  }

  private void recordBusinessEvent(
      String eventType,
      String status,
      String tenantId,
      Long recordId) {

    auditService.recordBusinessEvent(
        eventType,
        status,
        tenantId,
        CANDIDATE_RECORD_TYPE,
        recordId,
        "status=" + status);
  }
}
