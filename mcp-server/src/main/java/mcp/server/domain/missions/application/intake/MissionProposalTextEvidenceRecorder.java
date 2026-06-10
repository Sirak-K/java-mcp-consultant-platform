package mcp.server.domain.missions.application.intake;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class MissionProposalTextEvidenceRecorder {
  void add(
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence,
      String field,
      String value,
      String sourceText) {

    evidence.add(new MissionProposalIntake.WorkingCopyEvidenceView(
        field,
        safeText(value).trim(),
        limitLength(safeText(sourceText).replaceAll("\\s+", " ").trim(), 180),
        null));
  }

  String excerptAround(String sourceText, String term) {
    String safeText = safeText(sourceText).replaceAll("\\s+", " ").trim();
    String safeTerm = safeText(term).trim();
    if (safeText.isBlank() || safeTerm.isBlank()) {
      return "";
    }
    int index = safeText.toLowerCase(Locale.ROOT).indexOf(safeTerm.toLowerCase(Locale.ROOT));
    if (index < 0) {
      return limitLength(safeTerm, 120);
    }
    int start = Math.max(0, index - 45);
    int end = Math.min(safeText.length(), index + safeTerm.length() + 45);
    return limitLength(safeText.substring(start, end).trim(), 160);
  }

  String limitLength(String value, int maxLength) {
    String safeValue = safeText(value).trim();
    return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength).trim();
  }
}
