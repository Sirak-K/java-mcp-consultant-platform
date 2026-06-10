package mcp.server.domain.candidate_profiles.web;

import mcp.server.domain.candidate_profiles.application.intake.CandidateApplicationIntakeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@RestController
public final class CandidateApplicationController {

  private final CandidateApplicationIntakeService candidateApplicationIntakeService;

  public CandidateApplicationController(CandidateApplicationIntakeService candidateApplicationIntakeService) {
    this.candidateApplicationIntakeService = Objects.requireNonNull(
        candidateApplicationIntakeService,
        "candidateApplicationIntakeService");
  }

  @PostMapping(value = "/api/public/candidate-applications", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CandidateApplicationWebContract.CandidateApplicationView createCandidateApplication(
      @RequestBody CandidateApplicationWebContract.CandidateApplicationInput request) {
    return candidateApplicationIntakeService.createCandidateApplication(request);
  }

  @PostMapping(value = "/api/public/candidate-applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CandidateApplicationWebContract.CandidateApplicationView createCandidateApplicationFromCvFile(
      @RequestParam("contactEmail") String contactEmail,
      @RequestParam("cvFile") MultipartFile cvFile,
      @RequestPart(value = "certificateFiles", required = false) List<MultipartFile> certificateFiles,
      @RequestPart(value = "profileWorkingCopy", required = false)
      CandidateCvWebContract.CandidateCvProfileWorkingCopyInput profileWorkingCopy,
      @RequestPart(value = "generatedSummary", required = false)
      CandidateApplicationWebContract.CandidateProfileSummaryInput generatedSummary) {
    return candidateApplicationIntakeService.createCandidateApplicationFromCvFile(
        contactEmail,
        cvFile,
        certificateFiles,
        profileWorkingCopy,
        generatedSummary);
  }

  @PostMapping(value = "/api/public/candidate-cv-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CandidateCvWebContract.CandidateCvPreviewView previewCandidateCv(
      @RequestParam("cvFile") MultipartFile cvFile) {
    return candidateApplicationIntakeService.previewCandidateCv(cvFile);
  }

  @GetMapping("/api/ops/candidate-applications")
  public List<CandidateApplicationWebContract.CandidateApplicationView> candidateApplicationsForReview() {
    return candidateApplicationIntakeService.candidateApplicationsArchive();
  }
}
