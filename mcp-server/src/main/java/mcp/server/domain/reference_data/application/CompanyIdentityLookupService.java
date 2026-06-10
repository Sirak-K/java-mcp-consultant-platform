package mcp.server.domain.reference_data.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import mcp.server.domain.reference_data.persistence.CompanyIdentityLookupEntity;
import mcp.server.domain.reference_data.persistence.CompanyIdentityLookupJpaRepo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Service
public class CompanyIdentityLookupService {

  private static final int MAX_CANDIDATE_OPTIONS = 10;
  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
  private static final Pattern LEGAL_SUFFIX_PATTERN = Pattern.compile("(?iu)\\b(ab|aktiebolag)\\b");
  private static final Pattern NON_SEARCH_CHAR_PATTERN = Pattern.compile("(?iu)[^\\p{L}\\p{N}]+");

  private final CompanyIdentityLookupJpaRepo companyIdentityLookupRepo;

  public CompanyIdentityLookupService(
      CompanyIdentityLookupJpaRepo companyIdentityLookupRepo) {
    this.companyIdentityLookupRepo = Objects.requireNonNull(companyIdentityLookupRepo, "companyIdentityLookupRepo");
  }

  public CompanyIdentityResolution resolve(String rawCompanyName) {
    String normalizedName = normalizeCompanyName(rawCompanyName);
    if (normalizedName.isBlank()) {
      return CompanyIdentityResolution.empty();
    }

    Map<String, CompanyIdentityLookupEntity> candidatesByOrgNr = new LinkedHashMap<>();
    companyIdentityLookupRepo
        .findByOrganisationNameNormalizedOrderByOrganisationNameAsc(normalizedName)
        .forEach(candidate -> candidatesByOrgNr.putIfAbsent(candidate.getOrganisationNumber(), candidate));

    List<CompanyIdentityOption> options = candidatesByOrgNr.values().stream()
        .sorted(Comparator.comparing(CompanyIdentityLookupEntity::getOrganisationName))
        .limit(MAX_CANDIDATE_OPTIONS)
        .map(this::toOption)
        .toList();

    if (options.size() == 1) {
      return CompanyIdentityResolution.resolved(options.get(0));
    }
    if (!options.isEmpty()) {
      return CompanyIdentityResolution.options(options);
    }

    companyIdentityLookupRepo
        .findByOrganisationNameNormalizedPrefix(
            normalizedName + " ",
            PageRequest.of(0, MAX_CANDIDATE_OPTIONS))
        .forEach(candidate -> candidatesByOrgNr.putIfAbsent(candidate.getOrganisationNumber(), candidate));

    List<CompanyIdentityOption> prefixOptions = candidatesByOrgNr.values().stream()
        .sorted(Comparator.comparing(CompanyIdentityLookupEntity::getOrganisationName))
        .limit(MAX_CANDIDATE_OPTIONS)
        .map(this::toOption)
        .toList();

    return CompanyIdentityResolution.options(prefixOptions);
  }

  public String normalizeCompanyName(String rawCompanyName) {
    String safeName = safeText(rawCompanyName).trim();
    if (safeName.isBlank()) {
      return "";
    }
    String noDiacritics = DIACRITICS_PATTERN.matcher(
        Normalizer.normalize(safeName, Normalizer.Form.NFD)).replaceAll("");
    String noSuffix = LEGAL_SUFFIX_PATTERN.matcher(noDiacritics).replaceAll(" ");
    return NON_SEARCH_CHAR_PATTERN.matcher(noSuffix.toLowerCase(Locale.ROOT))
        .replaceAll(" ")
        .trim()
        .replaceAll("\\s{2,}", " ");
  }

  private CompanyIdentityOption toOption(
      CompanyIdentityLookupEntity entity) {
    return new CompanyIdentityOption(
        safeText(entity.getOrganisationName()).trim(),
        safeText(entity.getOrganisationNumber()).trim(),
        safeText(entity.getOrganisationCity()).trim());
  }

  public record CompanyIdentityOption(
      String organisationName,
      String organisationNumber,
      String organisationCity) {

    public CompanyIdentityOption {
      organisationName = organisationName == null ? "" : organisationName;
      organisationNumber = organisationNumber == null ? "" : organisationNumber;
      organisationCity = organisationCity == null ? "" : organisationCity;
    }
  }

  public record CompanyIdentityResolution(
      String organisationName,
      String organisationNumber,
      String organisationCity,
      List<CompanyIdentityOption> options) {

    public CompanyIdentityResolution {
      organisationName = organisationName == null ? "" : organisationName;
      organisationNumber = organisationNumber == null ? "" : organisationNumber;
      organisationCity = organisationCity == null ? "" : organisationCity;
      options = options == null ? List.of() : List.copyOf(options);
    }

    static CompanyIdentityResolution empty() {
      return new CompanyIdentityResolution("", "", "", List.of());
    }

    static CompanyIdentityResolution resolved(
        CompanyIdentityOption option) {
      if (option == null) {
        return empty();
      }
      return new CompanyIdentityResolution(
          option.organisationName(),
          option.organisationNumber(),
          option.organisationCity(),
          List.of());
    }

    static CompanyIdentityResolution options(
        List<CompanyIdentityOption> options) {
      List<CompanyIdentityOption> safeOptions = options == null ? List.of()
          : new ArrayList<>(options);
      return new CompanyIdentityResolution("", "", "", safeOptions);
    }
  }
}
