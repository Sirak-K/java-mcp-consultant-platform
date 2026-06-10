package mcp.server.foundation.support.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

@Service
public final class ProjectCatalogJsonLoader {

  private static final Path CATALOG_ROOT = Path.of("catalogs");
  private static final Path ALT_CATALOG_ROOT = Path.of("..", "catalogs");
  private static final List<Path> CATALOG_ROOT_CANDIDATES = List.of(
      CATALOG_ROOT,
      ALT_CATALOG_ROOT);

  private final ObjectMapper objectMapper;

  public ProjectCatalogJsonLoader(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public JsonNode loadCatalogObject(Path relativeCatalogPath) {
    requireRelativeJsonCatalogPath(relativeCatalogPath);
    Path catalogPath = resolveCatalogPath(relativeCatalogPath);
    String catalogJson = readUtf8WithoutBom(catalogPath, relativeCatalogPath);
    JsonNode root = readJson(catalogJson, relativeCatalogPath);
    if (!root.isObject()) {
      throw new IllegalStateException("Catalog JSON root must be an object: " + displayPath(relativeCatalogPath));
    }
    return root;
  }

  public List<Path> listCatalogJsonPaths() {
    Path catalogRoot = resolveCatalogRoot();
    try (Stream<Path> paths = Files.walk(catalogRoot)) {
      return paths
          .filter(Files::isRegularFile)
          .map(catalogRoot::relativize)
          .map(Path::normalize)
          .filter(path -> displayPath(path).endsWith(".json"))
          .sorted(Comparator.comparing(ProjectCatalogJsonLoader::displayPath))
          .toList();
    } catch (IOException exception) {
      throw new IllegalStateException("Catalog root could not be scanned.", exception);
    }
  }

  private Path resolveCatalogPath(Path relativeCatalogPath) {
    for (Path rootCandidate : CATALOG_ROOT_CANDIDATES) {
      Path candidate = rootCandidate.resolve(relativeCatalogPath);
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("Catalog file not found: " + displayPath(relativeCatalogPath));
  }

  private Path resolveCatalogRoot() {
    for (Path rootCandidate : CATALOG_ROOT_CANDIDATES) {
      if (Files.isDirectory(rootCandidate)) {
        return rootCandidate;
      }
    }
    throw new IllegalStateException("Catalog root not found.");
  }

  private String readUtf8WithoutBom(Path catalogPath, Path relativeCatalogPath) {
    try {
      byte[] bytes = Files.readAllBytes(catalogPath);
      if (bytes.length >= 3
          && bytes[0] == (byte) 0xEF
          && bytes[1] == (byte) 0xBB
          && bytes[2] == (byte) 0xBF) {
        throw new IllegalStateException("Catalog file must be UTF-8 without BOM: " + displayPath(relativeCatalogPath));
      }
      return StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException exception) {
      throw new IllegalStateException("Catalog file contains invalid UTF-8: " + displayPath(relativeCatalogPath),
          exception);
    } catch (IOException exception) {
      throw new IllegalStateException("Catalog file could not be read: " + displayPath(relativeCatalogPath),
          exception);
    }
  }

  private JsonNode readJson(String catalogJson, Path relativeCatalogPath) {
    try {
      return objectMapper.readTree(catalogJson);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Catalog file contains invalid JSON: " + displayPath(relativeCatalogPath),
          exception);
    }
  }

  private static void requireRelativeJsonCatalogPath(Path relativeCatalogPath) {
    Objects.requireNonNull(relativeCatalogPath, "relativeCatalogPath");
    if (relativeCatalogPath.isAbsolute()) {
      throw new IllegalArgumentException("Catalog path must be relative.");
    }

    Path normalized = relativeCatalogPath.normalize();
    if (!relativeCatalogPath.equals(normalized)) {
      throw new IllegalArgumentException("Catalog path must be normalized: " + displayPath(relativeCatalogPath));
    }

    for (Path part : relativeCatalogPath) {
      String value = part.toString();
      if (value.isBlank() || ".".equals(value) || "..".equals(value)) {
        throw new IllegalArgumentException(
            "Catalog path contains an invalid segment: " + displayPath(relativeCatalogPath));
      }
    }

    if (!displayPath(relativeCatalogPath).endsWith(".json")) {
      throw new IllegalArgumentException("Catalog path must point to a JSON file: " + displayPath(relativeCatalogPath));
    }
  }

  private static String displayPath(Path path) {
    return path.toString().replace('\\', '/');
  }
}
