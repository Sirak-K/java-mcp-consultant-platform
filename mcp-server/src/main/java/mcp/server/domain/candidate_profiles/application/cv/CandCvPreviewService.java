package mcp.server.domain.candidate_profiles.application.cv;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Service
public class CandCvPreviewService {

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

  private final CandCvTextExtractionService cvTextExtractionService;
  private final CandCvProfileWorkingCopyService cvProfileWorkingCopyService;

  public CandCvPreviewService(
      CandCvTextExtractionService cvTextExtractionService,
      CandCvProfileWorkingCopyService cvProfileWorkingCopyService) {
    this.cvTextExtractionService = Objects.requireNonNull(cvTextExtractionService, "cvTextExtractionService");
    this.cvProfileWorkingCopyService = Objects.requireNonNull(cvProfileWorkingCopyService,
        "cvProfileWorkingCopyService");
  }

  public CandCvTextExtractionService.ExtractionResult metadataOnlyExtraction() {
    return new CandCvTextExtractionService.ExtractionResult(
        CandCvTextExtractionService.METADATA_ONLY,
        "",
        "",
        null);
  }

  public CandCvTextExtractionService.ExtractionResult extract(MultipartFile cvFile) {
    return cvTextExtractionService.extract(cvFile);
  }

  public CandidateCvWebContract.CandidateCvPreviewView previewCandidateCv(MultipartFile cvFile) {
    if (cvFile == null || cvFile.isEmpty()) {
      throw reject("cvFile is required");
    }
    CandCvTextExtractionService.ExtractionResult extraction = extract(cvFile);
    return new CandidateCvWebContract.CandidateCvPreviewView(
        toExtractionView(extraction),
        cvProfileWorkingCopyService.toWorkingCopy(extraction));
  }

  public CandidateCvWebContract.CandidateCvExtractionView toExtractionView(
      CandCvTextExtractionService.ExtractionResult extraction) {

    return toExtractionView(
        extraction.status(),
        extraction.extractedText(),
        extraction.error(),
        extraction.extractedAt());
  }

  public CandidateCvWebContract.CandidateCvExtractionView toExtractionView(
      String status,
      String extractedText,
      String error,
      Instant extractedAt) {

    return new CandidateCvWebContract.CandidateCvExtractionView(
        status,
        previewText(extractedText),
        safeText(error),
        formatInstant(extractedAt));
  }

  private String previewText(String extractedText) {
    String safeExtractedText = safeText(extractedText);
    return safeExtractedText.length() <= 800
        ? safeExtractedText
        : safeExtractedText.substring(0, 800);
  }

  private String formatInstant(Instant instant) {
    return instant == null
        ? null
        : TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
  }
}
