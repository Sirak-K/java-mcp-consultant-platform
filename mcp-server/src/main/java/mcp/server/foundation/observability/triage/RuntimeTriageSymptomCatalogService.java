package mcp.server.foundation.observability.triage;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.array;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;

@Service
public final class RuntimeTriageSymptomCatalogService {

  private static final Path RUNTIME_TRIAGE_SYMPTOM_CATALOG_PATH = Path.of(
      "observability",
      "runtime_triage",
      "runtime_triage_symptom_catalog.json");
  private static final String CATALOG_LABEL = "Runtime triage symptom catalog";
  private static final String EXPECTED_CATALOG_ID = "runtime_triage_symptom_catalog";
  private static final Pattern TEMPLATE_TOKEN_PATTERN = Pattern.compile("\\{([A-Za-z0-9_]+)}");

  private final Map<String, SymptomDefinition> symptomsByKey;

  public RuntimeTriageSymptomCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    JsonNode root = catalogJsonLoader.loadCatalogObject(RUNTIME_TRIAGE_SYMPTOM_CATALOG_PATH);
    requireCatalogId(root, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(root, CATALOG_LABEL, "runtime_triage_catalog_version");
    this.symptomsByKey = loadSymptoms(root);
  }

  public TriageSymptomView symptom(
      String symptomKey,
      boolean active,
      List<TriageSignalView> signals) {

    SymptomDefinition symptom = symptomDefinition(symptomKey);
    return new TriageSymptomView(
        symptom.symptomId(),
        active,
        symptom.operatorHint(),
        signals);
  }

  public TriageSignalView signal(
      String symptomKey,
      String signalKey,
      boolean observed,
      Map<String, String> detailValues) {

    SignalDefinition signal = signalDefinition(symptomKey, signalKey);
    return new TriageSignalView(
        signal.source(),
        signal.signalId(),
        observed,
        renderTemplate(signal.detailTemplate(), detailValues));
  }

  private SignalDefinition signalDefinition(String symptomKey, String signalKey) {
    SymptomDefinition symptom = symptomDefinition(symptomKey);
    SignalDefinition signal = symptom.signalsByKey().get(signalKey);
    if (signal == null) {
      throw new IllegalStateException("Runtime triage signal catalog entry is missing: "
          + symptomKey + "." + signalKey);
    }
    return signal;
  }

  private SymptomDefinition symptomDefinition(String symptomKey) {
    SymptomDefinition symptom = symptomsByKey.get(symptomKey);
    if (symptom == null) {
      throw new IllegalStateException("Runtime triage symptom catalog entry is missing: " + symptomKey);
    }
    return symptom;
  }

  private static Map<String, SymptomDefinition> loadSymptoms(JsonNode root) {
    LinkedHashMap<String, SymptomDefinition> symptoms = new LinkedHashMap<>();
    for (JsonNode symptomNode : array(root, CATALOG_LABEL, "runtime_triage_symptoms")) {
      String symptomKey = requiredText(symptomNode, CATALOG_LABEL, "symptom_key");
      SymptomDefinition previous = symptoms.putIfAbsent(
          symptomKey,
          new SymptomDefinition(
              symptomKey,
              requiredText(symptomNode, CATALOG_LABEL, "symptom_id"),
              requiredText(symptomNode, CATALOG_LABEL, "operator_hint"),
              loadSignals(symptomNode, symptomKey)));
      if (previous != null) {
        throw new IllegalStateException("Duplicate runtime triage symptom key: " + symptomKey);
      }
    }
    return java.util.Collections.unmodifiableMap(symptoms);
  }

  private static Map<String, SignalDefinition> loadSignals(JsonNode symptomNode, String symptomKey) {
    LinkedHashMap<String, SignalDefinition> signals = new LinkedHashMap<>();
    Set<String> signalIds = new LinkedHashSet<>();
    for (JsonNode signalNode : array(symptomNode, CATALOG_LABEL, "signals")) {
      String signalKey = requiredText(signalNode, CATALOG_LABEL, "signal_key");
      String signalId = requiredText(signalNode, CATALOG_LABEL, "signal_id");
      SignalDefinition previous = signals.putIfAbsent(
          signalKey,
          new SignalDefinition(
              signalKey,
              requiredText(signalNode, CATALOG_LABEL, "signal_source"),
              signalId,
              requiredText(signalNode, CATALOG_LABEL, "detail_template")));
      if (previous != null) {
        throw new IllegalStateException("Duplicate runtime triage signal key: " + symptomKey + "." + signalKey);
      }
      if (!signalIds.add(signalId)) {
        throw new IllegalStateException("Duplicate runtime triage signal id: " + symptomKey + "." + signalId);
      }
    }
    return java.util.Collections.unmodifiableMap(signals);
  }

  private static String renderTemplate(String template, Map<String, String> detailValues) {
    Map<String, String> safeValues = detailValues == null ? Map.of() : detailValues;
    Matcher matcher = TEMPLATE_TOKEN_PATTERN.matcher(template);
    StringBuilder rendered = new StringBuilder();
    while (matcher.find()) {
      String token = matcher.group(1);
      String value = safeValues.get(token);
      if (value == null) {
        throw new IllegalStateException("Runtime triage detail template token is missing: " + token);
      }
      matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(rendered);
    return rendered.toString();
  }

  private record SymptomDefinition(
      String symptomKey,
      String symptomId,
      String operatorHint,
      Map<String, SignalDefinition> signalsByKey) {
  }

  private record SignalDefinition(
      String signalKey,
      String source,
      String signalId,
      String detailTemplate) {
  }
}
