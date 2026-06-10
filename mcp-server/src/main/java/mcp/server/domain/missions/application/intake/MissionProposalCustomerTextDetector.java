package mcp.server.domain.missions.application.intake;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
final class MissionProposalCustomerTextDetector {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b");
  private static final Pattern LABELED_COMPANY_PATTERN = Pattern
      .compile("(?imu)^\\s*(?:f\\u00F6retag|foretag|bolag|kund|company|customer)\\s*[:\\-]\\s*(.+)$");
  private static final Pattern SWEDISH_COMPANY_CONTEXT_PATTERN = Pattern.compile(
      "(?iu)\\b(?:vi\\s+p\\u00E5|vi\\s+pa|hos|p\\u00E5|pa|f\\u00F6r|for)\\s+([\\p{L}\\p{N}&.'\\- ]{2,90}\\s+(?:AB|Aktiebolag))\\b");
  private static final Pattern COMPANY_SUFFIX_PATTERN = Pattern.compile(
      "(?iu)^([\\p{L}\\p{N}&.'\\- ]{2,90}\\s+(?:AB|Aktiebolag|Ltd|LLC|Inc|GmbH|Oy|AS))\\b");

  private final MissionProposalTextMatcher textMatcher;
  private final MissionProposalTextEvidenceRecorder evidenceRecorder;

  MissionProposalCustomerTextDetector(
      MissionProposalTextMatcher textMatcher,
      MissionProposalTextEvidenceRecorder evidenceRecorder) {
    this.textMatcher = textMatcher;
    this.evidenceRecorder = evidenceRecorder;
  }

  String detectCustomerName(
      String sourceText,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    Matcher labeledMatcher = LABELED_COMPANY_PATTERN.matcher(sourceText);
    while (labeledMatcher.find()) {
      String customerName = cleanCustomerName(labeledMatcher.group(1));
      if (isUsefulCustomerName(customerName)) {
        evidenceRecorder.add(evidence, "customerName", customerName, labeledMatcher.group());
        return customerName;
      }
    }

    Matcher contextMatcher = SWEDISH_COMPANY_CONTEXT_PATTERN.matcher(sourceText);
    if (contextMatcher.find()) {
      String customerName = cleanCustomerName(contextMatcher.group(1));
      if (isUsefulCustomerName(customerName)) {
        evidenceRecorder.add(evidence, "customerName", customerName, contextMatcher.group());
        return customerName;
      }
    }

    for (String line : textMatcher.normalizedLines(sourceText)) {
      Matcher suffixMatcher = COMPANY_SUFFIX_PATTERN.matcher(line);
      if (suffixMatcher.find()) {
        String customerName = cleanCustomerName(suffixMatcher.group(1));
        if (isUsefulCustomerName(customerName)) {
          evidenceRecorder.add(evidence, "customerName", customerName, line);
          return customerName;
        }
      }
    }

    return "";
  }

  String detectEmail(
      String sourceText,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    Matcher matcher = EMAIL_PATTERN.matcher(sourceText);
    if (!matcher.find()) {
      return "";
    }
    String email = matcher.group().toLowerCase(Locale.ROOT);
    evidenceRecorder.add(evidence, "customerEmail", email, email);
    return email;
  }

  private String cleanCustomerName(String value) {
    return textMatcher.cleanLine(value)
        .replaceFirst("(?iu)\\s+(?:beh\\u00F6ver|behover|s\\u00F6ker|soker|needs?|looking|vill|ska|skall)\\b.*$", "")
        .replaceFirst("(?iu)\\s+(?:kontakt|contact)\\b.*$", "")
        .replaceAll("[,.;:]+$", "")
        .trim();
  }

  private boolean isUsefulCustomerName(String value) {
    String cleaned = cleanCustomerName(value);
    if (cleaned.length() < 4 || cleaned.length() > 100) {
      return false;
    }
    if (EMAIL_PATTERN.matcher(cleaned).find()) {
      return false;
    }
    return COMPANY_SUFFIX_PATTERN.matcher(cleaned).find();
  }
}
