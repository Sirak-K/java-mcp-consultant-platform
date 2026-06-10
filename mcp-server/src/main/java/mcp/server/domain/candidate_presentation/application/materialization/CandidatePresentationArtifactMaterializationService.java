package mcp.server.domain.candidate_presentation.application.materialization;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import mcp.server.domain.candidate_presentation.application.artifacts.CandidatePresentationTitleFormat;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationCatalogService;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationCatalogService.CandidatePresentationSectionSpec;
import mcp.server.domain.candidate_presentation.exception.CandidatePresentationException;
import mcp.server.domain.candidate_presentation.persistence.CandidatePresentationArtifactEntity;
import mcp.server.domain.missions.application.MissionQueryService;

@Service
public class CandidatePresentationArtifactMaterializationService {

  private static final Path ARTIFACT_ROOT = projectRoot().resolve(Path.of(
      "runtime_artifacts",
      "candidate_presentation",
      "artifacts"));
  private static final String CUSTOMER_MARKDOWN_FILE_NAME = "version-for-customer.md";
  private static final String OPS_REVIEW_JSON_FILE_NAME = "version-for-ops-review.json";

  private final ObjectMapper objectMapper;
  private final ObjectWriter prettyJsonWriter;
  private final CandidatePresentationGenerationCatalogService catalogService;
  private final MissionQueryService missionQueryService;

