package mcp.server.domain.candidate_profiles.application.cv;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.ArrayList;
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
public final class CandidateCvExtractionCatalogService {

  private static final Path TEXT_DETECTION_CATALOG_PATH = Path.of(
      "cv_extraction",
      "cv_extraction_text_detection.json");
  private static final Path CONTENT_BLOCKS_CATALOG_PATH = Path.of(
      "cv_extraction",
      "cv_extraction_content_blocks.json");
  private static final Path CONTENT_CONSTRAINTS_CATALOG_PATH = Path.of(
      "cv_extraction",
      "cv_extraction_content_constraints.json");
  private static final String TEXT_DETECTION_LABEL = "CV extraction text detection catalog";
  private static final String CONTENT_BLOCKS_LABEL = "CV extraction content blocks catalog";
  private static final String CONTENT_CONSTRAINTS_LABEL = "CV extraction content constraints catalog";

  private final JsonNode textDetectionCatalog;
  private final JsonNode contentBlocksCatalog;
  private final JsonNode contentConstraintsCatalog;

  public CandidateCvExtractionCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    this.textDetectionCatalog = catalogJsonLoader.loadCatalogObject(TEXT_DETECTION_CATALOG_PATH);
    this.contentBlocksCatalog = catalogJsonLoader.loadCatalogObject(CONTENT_BLOCKS_CATALOG_PATH);
    this.contentConstraintsCatalog = catalogJsonLoader.loadCatalogObject(CONTENT_CONSTRAINTS_CATALOG_PATH);
    requireCatalogId(textDetectionCatalog, TEXT_DETECTION_LABEL, "cv_extraction_text_detection");
    requireCatalogId(contentBlocksCatalog, CONTENT_BLOCKS_LABEL, "cv_extraction_content_blocks");
    requireCatalogId(contentConstraintsCatalog, CONTENT_CONSTRAINTS_LABEL, "cv_extraction_content_constraints");
    requiredText(textDetectionCatalog, TEXT_DETECTION_LABEL, "cv_extraction_text_detection_version");
    requiredText(contentBlocksCatalog, CONTENT_BLOCKS_LABEL, "cv_extraction_content_blocks_version");
    requiredText(contentConstraintsCatalog, CONTENT_CONSTRAINTS_LABEL, "cv_extraction_content_constraints_version");
  }

  public Map<String, List<String>> candidateSkillTitleAliases() {
    return aliasMap(textDetectionCatalog, "skill_title_aliases");
  }

  public Map<String, List<String>> candidateRoleTitleAliases() {
    return aliasMap(textDetectionCatalog, "role_title_aliases");
  }

  public String candidateCvSectionHeading(String sectionKey) {
    for (JsonNode section : allContentBlockDefinitions()) {
      if (requiredText(section, CONTENT_BLOCKS_LABEL, "cv_content_block_key").equals(sectionKey)) {
        return requiredText(section, CONTENT_BLOCKS_LABEL, "cv_content_heading");
      }
    }
    throw new IllegalStateException("CV extraction content block heading is missing: " + sectionKey);
  }

  public List<CandidateWorkModeSignal> candidateWorkModeSignals() {
    return array(textDetectionCatalog, TEXT_DETECTION_LABEL, "candidate_work_mode_signals").stream()
        .map(signal -> new CandidateWorkModeSignal(
            requiredText(signal, TEXT_DETECTION_LABEL, "display_value"),
            textList(signal, TEXT_DETECTION_LABEL, "text_detection_terms"),
            textList(signal, TEXT_DETECTION_LABEL, "cv_profile_section_terms")))
        .toList();
  }

  public List<String> candidateLocationHeadingTerms() {
    return textList(textDetectionCatalog, TEXT_DETECTION_LABEL, "location_related_terms");
  }

  public List<String> candidateSwedishCityNames() {
    return textList(textDetectionCatalog, TEXT_DETECTION_LABEL, "swedish_city_names");
  }

  public List<CountryTermSignal> candidateCountryTermSignals() {
    return array(textDetectionCatalog, TEXT_DETECTION_LABEL, "country_terms").stream()
        .map(signal -> new CountryTermSignal(
            requiredText(signal, TEXT_DETECTION_LABEL, "country"),
            textList(signal, TEXT_DETECTION_LABEL, "text_detection_terms")))
        .toList();
  }

  public List<String> candidateSwedishLanguageClues() {
    return textList(textDetectionCatalog, TEXT_DETECTION_LABEL, "swedish_language_clues");
  }

  public List<String> candidateDoNotInferFields() {
    return textList(contentConstraintsCatalog, CONTENT_CONSTRAINTS_LABEL, "do_not_infer");
  }

  private static Map<String, List<String>> aliasMap(JsonNode root, String fieldName) {
    LinkedHashMap<String, List<String>> aliases = new LinkedHashMap<>();
    for (JsonNode entry : array(root, TEXT_DETECTION_LABEL, fieldName)) {
      String title = requiredText(entry, TEXT_DETECTION_LABEL, "canonical_title");
      List<String> values = textList(entry, TEXT_DETECTION_LABEL, "canonical_title_aliases");
      if (aliases.putIfAbsent(title, values) != null) {
        throw new IllegalStateException("Duplicate alias title in " + fieldName + ": " + title);
      }
    }
    return Map.copyOf(aliases);
  }

  private List<JsonNode> allContentBlockDefinitions() {
    ArrayList<JsonNode> definitions = new ArrayList<>();
    array(contentBlocksCatalog, CONTENT_BLOCKS_LABEL, "content_blocks_to_extract").forEach(definitions::add);
    array(contentBlocksCatalog, CONTENT_BLOCKS_LABEL, "ignored_content_blocks").forEach(definitions::add);
    return List.copyOf(definitions);
  }

  public record CandidateWorkModeSignal(
      String displayValue,
      List<String> terms,
      List<String> profileSummaryTerms) {
  }

  public record CountryTermSignal(String country, List<String> terms) {
  }
}
