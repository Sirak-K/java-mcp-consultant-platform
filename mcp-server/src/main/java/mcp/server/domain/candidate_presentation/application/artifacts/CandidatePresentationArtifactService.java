package mcp.server.domain.candidate_presentation.application.artifacts;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_presentation.api.CandidatePresentationArtifactCleanup;
import mcp.server.domain.candidate_presentation.application.evidence.CandidatePresentationEvidenceView;
import mcp.server.domain.candidate_presentation.application.evidence.CandidatePresentationEvidenceService;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationCatalogService;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationContractService;
import mcp.server.domain.candidate_presentation.application.materialization.CandidatePresentationArtifactMaterializationService;

import mcp.server.domain.candidate_presentation.exception.CandidatePresentationException;
import mcp.server.domain.candidate_presentation.persistence.CandidatePresentationArtifactEntity;
import mcp.server.domain.candidate_presentation.persistence.CandidatePresentationArtifactJpaRepository;

@Service
public class CandidatePresentationArtifactService implements CandidatePresentationArtifactCleanup {

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

  private final CandidatePresentationArtifactJpaRepository artifactRepo;
  private final CandidatePresentationEvidenceService evidenceService;
  private final CandidatePresentationGenerationContractService generationContractService;
  private final CandidatePresentationArtifactMaterializationService artifactMaterializationService;
  private final CandidatePresentationGenerationCatalogService catalogService;
  private final CandidatePresentationArtifactLogService logService;
  private final ObjectMapper objectMapper;

