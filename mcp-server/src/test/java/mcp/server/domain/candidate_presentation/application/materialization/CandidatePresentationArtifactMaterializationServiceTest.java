package mcp.server.domain.candidate_presentation.application.materialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationCatalogService;
import mcp.server.domain.missions.application.MissionQueryService;

class CandidatePresentationArtifactMaterializationServiceTest {

    @Test
    void artifactDirectoryUsesCandidatePresentationRuntimeArtifactRoot() {
        CandidatePresentationArtifactMaterializationService service =
                new CandidatePresentationArtifactMaterializationService(
                        new ObjectMapper(),
                        mock(CandidatePresentationGenerationCatalogService.class),
                        mock(MissionQueryService.class));

        Path artifactDirectory = service.artifactDirectory(123L).normalize();
        String normalizedPath = artifactDirectory.toString().replace('\\', '/');

        assertThat(normalizedPath)
                .endsWith("runtime_artifacts/candidate_presentation/artifacts/123")
                .doesNotContain("vf4");
    }
}
