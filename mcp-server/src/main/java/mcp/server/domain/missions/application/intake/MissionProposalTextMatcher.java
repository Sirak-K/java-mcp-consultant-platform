package mcp.server.domain.missions.application.intake;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class MissionProposalTextMatcher {
  List<String> normalizedLines(String sourceText) {
    return sourceText.lines()
        .map(this::cleanLine)
        .filter(line -> !line.isBlank())
        .filter(line -> line.length() <= 240)
        .toList();
  }

  String cleanLine(String line) {
    return safeText(line)
        .replace('\u00A0', ' ')
        .replaceAll("\\s+", " ")
        .replaceFirst("^[\\s*#:_-]+", "")
        .trim();
  }

  boolean containsTerm(String text, String term) {
    String safeTerm = safeText(term).trim();
    if (safeTerm.isBlank()) {
      return false;
    }
    if (safeTerm.length() <= 2 && safeTerm.matches("[A-Za-z]{1,2}")) {
      return false;
    }
    Pattern pattern = Pattern.compile(
        "(?iu)(^|[^\\p{L}\\p{N}])" + Pattern.quote(safeTerm) + "($|[^\\p{L}\\p{N}])");
    return pattern.matcher(safeText(text)).find();
  }

  String termBoundaryRegex(String term) {
    String safeTerm = safeText(term).trim();
    if (safeTerm.isBlank()) {
      return "(?!)";
    }
    return "(?:^|[^\\p{L}\\p{N}])" + Pattern.quote(safeTerm) + "(?:$|[^\\p{L}\\p{N}])";
  }

  String firstContainedTerm(String sourceText, String... terms) {
    for (String term : terms) {
      if (containsTerm(sourceText, term)) {
        return term;
      }
    }
    return "";
  }
}