  public CandidatePresentationArtifactMaterializationService(
      ObjectMapper objectMapper,
      CandidatePresentationGenerationCatalogService catalogService,
      MissionQueryService missionQueryService) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.prettyJsonWriter = objectMapper.writerWithDefaultPrettyPrinter();
    this.catalogService = Objects.requireNonNull(catalogService, "catalogService");
    this.missionQueryService = Objects.requireNonNull(missionQueryService, "missionQueryService");
  }

  public MaterializedCandidatePresentationArtifact materialize(CandidatePresentationArtifactEntity artifact) {
    Objects.requireNonNull(artifact, "artifact");
    Long artifactId = Objects.requireNonNull(artifact.getId(), "artifact.id");

    Path artifactDir = artifactDirectory(artifactId);
    Path customerMarkdownPath = artifactDir.resolve(CUSTOMER_MARKDOWN_FILE_NAME);
    Path opsReviewJsonPath = artifactDir.resolve(OPS_REVIEW_JSON_FILE_NAME);

    JsonNode customerFacingContent = parseJson(
        artifact.getCustomerFacingContentJson(),
        "customerFacingContentJson");
    String customerReadyMarkdown = customerReadyMarkdown(customerFacingContent, artifact);
    String opsReviewJson = opsReviewJson(artifact, customerReadyMarkdown);

    try {
      Files.createDirectories(artifactDir);
      writeUtf8Replacing(customerMarkdownPath, customerReadyMarkdown);
      writeUtf8Replacing(opsReviewJsonPath, opsReviewJson);
    } catch (IOException exception) {
      throw new UncheckedIOException(
          "Failed to materialize candidate presentation artifact " + artifactId,
          exception);
    }

    return new MaterializedCandidatePresentationArtifact(
        artifactId,
        customerMarkdownPath,
        opsReviewJsonPath);
  }

  public Path artifactDirectory(long artifactId) {
    return ARTIFACT_ROOT.resolve(Long.toString(artifactId));
  }

  public void deleteMaterializedArtifact(long artifactId) {
    Path artifactDir = artifactDirectory(artifactId).toAbsolutePath().normalize();
    Path artifactRoot = ARTIFACT_ROOT.toAbsolutePath().normalize();
    if (!artifactDir.startsWith(artifactRoot)) {
      throw new IllegalStateException("Refusing to delete artifact path outside candidate presentation artifact root");
    }
    if (!Files.exists(artifactDir)) {
      return;
    }
    try (var paths = Files.walk(artifactDir)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.deleteIfExists(path);
            } catch (IOException exception) {
              throw new UncheckedIOException(exception);
            }
          });
    } catch (IOException | UncheckedIOException exception) {
      throw new IllegalStateException(
          "Failed to delete materialized candidate presentation artifact " + artifactId,
          exception);
    }
  }

  private String customerReadyMarkdown(
      JsonNode customerFacingContent,
      CandidatePresentationArtifactEntity artifact) {
    if (customerFacingContent == null || !customerFacingContent.isObject()) {
      throw invalidSavedJson("customerFacingContentJson must be a JSON object");
    }

    StringBuilder markdown = new StringBuilder();
    List<CandidatePresentationSectionSpec> sectionSpecs = catalogService.customerFacingSectionSpecs().stream()
        .sorted(Comparator.comparingInt(CandidatePresentationSectionSpec::order))
        .toList();
    int missionSlotNumber = missionSlotNumber(artifact.getMissionSlotId());
    for (int index = 0; index < sectionSpecs.size(); index++) {
      CandidatePresentationSectionSpec sectionSpec = sectionSpecs.get(index);
      markdown.append(markdownHeading(sectionSpec.markdownLevel()))
          .append(" ")
          .append(sectionTitle(sectionSpec, missionSlotNumber))
          .append(System.lineSeparator());
      markdown.append(System.lineSeparator());
      markdown.append(markdownValue(customerFacingContent.get(sectionSpec.key()))).append(System.lineSeparator());
      if (index < sectionSpecs.size() - 1) {
        markdown.append(System.lineSeparator());
      }
    }
    return markdown.toString();
  }

  private String sectionTitle(CandidatePresentationSectionSpec sectionSpec, int missionSlotNumber) {
    String titleTemplate = sectionSpec.titleTemplate();
    if (titleTemplate != null && !titleTemplate.isBlank()) {
      return titleTemplate.replace("{missionSlotNumber}", Integer.toString(missionSlotNumber));
    }
    return sectionSpec.title();
  }

  private int missionSlotNumber(long missionSlotId) {
    return missionQueryService.missionSlotNumber(missionSlotId);
  }

  private static String markdownHeading(int markdownLevel) {
    int normalizedLevel = Math.min(Math.max(markdownLevel, 1), 6);
    return "#".repeat(normalizedLevel);
  }

  private String markdownValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return "";
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isArray()) {
      StringBuilder markdown = new StringBuilder();
      for (JsonNode item : node) {
        markdown.append("- ").append(markdownValue(item)).append(System.lineSeparator());
      }
      return markdown.toString().stripTrailing();
    }
    if (node.isObject()) {
      StringBuilder markdown = new StringBuilder();
      Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        markdown.append("- ")
            .append(field.getKey())
            .append(": ")
            .append(markdownValue(field.getValue()))
            .append(System.lineSeparator());
      }
      return markdown.toString().stripTrailing();
    }
    return node.asText();
  }

  private String opsReviewJson(
      CandidatePresentationArtifactEntity artifact,
      String customerReadyMarkdown) {
    LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
    payload.put("metadata", metadata(artifact));
    payload.put("customerReadyMarkdown", customerReadyMarkdown);
    payload.put("opsReviewContent", parseJson(
        artifact.getOpsReviewContentJson(),
        "opsReviewContentJson"));
    payload.put("evidenceTrace", parseJson(
        artifact.getEvidenceTraceJson(),
        "evidenceTraceJson"));

    try {
      return prettyJsonWriter.writeValueAsString(payload) + System.lineSeparator();
    } catch (JsonProcessingException exception) {
      throw CandidatePresentationException.internalError("Could not serialize candidate presentation OPS review artifact", exception);
    }
  }

  private Map<String, Object> metadata(CandidatePresentationArtifactEntity artifact) {
    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("artifactId", artifact.getId());
    metadata.put("sourceCandidateToSlotMatchId", artifact.getSourceCandidateToSlotMatchId());
    metadata.put("candProfileId", artifact.getCandProfileId());
    metadata.put("missionId", artifact.getMissionId());
    metadata.put("missionSlotId", artifact.getMissionSlotId());
    metadata.put("artifactStatus", artifact.getArtifactStatus());
    metadata.put(
        "presentationTitle",
        CandidatePresentationTitleFormat.forMatchId(artifact.getSourceCandidateToSlotMatchId()));
    metadata.put("createdAt", artifact.getCreatedAt());
    metadata.put("updatedAt", artifact.getUpdatedAt());
    return metadata;
  }

  private JsonNode parseJson(String rawJson, String fieldName) {
    try {
      return objectMapper.readTree(rawJson);
    } catch (JsonProcessingException exception) {
      throw CandidatePresentationException.internalError("Saved " + fieldName + " contains invalid JSON", exception);
    }
  }

  private CandidatePresentationException invalidSavedJson(String reason) {
    return CandidatePresentationException.internalError(reason);
  }

  private void writeUtf8Replacing(Path target, String content) throws IOException {
    Path tempFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
    try {
      Files.writeString(tempFile, content, StandardCharsets.UTF_8);
      try {
        Files.move(
            tempFile,
            target,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  public record MaterializedCandidatePresentationArtifact(
      long artifactId,
      Path customerMarkdownPath,
      Path opsReviewJsonPath) {
  }

  private static Path projectRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    if (cwd.getFileName() != null && "mcp-server".equals(cwd.getFileName().toString())) {
      return cwd.getParent();
    }
    return cwd;
  }
}
