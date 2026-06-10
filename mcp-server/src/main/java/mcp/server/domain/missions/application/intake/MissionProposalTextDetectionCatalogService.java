package mcp.server.domain.missions.application.intake;

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
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredInt;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.textList;

@Service
public final class MissionProposalTextDetectionCatalogService {

  private static final Path MISSION_PROPOSAL_CATALOG_PATH = Path.of(
      "missions",
      "text_detection",
      "mission_proposal_text_detection_catalog.json");
  private static final String CATALOG_LABEL = "Mission proposal text detection catalog";
  private static final String EXPECTED_CATALOG_ID = "mission_proposal_text_detection_catalog";

  private final JsonNode missionProposalCatalog;
  private final Map<String, Integer> missionMonthNumbers;

  public MissionProposalTextDetectionCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    this.missionProposalCatalog = catalogJsonLoader.loadCatalogObject(MISSION_PROPOSAL_CATALOG_PATH);
    requireCatalogId(missionProposalCatalog, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(missionProposalCatalog, CATALOG_LABEL, "mission_proposal_text_detection_catalog_version");
    this.missionMonthNumbers = monthNumbers();
  }

  public List<String> missionMonthTerms() {
    return List.copyOf(missionMonthNumbers.keySet());
  }

  public int missionMonthNumber(String value) {
    return missionMonthNumbers.getOrDefault(value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT), 0);
  }

  public List<CountWordSignal> missionSlotCountWords() {
    return array(missionProposalCatalog, CATALOG_LABEL, "slot_count_words").stream()
        .map(signal -> new CountWordSignal(
            requiredText(signal, CATALOG_LABEL, "word"),
            requiredInt(signal, CATALOG_LABEL, "count")))
        .toList();
  }

  public List<String> missionSlotResourceTerms() {
    return textList(missionProposalCatalog, CATALOG_LABEL, "slot_resource_terms");
  }

  public List<WorkModeSignal> missionWorkModeSignals() {
    return array(missionProposalCatalog, CATALOG_LABEL, "mission_work_mode_signals").stream()
        .map(signal -> new WorkModeSignal(
            requiredText(signal, CATALOG_LABEL, "work_mode"),
            textList(signal, CATALOG_LABEL, "terms")))
        .toList();
  }

  public List<ExperienceLevelSignal> missionExperienceLevelSignals() {
    return array(missionProposalCatalog, CATALOG_LABEL, "experience_level_signals").stream()
        .map(signal -> new ExperienceLevelSignal(
            requiredText(signal, CATALOG_LABEL, "level_key"),
            textList(signal, CATALOG_LABEL, "terms"),
            requiredInt(signal, CATALOG_LABEL, "inferred_years"),
            textList(signal, CATALOG_LABEL, "skill_level_names")))
        .toList();
  }

  public List<RoleSignal> missionRoleSignals() {
    return array(missionProposalCatalog, CATALOG_LABEL, "role_signals").stream()
        .map(signal -> new RoleSignal(
            requiredText(signal, CATALOG_LABEL, "role_title"),
            textList(signal, CATALOG_LABEL, "terms")))
        .toList();
  }

  public List<String> missionFallbackSkillLevelNames() {
    return textList(missionProposalCatalog, CATALOG_LABEL, "fallback_skill_level_names");
  }

  private Map<String, Integer> monthNumbers() {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    for (JsonNode monthGroup : array(missionProposalCatalog, CATALOG_LABEL, "month_aliases")) {
      int monthNumber = requiredInt(monthGroup, CATALOG_LABEL, "month_number");
      for (String term : textList(monthGroup, CATALOG_LABEL, "terms")) {
        String key = term.toLowerCase(java.util.Locale.ROOT);
        Integer previous = result.putIfAbsent(key, monthNumber);
        if (previous != null && previous != monthNumber) {
          throw new IllegalStateException("Duplicate month alias with conflicting month number: " + term);
        }
      }
    }
    return Map.copyOf(result);
  }

  public record CountWordSignal(String word, int count) {
  }

  public record WorkModeSignal(String workMode, List<String> terms) {
  }

  public record ExperienceLevelSignal(
      String levelKey,
      List<String> terms,
      int inferredYears,
      List<String> skillLevelNames) {
  }

  public record RoleSignal(String roleTitle, List<String> terms) {
  }
}
