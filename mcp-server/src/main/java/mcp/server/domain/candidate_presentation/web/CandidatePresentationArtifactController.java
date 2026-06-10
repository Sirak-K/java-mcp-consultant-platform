package mcp.server.domain.candidate_presentation.web;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactService;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationStartService;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationStartService.CandidatePresentationGenerationStartOutcome;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationStartService.CandidatePresentationGenerationStartResult;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationStartView;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactLogService;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactEditCommand;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactView;

@RestController
public final class CandidatePresentationArtifactController {

  private final CandidatePresentationArtifactService artifactService;
  private final CandidatePresentationGenerationStartService generationStartService;
  private final CandidatePresentationArtifactLogService logService;

  public CandidatePresentationArtifactController(
      CandidatePresentationArtifactService artifactService,
      CandidatePresentationGenerationStartService generationStartService,
      CandidatePresentationArtifactLogService logService) {
    this.artifactService = Objects.requireNonNull(artifactService, "artifactService");
    this.generationStartService = Objects.requireNonNull(generationStartService, "generationStartService");
    this.logService = Objects.requireNonNull(logService, "logService");
  }

  @GetMapping("/api/ops/candidate-presentation-artifacts")
  public List<CandidatePresentationArtifactView> candidatePresentationArtifacts() {
    logService.logRestEndpointCalled("candidatePresentationArtifacts", null, null);
    return artifactService.artifactsForOpsReview();
  }

  @GetMapping("/api/ops/candidate-presentation-artifacts/{id}")
  public CandidatePresentationArtifactView candidatePresentationArtifact(
      @PathVariable("id") long id) {
    logService.logRestEndpointCalled("candidatePresentationArtifact", null, id);
    return artifactService.artifactForOpsReview(id);
  }

  @PutMapping("/api/ops/candidate-presentation-artifacts/{id}")
  public CandidatePresentationArtifactView editCandidatePresentationArtifact(
      @PathVariable("id") long id,
      @RequestBody CandidatePresentationArtifactEditRequest request) {
    logService.logRestEndpointCalled("editCandidatePresentationArtifact", null, id);
    return artifactService.editArtifactForOpsReview(
        id,
        new CandidatePresentationArtifactEditCommand(
            request.customerFacingContentJson(),
            request.opsReviewContentJson(),
            request.evidenceTraceJson()));
  }

  @PostMapping("/api/ops/matches/{matchId}/candidate-presentation-generation")
  public ResponseEntity<CandidatePresentationGenerationStartView> startCandidatePresentationGeneration(
      @PathVariable("matchId") long matchId) {
    logService.logRestEndpointCalled("startCandidatePresentationGeneration", matchId, null);
    CandidatePresentationGenerationStartResult result = generationStartService.startCandidatePresentationGeneration(matchId);
    return ResponseEntity.status(httpStatus(result.outcome())).body(result.view());
  }

  private static HttpStatus httpStatus(CandidatePresentationGenerationStartOutcome outcome) {
    return switch (outcome) {
      case ACCEPTED -> HttpStatus.ACCEPTED;
      case FAILED_TO_START -> HttpStatus.SERVICE_UNAVAILABLE;
    };
  }
}
