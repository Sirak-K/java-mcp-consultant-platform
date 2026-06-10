package mcp.server.domain.candidate_presentation.application.generation;

import java.util.Objects;

import org.springframework.stereotype.Service;

import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactView;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactLogService;
import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationArtifactService;

import mcp.server.domain.candidate_presentation.exception.CandidatePresentationException;
import mcp.server.foundation.ai.generation.AiGenerationRunAccepted;
import mcp.server.foundation.ai.generation.AiGenerationRunRequest;
import mcp.server.foundation.ai.generation.AiGenerationRuntimeClient;
import mcp.server.foundation.ai.generation.AiGenerationRuntimeException;

@Service
public final class CandidatePresentationGenerationStartService {

  private static final String START_STATUS_ACCEPTED = "accepted";
  private static final String START_STATUS_FAILED_TO_START = "failed_to_start";

  private final CandidatePresentationArtifactService artifactService;
  private final AiGenerationRuntimeClient generationRuntimeClient;
  private final CandidatePresentationArtifactLogService logService;

  public CandidatePresentationGenerationStartService(
      CandidatePresentationArtifactService artifactService,
      AiGenerationRuntimeClient generationRuntimeClient,
      CandidatePresentationArtifactLogService logService) {

    this.artifactService = Objects.requireNonNull(artifactService, "artifactService");
    this.generationRuntimeClient = Objects.requireNonNull(generationRuntimeClient, "generationRuntimeClient");
    this.logService = Objects.requireNonNull(logService, "logService");
  }

  public CandidatePresentationGenerationStartResult startCandidatePresentationGeneration(long matchId) {
    CandidatePresentationArtifactService.CandidatePresentationGenerationStartArtifact startArtifact;
    CandidatePresentationArtifactView artifact;
    try {
      startArtifact = artifactService.createOrPrepareForGenerationStart(matchId);
      artifact = startArtifact.artifact();
    } catch (CandidatePresentationException e) {
      String failureMessage = safeMessage(e);
      logService.logGenerationStartFailed(matchId, null, e);
      return new CandidatePresentationGenerationStartResult(
          CandidatePresentationGenerationStartOutcome.FAILED_TO_START,
          failedToStartView(matchId, null, null, failureMessage));
    }
    try {
      logService.logGenerationRunTriggerStarted(
          matchId,
          artifact.id());
      AiGenerationRunAccepted generationRun = generationRuntimeClient
          .startGenerationRun(generationRunInput(matchId, artifact));
      if (!generationRunAccepted(generationRun)) {
        throw new AiGenerationRuntimeException(
            "Generation runtime did not accept generation run start.");
      }
      logService.logGenerationStartAccepted(
          artifact,
          generationRun.runId());
      return new CandidatePresentationGenerationStartResult(
          CandidatePresentationGenerationStartOutcome.ACCEPTED,
          acceptedView(matchId, artifact, generationRun.runId()));
    } catch (RuntimeException e) {
      String failureMessage = safeMessage(e);
      CandidatePresentationArtifactView failedArtifact = artifact;
      if (startArtifact.preparedForThisStart()) {
        failedArtifact = artifactService.recordGenerationStartFailure(artifact.id(), failureMessage);
      }
      logService.logGenerationStartFailed(matchId, artifact.id(), e);
      return new CandidatePresentationGenerationStartResult(
          CandidatePresentationGenerationStartOutcome.FAILED_TO_START,
          failedToStartView(matchId, failedArtifact.id(), failedArtifact.artifactStatus(), failureMessage));
    }
  }

  private AiGenerationRunRequest generationRunInput(
      long matchId,
      CandidatePresentationArtifactView artifact) {

    return new AiGenerationRunRequest(
        String.valueOf(matchId),
        String.valueOf(artifact.id()));
  }

  private static CandidatePresentationGenerationStartView acceptedView(
      long matchId,
      CandidatePresentationArtifactView artifact,
      String runId) {

    return new CandidatePresentationGenerationStartView(
        START_STATUS_ACCEPTED,
        matchId,
        artifact.id(),
        artifact.artifactStatus(),
        runId,
        "Candidate Presentation generation accepted.",
        artifact);
  }

  private static CandidatePresentationGenerationStartView failedToStartView(
      long matchId,
      Long artifactId,
      String artifactStatus,
      String message) {

    return new CandidatePresentationGenerationStartView(
        START_STATUS_FAILED_TO_START,
        matchId,
        artifactId,
        artifactStatus,
        "",
        message,
        null);
  }

  private static boolean generationRunAccepted(
      AiGenerationRunAccepted generationRun) {

    return generationRun != null
        && "accepted".equalsIgnoreCase(blankTo(generationRun.runState(), ""))
        && Boolean.TRUE.equals(generationRun.backgroundStarted())
        && !blankTo(generationRun.runId(), "").isBlank();
  }

  private static String safeMessage(RuntimeException exception) {
    if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
      return "Candidate Presentation generation could not be started.";
    }
    return exception.getMessage();
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  public record CandidatePresentationGenerationStartResult(
      CandidatePresentationGenerationStartOutcome outcome,
      CandidatePresentationGenerationStartView view) {
  }

  public enum CandidatePresentationGenerationStartOutcome {
    ACCEPTED,
    FAILED_TO_START
  }
}
