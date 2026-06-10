package mcp.server.domain.candidate_profiles.web;

import mcp.server.domain.matching.api.MissionMatchDiscoveryResult;

import java.util.List;

public final class CandidateApplicationWebContract {

  private CandidateApplicationWebContract() {
  }

  public record CandidateApplicationInput(
      String contactEmail,
      String cvFileName,
      String cvContentType,
      Long cvSizeBytes,
      CandidateCvWebContract.CandidateCvProfileWorkingCopyInput profileWorkingCopy,
      CandidateProfileSummaryInput generatedSummary) {
  }

  public record CandidateProfileSpecificationView(
      String contactEmail,
      String cvFileName,
      String cvContentType,
      Long cvSizeBytes,
      boolean cvExtractionPending,
      CandidateCvWebContract.CandidateCvProfileWorkingCopyView profileWorkingCopy) {
  }

  public record CandidateProfileSummaryView(
      String status,
      String coreCompetenceOverview,
      String location,
      String otherDetails,
      String generatedAt) {
  }

  public record CandidateProfileSummaryInput(
      String coreCompetenceOverview,
      String location,
      String otherDetails) {
  }

  public record CandidateApplicationView(
      long id,
      CandidateProfileSpecificationView specification,
      CandidateCvWebContract.CandidateCvExtractionView cvExtraction,
      CandidateProfileSummaryView generatedSummary,
      List<MissionMatchDiscoveryResult> findMissionResults,
      String outcome,
      String createdAt,
      String updatedAt) {
  }
}