  public CandidatePresentationArtifactService(
      CandidatePresentationArtifactJpaRepository artifactRepo,
      CandidatePresentationEvidenceService evidenceService,
      CandidatePresentationGenerationContractService generationContractService,
      CandidatePresentationArtifactMaterializationService artifactMaterializationService,
      CandidatePresentationGenerationCatalogService catalogService,
      CandidatePresentationArtifactLogService logService,
      ObjectMapper objectMapper) {
    this.artifactRepo = Objects.requireNonNull(artifactRepo, "artifactRepo");
    this.evidenceService = Objects.requireNonNull(evidenceService, "evidenceService");
    this.generationContractService = Objects.requireNonNull(generationContractService, "generationContractService");
    this.artifactMaterializationService = Objects.requireNonNull(
        artifactMaterializationService,
        "artifactMaterializationService");
    this.catalogService = Objects.requireNonNull(catalogService, "catalogService");
    this.logService = Objects.requireNonNull(logService, "logService");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public record CandidatePresentationGenerationStartArtifact(
      CandidatePresentationArtifactView artifact,
      boolean preparedForThisStart) {
  }

  @Transactional(readOnly = true)
  public List<CandidatePresentationArtifactView> artifactsForOpsReview() {
    return artifactRepo.findAllByOrderByUpdatedAtDesc().stream()
        .map(this::toView)
        .toList();
  }

  @Transactional(readOnly = true)
  public CandidatePresentationArtifactView artifactForOpsReview(long artifactId) {
    return toView(requireArtifact(artifactId));
  }

  @Transactional
  @Override
  public int deleteArtifactsForCandidateProfile(long candProfileId) {
    List<CandidatePresentationArtifactEntity> artifacts = artifactRepo
        .findAllByCandProfileIdOrderByUpdatedAtDesc(candProfileId);
    artifacts.forEach(artifact -> artifactMaterializationService.deleteMaterializedArtifact(artifact.getId()));
    artifactRepo.deleteAll(artifacts);
    return artifacts.size();
  }

  @Transactional
  public CandidatePresentationGenerationStartArtifact createOrPrepareForGenerationStart(long matchId) {
    return artifactRepo.findBySourceCandidateToSlotMatchId(matchId)
        .map(artifact -> prepareExistingArtifactForGenerationStart(matchId, artifact))
        .orElseGet(() -> createFromMatchForGenerationStart(matchId));
  }

  @Transactional
  public CandidatePresentationArtifactView editArtifactForOpsReview(
      long artifactId,
      CandidatePresentationArtifactEditCommand request) {

    CandidatePresentationArtifactEntity artifact = requireArtifact(artifactId);
    String oldStatus = artifact.getArtifactStatus();
    requireStatus(
        artifact,
        "edit candidate presentation artifact",
        CandidatePresentationArtifactEntity.STATUS_GENERATED,
        CandidatePresentationArtifactEntity.STATUS_OPS_REVIEW);
    String customerFacingContentJson = requireNonBlankJsonText(
        request.customerFacingContentJson(),
        "customerFacingContentJson");
    String opsReviewContentJson = requireNonBlankJsonText(
        request.opsReviewContentJson(),
        "opsReviewContentJson");
    String evidenceTraceJson = requireNonBlankJsonText(
        request.evidenceTraceJson(),
        "evidenceTraceJson");
    generationContractService.validateGeneratedContentJson(
        customerFacingContentJson,
        opsReviewContentJson,
        evidenceTraceJson);
    artifact.markOpsReview(
        presentationTitle(artifact.getSourceCandidateToSlotMatchId()),
        customerFacingContentJson,
        opsReviewContentJson,
        evidenceTraceJson,
        Instant.now());
    CandidatePresentationArtifactEntity saved = artifactRepo.save(artifact);
    artifactMaterializationService.materialize(saved);
    logService.logStatusTransition(
        "editArtifactForOpsReview",
        saved,
        oldStatus,
        "OPS edited artifact for review");
    logService.logLifecycleServiceResult(
        "editArtifactForOpsReview",
        saved.getSourceCandidateToSlotMatchId(),
        saved.getId(),
        saved.getArtifactStatus());
    return toView(saved);
  }

  @Transactional
  public CandidatePresentationArtifactView recordGeneratedContent(
      CandidatePresentationGeneratedContentCommand request) {

    CandidatePresentationArtifactEntity artifact = requireArtifact(request.artifactId());
    String oldStatus = artifact.getArtifactStatus();
    requireStatus(
        artifact,
        "record generated candidate presentation content",
        CandidatePresentationArtifactEntity.STATUS_PENDING_GENERATION);
    try {
      String customerFacingContentJson = requireNonBlankJsonText(
          request.customerFacingContentJson(),
          "customerFacingContentJson");
      String opsReviewContentJson = requireNonBlankJsonText(
          request.opsReviewContentJson(),
          "opsReviewContentJson");
      String evidenceTraceJson = requireNonBlankJsonText(
          request.evidenceTraceJson(),
          "evidenceTraceJson");
      generationContractService.validateGeneratedContentJson(
          customerFacingContentJson,
          opsReviewContentJson,
          evidenceTraceJson);
      artifact.markGenerated(
          presentationTitle(artifact.getSourceCandidateToSlotMatchId()),
          customerFacingContentJson,
          opsReviewContentJson,
          evidenceTraceJson,
          Instant.now());
    } catch (CandidatePresentationException exception) {
      logService.logWritebackValidationFailed(
          "recordGeneratedContent",
          artifact,
          exception);
      throw exception;
    }
    CandidatePresentationArtifactEntity saved = artifactRepo.save(artifact);
    artifactMaterializationService.materialize(saved);
    logService.logMcpWritebackRecorded(
        "recordGeneratedContent",
        saved,
        oldStatus,
        "agent generated content accepted");
    logService.logLifecycleServiceResult(
        "recordGeneratedContent",
        saved.getSourceCandidateToSlotMatchId(),
        saved.getId(),
        saved.getArtifactStatus());
    return toView(saved);
  }

  @Transactional
  public CandidatePresentationArtifactView recordGenerationFailure(
      CandidatePresentationGenerationFailureCommand request) {

    CandidatePresentationArtifactEntity artifact = requireArtifact(request.artifactId());
    String oldStatus = artifact.getArtifactStatus();
    requireStatus(
        artifact,
        "record candidate presentation generation failure",
        CandidatePresentationArtifactEntity.STATUS_PENDING_GENERATION);
    Instant now = Instant.now();
    try {
      GenerationFailureValues failureValues = generationFailureValues(request);
      artifact.markGenerationFailed(
          generationFailureOpsReviewJson(artifact, failureValues, now),
          generationFailureEvidenceTraceJson(artifact, failureValues, now),
          now);
    } catch (CandidatePresentationException exception) {
      logService.logWritebackValidationFailed(
          "recordGenerationFailure",
          artifact,
          exception);
      throw exception;
    }
    CandidatePresentationArtifactEntity saved = artifactRepo.save(artifact);
    logService.logMcpWritebackRecorded(
        "recordGenerationFailure",
        saved,
        oldStatus,
        "agent generation failure accepted");
    logService.logLifecycleServiceResult(
        "recordGenerationFailure",
        saved.getSourceCandidateToSlotMatchId(),
        saved.getId(),
        saved.getArtifactStatus());
    return toView(saved);
  }

  @Transactional
  public CandidatePresentationArtifactView recordGenerationStartFailure(
      long artifactId,
      String failureDetail) {

    CandidatePresentationArtifactEntity artifact = requireArtifact(artifactId);
    String oldStatus = artifact.getArtifactStatus();
    requireStatus(
        artifact,
        "record candidate presentation generation start failure",
        CandidatePresentationArtifactEntity.STATUS_PENDING_GENERATION);
    Instant now = Instant.now();
    artifact.markGenerationFailed(
        generationStartFailureOpsReviewJson(artifact, failureDetail, now),
        generationStartFailureEvidenceTraceJson(artifact, failureDetail, now),
        now);
    CandidatePresentationArtifactEntity saved = artifactRepo.save(artifact);
    logService.logStatusTransition(
        "recordGenerationStartFailure",
        saved,
        oldStatus,
        "generation runtime did not accept generation start");
    logService.logLifecycleServiceResult(
        "recordGenerationStartFailure",
        saved.getSourceCandidateToSlotMatchId(),
        saved.getId(),
        saved.getArtifactStatus());
    return toView(saved);
  }

  private CandidatePresentationGenerationStartArtifact createFromMatchForGenerationStart(long matchId) {
    CandidatePresentationArtifactEntity saved = createPendingGenerationArtifact(matchId);
    logService.logArtifactCreatedForGenerationStart(saved);
    logService.logLifecycleServiceResult(
        "createOrPrepareForGenerationStart",
        saved.getSourceCandidateToSlotMatchId(),
        saved.getId(),
        saved.getArtifactStatus());
    return new CandidatePresentationGenerationStartArtifact(toView(saved), true);
  }

  private CandidatePresentationArtifactEntity createPendingGenerationArtifact(long matchId) {
    CandidatePresentationEvidenceView evidence = evidenceService.collectEvidence(matchId);
    Instant now = Instant.now();
    CandidatePresentationArtifactEntity artifact = CandidatePresentationArtifactEntity.pendingGenerationDraft(
        evidence.matchContext().matchId(),
        evidence.candidateContext().candidateProfileId(),
        evidence.missionContext().missionId(),
        evidence.missionContext().missionSlotId(),
        presentationTitle(evidence),
        customerFacingDraftJson(),
        opsReviewDraftJson(evidence),
        toJson(evidence),
        now);
    return artifactRepo.save(artifact);
  }

  private CandidatePresentationGenerationStartArtifact prepareExistingArtifactForGenerationStart(
      long matchId,
      CandidatePresentationArtifactEntity artifact) {

    if (Objects.equals(artifact.getArtifactStatus(), CandidatePresentationArtifactEntity.STATUS_PENDING_GENERATION)) {
      logService.logArtifactLinkedForGenerationStart(matchId, artifact);
      logService.logLifecycleServiceResult(
          "createOrPrepareForGenerationStart",
          matchId,
          artifact.getId(),
          artifact.getArtifactStatus());
      return new CandidatePresentationGenerationStartArtifact(toView(artifact), false);
    }

    CandidatePresentationEvidenceView evidence = evidenceService.collectEvidence(matchId);
    String oldStatus = artifact.getArtifactStatus();
    artifact.markPendingGeneration(
        presentationTitle(evidence),
        customerFacingDraftJson(),
        opsReviewDraftJson(evidence),
        toJson(evidence),
        Instant.now());
    CandidatePresentationArtifactEntity saved = artifactRepo.save(artifact);
    logService.logStatusTransition(
        "createOrPrepareForGenerationStart",
        saved,
        oldStatus,
        "artifact prepared for generation restart");
    logService.logLifecycleServiceResult(
        "createOrPrepareForGenerationStart",
        matchId,
        saved.getId(),
        saved.getArtifactStatus());
    return new CandidatePresentationGenerationStartArtifact(toView(saved), true);
  }

  private CandidatePresentationArtifactEntity requireArtifact(long artifactId) {
    return artifactRepo.findById(artifactId)
        .orElseThrow(() -> CandidatePresentationException.notFound("candidatePresentationArtifact not found"));
  }

  private void requireStatus(
      CandidatePresentationArtifactEntity artifact,
      String action,
      String... allowedStatuses) {
    String currentStatus = artifact.getArtifactStatus();
    for (String allowedStatus : allowedStatuses) {
      if (Objects.equals(currentStatus, allowedStatus)) {
        return;
      }
    }
    logService.logLifecycleDenied(artifact, action, allowedStatuses);
    throw CandidatePresentationException.conflict(action + " requires artifact status "
            + String.join(" or ", allowedStatuses)
            + " but was "
            + currentStatus);
  }

  private CandidatePresentationArtifactView toView(CandidatePresentationArtifactEntity artifact) {
    return new CandidatePresentationArtifactView(
        artifact.getId(),
        artifact.getSourceCandidateToSlotMatchId(),
        artifact.getCandProfileId(),
        artifact.getMissionId(),
        artifact.getMissionSlotId(),
        artifact.getArtifactStatus(),
        presentationTitle(artifact.getSourceCandidateToSlotMatchId()),
        artifact.getCustomerFacingContentJson(),
        artifact.getOpsReviewContentJson(),
        artifact.getEvidenceTraceJson(),
        formatInstant(artifact.getCreatedAt()),
        formatInstant(artifact.getUpdatedAt()));
  }

  private String requireText(String value, String message) {
    String text = value == null ? "" : value.trim();
    if (text.isBlank()) {
      throw CandidatePresentationException.invalidRequest(message);
    }
    return text;
  }

  private String requireJsonText(String value, String fieldName) {
    String json = value == null || value.isBlank() ? "{}" : value.trim();
    try {
      objectMapper.readTree(json);
      return json;
    } catch (JsonProcessingException exception) {
      throw CandidatePresentationException.invalidRequest(fieldName + " must contain valid JSON", exception);
    }
  }

  private String requireNonBlankJsonText(String value, String fieldName) {
    return requireJsonText(requireText(value, fieldName + " is required"), fieldName);
  }

  private String presentationTitle(CandidatePresentationEvidenceView evidence) {
    return presentationTitle(evidence.matchContext().matchId());
  }

  private String presentationTitle(long matchId) {
    return CandidatePresentationTitleFormat.forMatchId(matchId);
  }

  private String textOrFallback(String value, String fallback) {
    String text = value == null ? "" : value.trim();
    return text.isBlank() ? fallback : text;
  }

  private String blankTo(String value, String fallback) {
    String text = value == null ? "" : value.trim();
    return text.isBlank() ? fallback : text;
  }

  private String customerFacingDraftJson() {
    return toJson(catalogService.customerFacingDraftContent());
  }

  private String opsReviewDraftJson(CandidatePresentationEvidenceView evidence) {
    LinkedHashMap<String, String> draft = new LinkedHashMap<>();
    catalogService.opsReviewDraftContent()
        .forEach((sectionKey, placeholder) -> draft.put(
            sectionKey,
            renderOpsReviewDraftPlaceholder(placeholder, evidence)));
    return toJson(draft);
  }

  private String renderOpsReviewDraftPlaceholder(
      String placeholder,
      CandidatePresentationEvidenceView evidence) {

    return placeholder
        .replace("{matchScoreLabel}", textOrFallback(evidence.matchContext().scoreLabel(), "unknown"))
        .replace("{matchedSkillCount}", Integer.toString(evidence.matchContext().matchedSkillCount()))
        .replace("{requiredSkillCount}", Integer.toString(evidence.matchContext().requiredSkillCount()))
        .replace("{missingOrWeakFactors}", missingOrWeakFactorsText(evidence));
  }

  private String missingOrWeakFactorsText(CandidatePresentationEvidenceView evidence) {
    List<String> factors = evidence.internalEvidenceTrace().missingOrWeakFactors();
    if (factors == null || factors.isEmpty()) {
      return "none";
    }
    return String.join("; ", factors);
  }

  private String generationFailureOpsReviewJson(
      CandidatePresentationArtifactEntity artifact,
      GenerationFailureValues failureValues,
      Instant failedAt) {
    return toJson(new GenerationFailureReview(
        failureValues.failureMessage(),
        failureValues.failureDetail(),
        failureValues.failureStage(),
        failureValues.runId(),
        failureValues.modelAlias(),
        artifact.getSourceCandidateToSlotMatchId(),
        artifact.getId(),
        formatInstant(failedAt)));
  }

  private String generationFailureEvidenceTraceJson(
      CandidatePresentationArtifactEntity artifact,
      GenerationFailureValues failureValues,
      Instant failedAt) {
    return toJson(List.of(new GenerationFailureTrace(
        failureValues.failureStage(),
        failureValues.runId(),
        artifact.getSourceCandidateToSlotMatchId(),
        artifact.getId(),
        failureValues.failureMessage(),
        failureValues.failureDetail(),
        failureValues.modelAlias(),
        formatInstant(failedAt))));
  }

  private GenerationFailureValues generationFailureValues(
      CandidatePresentationGenerationFailureCommand request) {
    return new GenerationFailureValues(
        requireText(request.failureMessage(), "failureMessage is required"),
        request.failureDetail() == null ? "" : request.failureDetail().trim(),
        requireText(request.failureStage(), "failureStage is required"),
        requireText(request.runId(), "runId is required"),
        request.modelAlias() == null ? "" : request.modelAlias().trim());
  }

  private String generationStartFailureOpsReviewJson(
      CandidatePresentationArtifactEntity artifact,
      String failureDetail,
      Instant failedAt) {
    return toJson(new GenerationFailureReview(
        "Candidate Presentation generation could not be started.",
        blankTo(failureDetail, "Generation runtime did not accept generation start."),
        "generation_runtime_start",
        "not_started",
        "",
        artifact.getSourceCandidateToSlotMatchId(),
        artifact.getId(),
        formatInstant(failedAt)));
  }

  private String generationStartFailureEvidenceTraceJson(
      CandidatePresentationArtifactEntity artifact,
      String failureDetail,
      Instant failedAt) {
    return toJson(List.of(new GenerationFailureTrace(
        "generation_runtime_start",
        "not_started",
        artifact.getSourceCandidateToSlotMatchId(),
        artifact.getId(),
        "Candidate Presentation generation could not be started.",
        blankTo(failureDetail, "Generation runtime did not accept generation start."),
        "",
        formatInstant(failedAt))));
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw CandidatePresentationException.internalError("candidate presentation artifact JSON could not be written", exception);
    }
  }

  private String formatInstant(Instant instant) {
    return TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
  }

  private record GenerationFailureReview(
      String failureMessage,
      String failureDetail,
      String failureStage,
      String runId,
      String modelAlias,
      long matchId,
      long artifactId,
      String failedAt) {
  }

  private record GenerationFailureTrace(
      String failureStage,
      String runId,
      long matchId,
      long artifactId,
      String failureMessage,
      String failureDetail,
      String modelAlias,
      String failedAt) {
  }

  private record GenerationFailureValues(
      String failureMessage,
      String failureDetail,
      String failureStage,
      String runId,
      String modelAlias) {
  }
}
