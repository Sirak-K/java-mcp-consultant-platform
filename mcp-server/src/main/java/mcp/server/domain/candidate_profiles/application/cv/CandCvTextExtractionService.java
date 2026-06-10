package mcp.server.domain.candidate_profiles.application.cv;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Service
public final class CandCvTextExtractionService {

  public static final String METADATA_ONLY = "METADATA_ONLY";
  static final String EXTRACTED = "EXTRACTED";
  static final String EMPTY_TEXT = "EMPTY_TEXT";
  static final String UNSUPPORTED_CONTENT_TYPE = "UNSUPPORTED_CONTENT_TYPE";
  static final String FAILED = "FAILED";

  private static final int MAX_STORED_TEXT_CHARS = 200_000;

  private final Clock clock;

  public CandCvTextExtractionService() {
    this(Clock.systemDefaultZone());
  }

  CandCvTextExtractionService(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public ExtractionResult extract(MultipartFile file) {
    Objects.requireNonNull(file, "file");
    String contentType = normalize(file.getContentType());
    String fileName = normalize(file.getOriginalFilename());

    try {
      if (isPdf(contentType, fileName)) {
        return extractPdf(file);
      }
      if (isPlainText(contentType, fileName)) {
        return extractPlainText(file);
      }
      return new ExtractionResult(
          UNSUPPORTED_CONTENT_TYPE,
          "",
          "Unsupported CV format. PDF and plain text are supported now; DOC/DOCX require separate parser support.",
          null);
    } catch (IOException exception) {
      return new ExtractionResult(FAILED, "", "Could not read uploaded CV file.", null);
    }
  }

  private ExtractionResult extractPdf(MultipartFile file) throws IOException {
    try (PDDocument document = PDDocument.load(file.getInputStream())) {
      String extractedText = normalizeText(new PDFTextStripper().getText(document));
      if (extractedText.isBlank()) {
        return new ExtractionResult(
            EMPTY_TEXT,
            "",
            "Uploaded PDF did not contain readable text. The PDF may be image-based and require OCR.",
            now());
      }
      return new ExtractionResult(EXTRACTED, truncate(extractedText), "", now());
    }
  }

  private ExtractionResult extractPlainText(MultipartFile file) throws IOException {
    String extractedText = normalizeText(new String(file.getBytes(), StandardCharsets.UTF_8));
    if (extractedText.isBlank()) {
      return new ExtractionResult(EMPTY_TEXT, "", "Uploaded text file did not contain readable text.", now());
    }
    return new ExtractionResult(EXTRACTED, truncate(extractedText), "", now());
  }

  private boolean isPdf(String contentType, String fileName) {
    return "application/pdf".equals(contentType) || fileName.endsWith(".pdf");
  }

  private boolean isPlainText(String contentType, String fileName) {
    return contentType.startsWith("text/") || fileName.endsWith(".txt") || fileName.endsWith(".md");
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeText(String value) {
    return value
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim();
  }

  private String truncate(String value) {
    if (value.length() <= MAX_STORED_TEXT_CHARS) {
      return value;
    }
    return value.substring(0, MAX_STORED_TEXT_CHARS);
  }

  private Instant now() {
    return Instant.now(clock);
  }

  public record ExtractionResult(
      String status,
      String extractedText,
      String error,
      Instant extractedAt) {
  }
}
