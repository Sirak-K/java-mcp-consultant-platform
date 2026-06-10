package mcp.server.foundation.ai.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class AiGenerationRuntimeClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void startGenerationRunPostsClientNeutralMcpRuntimePayload() throws Exception {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/candidate-presentation-generation/runs", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respondJson(exchange, """
                    {
                      "status": "accepted",
                      "service": "ai_generation_runtime",
                      "accepted_generation_run": {
                        "run_id": "run-123",
                        "run_state": "accepted",
                        "match_id": "match-9001",
                        "artifact_id": "artifact-77",
                        "background_started": true
                      }
                    }
                    """);
        });
        try {
            server.start();
            AiGenerationRuntimeProperties properties = new AiGenerationRuntimeProperties();
            properties.setMcpEndpointUrl("http://127.0.0.1:8080/mcp");
            properties.setMcpTransportKind("streamable_http");
            properties.setMcpTimeoutSeconds(12.5d);
            properties.getRuntime().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.getRuntime().setGenerationRunPath("candidate-presentation-generation/runs");
            AiGenerationRuntimeClient client = new AiGenerationRuntimeClient(properties, objectMapper);

            AiGenerationRunAccepted accepted = client.startGenerationRun(new AiGenerationRunRequest(
                    "match-9001",
                    "artifact-77"));

            assertThat(requestPath.get()).isEqualTo("/candidate-presentation-generation/runs");
            JsonNode body = objectMapper.readTree(requestBody.get());
            assertThat(body.path("matchId").asText()).isEqualTo("match-9001");
            assertThat(body.path("artifactId").asText()).isEqualTo("artifact-77");
            assertThat(body.path("mcpTransportKind").asText()).isEqualTo("streamable_http");
            assertThat(body.path("mcpEndpointUrl").asText()).isEqualTo("http://127.0.0.1:8080/mcp");
            assertThat(body.path("mcpTimeoutSeconds").asDouble()).isEqualTo(12.5d);
            assertThat(accepted.runId()).isEqualTo("run-123");
            assertThat(accepted.runState()).isEqualTo("accepted");
            assertThat(accepted.generationInputId()).isEqualTo("match-9001");
            assertThat(accepted.generationArtifactId()).isEqualTo("artifact-77");
            assertThat(accepted.backgroundStarted()).isTrue();
        } finally {
            server.stop(0);
        }
    }

    private static void respondJson(HttpExchange exchange, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(202, response.length);
        try (exchange; var body = exchange.getResponseBody()) {
            body.write(response);
        }
    }
}
