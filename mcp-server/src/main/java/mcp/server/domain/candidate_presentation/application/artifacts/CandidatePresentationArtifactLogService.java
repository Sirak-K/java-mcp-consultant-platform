package mcp.server.domain.candidate_presentation.application.artifacts;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import mcp.server.domain.candidate_presentation.persistence.CandidatePresentationArtifactEntity;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.context.ObservCtxHolder;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;

@Service
public final class CandidatePresentationArtifactLogService {

  private static final ServerLogger.Component RUNTIME = ServerLogger.Component.RUNTIME;
  private static final ServerLogger.Component MCP = ServerLogger.Component.MCP;

  private final ServerLogger serverLogger;
  private final ObservCtxFactory obsCtxFactory;

  public CandidatePresentationArtifactLogService(
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory) {

    this.serverLogger = Objects.requireNonNull(serverLogger, "serverLogger");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
  }

  public void logRestEndpointCalled(
      String operation,
      Long matchId,
      Long artifactId) {

    serverLogger.ServerLogInfoObserved(
        RUNTIME,
        context(),
        "CALL",
        "CANDIDATE_PRESENTATION_REST_ENDPOINT_CALLED",
        "Candidate Presentation REST endpoint called: " + details(operation, matchId, artifactId, null, null, "allow=true"));
  }

  public void logMcpToolCalled(
      String toolName,
      Long matchId,
      Long artifactId) {

    serverLogger.ServerLogInfoObserved(
        MCP,
        context(),
        "CALL",
        "CANDIDATE_PRESENTATION_MCP_TOOL_CALLED",
        "Candidate Presentation MCP tool called: "
            + details(toolName, matchId, artifactId, null, null, "allow=true"));
  }

  public void logLifecycleServiceResult(
      String operation,
      Long matchId,
      Long artifactId,
      String artifactStatus) {

    serverLogger.ServerLogInfoObserved(
        RUNTIME,
        context(),
        "RESULT",
        "CANDIDATE_PRESENTATION_LIFECYCLE_SERVICE_RESULT",
        "Candidate Presentation lifecycle service result: "
            + details(operation, matchId, artifactId, null, artifactStatus));
  }

  public void logArtifactLinkedForGenerationStart(
      long matchId,
      CandidatePresentationArtifactEntity artifact) {

    serverLogger.ServerLogAuditInfoObserved(
        RUNTIME,
        context(),
        "LINK",
        "CANDIDATE_PRESENTATION_ARTIFACT_LINKED_FOR_GENERATION_START",
        "Candidate Presentation artifact linked for generation start: "
            + details(
                "createOrPrepareForGenerationStart",
                matchId,
                artifact.getId(),
                artifact.getArtifactStatus(),
                artifact.getArtifactStatus(),
                "actor=" + actorId(),
                "allow=true",
                "reason=existing generation target reused"));
  }

  public void logArtifactCreatedForGenerationStart(
      CandidatePresentationArtifactEntity artifact) {

    serverLogger.ServerLogAuditInfoObserved(
        RUNTIME,
        context(),
        "CREATE",
        "CANDIDATE_PRESENTATION_ARTIFACT_CREATED_FOR_GENERATION_START",
        "Candidate Presentation artifact created for generation start: "
            + details(
                "createOrPrepareForGenerationStart",
                artifact.getSourceCandidateToSlotMatchId(),
                artifact.getId(),
                "N/A",
                artifact.getArtifactStatus(),
                "actor=" + actorId(),
                "allow=true",
                "reason=generation target artifact created"));
  }

  public void logGenerationRunTriggerStarted(
      long matchId,
      long artifactId) {

    serverLogger.ServerLogInfoObserved(
        RUNTIME,
        context(),
        "CALL",
        "CANDIDATE_PRESENTATION_GENERATION_RUN_TRIGGER_STARTED",
        "Candidate Presentation generation run trigger started: "
            + details(
                "candidatePresentationGenerationRun",
                matchId,
                artifactId,
                null,
                null,
                "actor=" + actorId(),
                "allow=true"));
  }

  public void logGenerationStartAccepted(
      CandidatePresentationArtifactView artifact,
      String runId) {

    serverLogger.ServerLogAuditInfoObserved(
        RUNTIME,
        context(),
        "START",
        "CANDIDATE_PRESENTATION_GENERATION_START_ACCEPTED",
        "Candidate Presentation generation start accepted: "
            + details(
                "candidatePresentationGenerationStart",
                artifact.sourceCandidateToSlotMatchId(),
                artifact.id(),
                artifact.artifactStatus(),
                artifact.artifactStatus(),
                "actor=" + actorId(),
                "allow=true",
                "runId=" + runId));
  }

  public void logGenerationStartFailed(
      long matchId,
      Long artifactId,
      Throwable throwable) {

    serverLogger.ServerLogErrorObserved(
        RUNTIME,
        context(),
        "START",
        "CANDIDATE_PRESENTATION_GENERATION_START_FAILED",
        "Candidate Presentation generation start failed: "
            + details(
                "candidatePresentationGenerationStart",
                matchId,
                artifactId,
                null,
                null,
                "actor=" + actorId(),
                "allow=false",
                "reason=" + safeReason(throwable)),
        "CANDIDATE_PRESENTATION_GENERATION_START_FAILED",
        throwable);
  }

