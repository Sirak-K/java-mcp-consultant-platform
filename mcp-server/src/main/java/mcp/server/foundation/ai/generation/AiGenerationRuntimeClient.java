package mcp.server.foundation.ai.generation;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public final class AiGenerationRuntimeClient {

  private final AiGenerationRuntimeProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public AiGenerationRuntimeClient(
      AiGenerationRuntimeProperties properties,
      ObjectMapper objectMapper) {

    this.properties = Objects.requireNonNull(properties, "properties");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    AiGenerationRuntimeProperties.Runtime runtime = properties.getRuntime();
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(runtime.getConnectTimeoutMillis()))
        .version(HttpClient.Version.HTTP_1_1)
        .build();
  }

  public AiGenerationRunAccepted startGenerationRun(AiGenerationRunRequest input) {
    Objects.requireNonNull(input, "input");
    String body = json(httpRequest(input));
    HttpRequest request = generationRunRequest(body);
    HttpResponse<String> response = send(request);
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new AiGenerationRuntimeException(failedResponseMessage("generation run", response));
    }
    GenerationRunStartHttpResponse output = fromJson(response.body(), GenerationRunStartHttpResponse.class);
    if (output.acceptedGenerationRun() == null) {
      throw new AiGenerationRuntimeException(
          "Generation runtime response did not include nested accepted_generation_run payload.");
    }
    return output.acceptedGenerationRun().toAccepted();
  }

  private GenerationRunStartHttpRequest httpRequest(AiGenerationRunRequest input) {
    return new GenerationRunStartHttpRequest(
        input.generationInputId(),
        input.generationArtifactId(),
        properties.getMcpTransportKind(),
        properties.getMcpEndpointUrl(),
        properties.getMcpTimeoutSeconds());
  }

  private HttpRequest generationRunRequest(String body) {
    AiGenerationRuntimeProperties.Runtime runtime = properties.getRuntime();
    return HttpRequest.newBuilder(uri(runtime))
        .version(HttpClient.Version.HTTP_1_1)
        .timeout(requestTimeout(runtime))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        .build();
  }

  private HttpResponse<String> send(HttpRequest request) {
    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new AiGenerationRuntimeException("Generation runtime request I/O failure: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AiGenerationRuntimeException("Generation runtime request interrupted.", e);
    }
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException e) {
      throw new AiGenerationRuntimeException("Generation runtime request JSON serialization failed.", e);
    }
  }

  private <T> T fromJson(String json, Class<T> targetType) {
    try {
      return objectMapper.readValue(json, targetType);
    } catch (IOException e) {
      throw new AiGenerationRuntimeException("Generation runtime response JSON parsing failed.", e);
    }
  }

  private URI uri(AiGenerationRuntimeProperties.Runtime runtime) {
    String baseUrl = runtime.getBaseUrl();
    String path = runtime.getGenerationRunPath();
    String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    String normalizedPath = path.startsWith("/") ? path : "/" + path;
    return URI.create(normalizedBase + normalizedPath);
  }

  private static Duration requestTimeout(AiGenerationRuntimeProperties.Runtime runtime) {
    return Duration.ofMillis(runtime.getRequestTimeoutMillis());
  }

  private static String responsePreview(String body) {
    if (body == null || body.isBlank()) {
      return "empty response body";
    }
    String compact = body.replace('\r', ' ').replace('\n', ' ').trim();
    return compact.length() <= 240 ? compact : compact.substring(0, 240) + "...";
  }

  private static String failedResponseMessage(String operation, HttpResponse<String> response) {
    int statusCode = response.statusCode();
    String body = response.body();
    if (statusCode == 422 && missingRequestBodyResponse(body)) {
      return "Generation runtime "
          + operation
          + " rejected the request because the JSON request body was missing. "
          + "Restart the local dev processes and retry with the current MCP Java build.";
    }
    return "Generation runtime "
        + operation
        + " failed with HTTP "
        + statusCode
        + ": "
        + responsePreview(body);
  }

  private static boolean missingRequestBodyResponse(String body) {
    if (body == null) {
      return false;
    }
    return body.contains("\"loc\":[\"body\"]") && body.contains("\"Field required\"");
  }

  private record GenerationRunStartHttpRequest(
      String matchId,
      String artifactId,
      String mcpTransportKind,
      String mcpEndpointUrl,
      double mcpTimeoutSeconds) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record GenerationRunStartHttpResponse(
      @JsonProperty("status") String status,
      @JsonProperty("service") String service,
      @JsonProperty("accepted_generation_run") AcceptedGenerationRunHttpResponse acceptedGenerationRun) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record AcceptedGenerationRunHttpResponse(
      @JsonProperty("run_id") String runId,
      @JsonProperty("run_state") String runState,
      @JsonProperty("match_id") String matchId,
      @JsonProperty("artifact_id") String artifactId,
      @JsonProperty("background_started") Boolean backgroundStarted) {

    AiGenerationRunAccepted toAccepted() {
      return new AiGenerationRunAccepted(
          runId,
          runState,
          matchId,
          artifactId,
          backgroundStarted);
    }
  }
}
