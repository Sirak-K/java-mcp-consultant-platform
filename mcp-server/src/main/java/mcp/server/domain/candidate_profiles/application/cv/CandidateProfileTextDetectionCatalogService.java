package mcp.server.domain.candidate_profiles.application.cv;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.textList;

@Service
public final class CandidateProfileTextDetectionCatalogService {

  private static final Path CANDIDATE_PROFILE_CATALOG_PATH = Path.of(
      "candidate_profiles",
      "text_detection",
      "candidate_profile_text_detection_catalog.json");
  private static final String CATALOG_LABEL = "Candidate profile text detection catalog";
  private static final String EXPECTED_CATALOG_ID = "candidate_profile_text_detection_catalog";

  private final JsonNode candidateProfileCatalog;

  public CandidateProfileTextDetectionCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    this.candidateProfileCatalog = catalogJsonLoader.loadCatalogObject(CANDIDATE_PROFILE_CATALOG_PATH);
    requireCatalogId(candidateProfileCatalog, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(candidateProfileCatalog, CATALOG_LABEL, "candidate_profile_text_detection_catalog_version");
  }

  public Map<String, List<String>> candidateSkillTitleAliases() {
    return aliasMap(candidateProfileCatalog, "skill_title_aliases");
  }

  public Map<String, List<String>> candidateRoleTitleAliases() {
    return aliasMap(candidateProfileCatalog, "role_title_aliases");
  }

  public String candidateCvSectionHeading(String sectionKey) {
    for (JsonNode section : array(candidateProfileCatalog, CATALOG_LABEL, "cv_section_headings")) {
      if (requiredText(section, CATALOG_LABEL, "section_key").equals(sectionKey)) {
        return requiredText(section, CATALOG_LABEL, "heading");
      }
    }
    throw new IllegalStateException("Candidate profile CV section heading is missing: " + sectionKey);
  }

  public List<CandidateWorkModeSignal> candidateWorkModeSignals() {
    return array(candidateProfileCatalog, CATALOG_LABEL, "candidate_work_mode_signals").stream()
        .map(signal -> new CandidateWorkModeSignal(
            requiredText(signal, CATALOG_LABEL, "display_value"),
            textList(signal, CATALOG_LABEL, "terms"),
            textList(signal, CATALOG_LABEL, "profile_summary_terms")))
        .toList();
  }

  public List<String> candidateLocationHeadingTerms() {
    return textList(candidateProfileCatalog, CATALOG_LABEL, "location_heading_terms");
  }

  public List<String> candidateSwedishCityNames() {
    return textList(candidateProfileCatalog, CATALOG_LABEL, "swedish_city_names");
  }

  public List<CountryTermSignal> candidateCountryTermSignals() {
    return array(candidateProfileCatalog, CATALOG_LABEL, "country_terms").stream()
        .map(signal -> new CountryTermSignal(
            requiredText(signal, CATALOG_LABEL, "country"),
            textList(signal, CATALOG_LABEL, "terms")))
        .toList();
  }

  public List<String> candidateSwedishLanguageClues() {
    return textList(candidateProfileCatalog, CATALOG_LABEL, "swedish_language_clues");
  }

  private static Map<String, List<String>> aliasMap(JsonNode root, String fieldName) {
    LinkedHashMap<String, List<String>> aliases = new LinkedHashMap<>();
    for (JsonNode entry : array(root, CATALOG_LABEL, fieldName)) {
      String title = requiredText(entry, CATALOG_LABEL, "canonical_title");
      List<String> values = textList(entry, CATALOG_LABEL, "aliases");
      if (aliases.putIfAbsent(title, values) != null) {
        throw new IllegalStateException("Duplicate alias title in " + fieldName + ": " + title);
      }
    }
    return Map.copyOf(aliases);
  }

  public record CandidateWorkModeSignal(
      String displayValue,
      List<String> terms,
      List<String> profileSummaryTerms) {
  }

  public record CountryTermSignal(String country, List<String> terms) {
  }
}
