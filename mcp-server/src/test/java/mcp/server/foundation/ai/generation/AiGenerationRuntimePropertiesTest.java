package mcp.server.foundation.ai.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiGenerationRuntimePropertiesTest {

    @Test
    void defaultsUseCandidatePresentationGenerationRuntimePath() {
        AiGenerationRuntimeProperties properties = new AiGenerationRuntimeProperties();

        assertThat(properties.getMcpEndpointUrl()).isEqualTo("http://127.0.0.1:8080/mcp");
        assertThat(properties.getMcpTransportKind()).isEqualTo("streamable_http");
        assertThat(properties.getMcpTimeoutSeconds()).isEqualTo(30.0d);
        assertThat(properties.getRuntime().getBaseUrl()).isEqualTo("http://127.0.0.1:8091");
        assertThat(properties.getRuntime().getGenerationRunPath())
                .isEqualTo("/candidate-presentation-generation/runs");
    }

    @Test
    void generationRunPathIsNormalizedToLeadingSlash() {
        AiGenerationRuntimeProperties properties = new AiGenerationRuntimeProperties();

        properties.getRuntime().setGenerationRunPath("candidate-presentation-generation/runs");

        assertThat(properties.getRuntime().getGenerationRunPath())
                .isEqualTo("/candidate-presentation-generation/runs");
    }

    @Test
    void requiredRuntimePropertiesRejectBlankValuesAndInvalidTimeouts() {
        AiGenerationRuntimeProperties properties = new AiGenerationRuntimeProperties();

        assertThatThrownBy(() -> properties.setMcpEndpointUrl(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mcpEndpointUrl");
        assertThatThrownBy(() -> properties.setMcpTimeoutSeconds(0.0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mcpTimeoutSeconds");
        assertThatThrownBy(() -> properties.getRuntime().setGenerationRunPath(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("generationRunPath");
        assertThatThrownBy(() -> properties.getRuntime().setConnectTimeoutMillis(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectTimeoutMillis");
        assertThatThrownBy(() -> properties.getRuntime().setRequestTimeoutMillis(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestTimeoutMillis");
    }
}
