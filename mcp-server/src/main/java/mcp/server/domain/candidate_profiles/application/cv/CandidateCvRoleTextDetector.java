package mcp.server.domain.candidate_profiles.application.cv;

import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import mcp.server.domain.reference_data.persistence.RoleEntity;
import mcp.server.domain.reference_data.persistence.RoleJpaRepo;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
final class CandidateCvRoleTextDetector {

  private static final int MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS = 100;
  private static final Pattern ROLE_FROM_PROFILE_PATTERN = Pattern
      .compile("(?iu)studerar\\s+till\\s+([^\\n]+?)\\s+p\\u00E5\\s");

  private final RoleJpaRepo roleRepo;
  private final CandidateProfileTextDetectionCatalogService textDetectionCatalogService;
  private final CandidateCvTextMatcher textMatcher;

  CandidateCvRoleTextDetector(
      RoleJpaRepo roleRepo,
      CandidateProfileTextDetectionCatalogService textDetectionCatalogService,
      CandidateCvTextMatcher textMatcher) {
    this.roleRepo = roleRepo;
    this.textDetectionCatalogService = textDetectionCatalogService;
    this.textMatcher = textMatcher;
  }

  DetectedRole detectRole(String extractedText, List<String> profileLines) {
    String profileText = textMatcher.joinLines(profileLines);
    List<RoleEntity> roles = roleRepo.findAll().stream()
        .filter(role -> role.getId() != null)
        .filter(role -> role.getRoleTitle() != null)
        .distinct()
        .sorted(Comparator.comparingInt((RoleEntity role) -> role.getRoleTitle().length()).reversed())
        .toList();
    for (RoleEntity role : roles) {
      String roleTitle = role.getRoleTitle();
      if (containsRoleTerm(profileText, roleTitle) || containsRoleTerm(extractedText, roleTitle)) {
        return new DetectedRole(role.getId(), roleTitle);
      }
    }
    Matcher matcher = ROLE_FROM_PROFILE_PATTERN.matcher(extractedText);
    if (matcher.find()) {
      return new DetectedRole(0, textMatcher.cleanLine(matcher.group(1)));
    }
    return DetectedRole.empty();
  }

  int parseRoleExperienceYears(String yearsOfExperience) {
    String trimmedYears = mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText(
        yearsOfExperience).trim();
    if (!trimmedYears.matches("\\d+")) {
      return 0;
    }
    return Math.min(Integer.parseInt(trimmedYears), MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS);
  }

  private boolean containsRoleTerm(String text, String roleTitle) {
    if (textMatcher.containsTerm(text, roleTitle)) {
      return true;
    }
    return textDetectionCatalogService.candidateRoleTitleAliases()
        .getOrDefault(mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText(roleTitle).trim(),
            List.of())
        .stream()
        .anyMatch(alias -> textMatcher.containsCatalogAlias(text, alias));
  }

  record DetectedRole(long roleId, String roleTitle) {
    List<CandidateCvWebContract.CandidateRoleWorkingCopyView> toCandidateRoles(int roleExperienceYears) {
      if (roleId <= 0 || roleTitle == null || roleTitle.isBlank()) {
        return List.of();
      }
      return List.of(new CandidateCvWebContract.CandidateRoleWorkingCopyView(roleId, roleTitle, roleExperienceYears));
    }

    static DetectedRole empty() {
      return new DetectedRole(0, "");
    }
  }
}