  public void logStatusTransition(
      String operation,
      CandidatePresentationArtifactEntity artifact,
      String oldStatus,
      String reason) {

    serverLogger.ServerLogAuditInfoObserved(
        RUNTIME,
        context(),
        "TRANSITION",
        "CANDIDATE_PRESENTATION_ARTIFACT_STATUS_TRANSITION",
        "Candidate Presentation artifact status transition: "
            + details(
                operation,
                artifact.getSourceCandidateToSlotMatchId(),
                artifact.getId(),
                oldStatus,
                artifact.getArtifactStatus(),
                "actor=" + actorId(),
                "allow=true",
                "reason=" + reason));
  }

  public void logMcpWritebackRecorded(
      String operation,
      CandidatePresentationArtifactEntity artifact,
      String oldStatus,
      String reason) {

    serverLogger.ServerLogAuditInfoObserved(
        MCP,
        context(),
        "WRITEBACK",
        "CANDIDATE_PRESENTATION_MCP_RESULT_RECORDED",
        "Candidate Presentation MCP result recorded: "
            + details(
                operation,
                artifact.getSourceCandidateToSlotMatchId(),
                artifact.getId(),
                oldStatus,
                artifact.getArtifactStatus(),
                "actor=" + actorId(),
                "allow=true",
                "reason=" + reason));
  }

  public void logLifecycleDenied(
      CandidatePresentationArtifactEntity artifact,
      String operation,
      String... allowedStatuses) {

    String allowed = String.join("/", allowedStatuses);
    String denyDetails = details(
        operation,
        artifact.getSourceCandidateToSlotMatchId(),
        artifact.getId(),
        artifact.getArtifactStatus(),
        "N/A",
        "actor=" + actorId(),
        "allow=false",
        "reason=requires " + allowed);

    serverLogger.ServerLogAuditWarnObserved(
        RUNTIME,
        context(),
        "DENY",
        "CANDIDATE_PRESENTATION_LIFECYCLE_OPERATION_DENIED",
        "Candidate Presentation lifecycle operation denied: " + denyDetails,
        "CANDIDATE_PRESENTATION_LIFECYCLE_OPERATION_DENIED");

    serverLogger.ServerLogErrorObserved(
        RUNTIME,
        context(),
        "VALIDATE",
        "CANDIDATE_PRESENTATION_INVALID_LIFECYCLE_TRANSITION",
        "Candidate Presentation invalid lifecycle transition: " + denyDetails,
        "CANDIDATE_PRESENTATION_INVALID_LIFECYCLE_TRANSITION",
        null);
  }

  public void logWritebackValidationFailed(
      String operation,
      CandidatePresentationArtifactEntity artifact,
      Throwable throwable) {

    serverLogger.ServerLogErrorObserved(
        MCP,
        context(),
        "VALIDATE",
        "CANDIDATE_PRESENTATION_RESULT_VALIDATION_FAILED",
        "Candidate Presentation MCP result validation failed: "
            + details(
                operation,
                artifact.getSourceCandidateToSlotMatchId(),
                artifact.getId(),
                artifact.getArtifactStatus(),
                "N/A",
                "actor=" + actorId(),
                "allow=false",
                "reason=" + safeReason(throwable)),
        "CANDIDATE_PRESENTATION_RESULT_VALIDATION_FAILED",
        throwable);
  }

  private ObservCtx context() {
    return obsCtxFactory.ObservCtxFactoryCurrentOrEmpty();
  }

  private String actorId() {
    ReqsAuthBinding binding = currentReqsAuthBinding();
    if (binding == null || binding.principalId() == null || binding.principalId().isBlank()) {
      return "unknown";
    }
    return sanitize(binding.principalId());
  }

  private static ReqsAuthBinding currentReqsAuthBinding() {
    ObservCtx context = ObservCtxHolder.ObservCtxHolderGet();
    if (context != null && context.ObservCtxGetReqsAuthBinding() != null) {
      return context.ObservCtxGetReqsAuthBinding();
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof ReqsAuthBinding binding) {
      return binding;
    }

    return null;
  }

  private static String details(
      String operation,
      Long matchId,
      Long artifactId,
      String oldStatus,
      String newStatus,
      String... extra) {

    StringBuilder builder = new StringBuilder();
    append(builder, "operation", operation);
    append(builder, "matchId", matchId);
    append(builder, "artifactId", artifactId);
    append(builder, "oldStatus", oldStatus);
    append(builder, "newStatus", newStatus);
    Arrays.stream(extra)
        .filter(Objects::nonNull)
        .filter(value -> !value.isBlank())
        .forEach(value -> {
          if (builder.length() > 0) {
            builder.append(", ");
          }
          builder.append(sanitize(value));
        });
    return builder.toString();
  }

  private static void append(StringBuilder builder, String key, Object value) {
    if (value == null) {
      return;
    }
    if (builder.length() > 0) {
      builder.append(", ");
    }
    builder.append(key).append('=').append(sanitize(String.valueOf(value)));
  }

  private static String safeReason(Throwable throwable) {
    if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
      return "unspecified";
    }
    return throwable.getMessage();
  }

  private static String sanitize(String value) {
    return value == null ? ""
        : value
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace('|', '/')
            .trim();
  }
}
