package mcp.server.domain.candidate_presentation.application.artifacts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_presentation.application.evidence.CandidatePresentationEvidenceService;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationCatalogService;
import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationContractService;
import mcp.server.domain.candidate_presentation.application.materialization.CandidatePresentationArtifactMaterializationService;
import mcp.server.domain.candidate_presentation.exception.CandidatePresentationException;
import mcp.server.domain.candidate_presentation.persistence.CandidatePresentationArtifactEntity;
import mcp.server.domain.candidate_presentation.persistence.CandidatePresentationArtifactJpaRepository;

class CandidatePresentationArtifactServiceWritebackTest {

    private final CandidatePresentationArtifactJpaRepository artifactRepo =
            mock(CandidatePresentationArtifactJpaRepository.class);
    private final CandidatePresentationEvidenceService evidenceService =
            mock(CandidatePresentationEvidenceService.class);
    private final CandidatePresentationGenerationContractService generationContractService =
            mock(CandidatePresentationGenerationContractService.class);
    private final CandidatePresentationArtifactMaterializationService materializationService =
            mock(CandidatePresentationArtifactMaterializationService.class);
    private final CandidatePresentationGenerationCatalogService catalogService =
            mock(CandidatePresentationGenerationCatalogService.class);
    private final CandidatePresentationArtifactLogService logService =
            mock(CandidatePresentationArtifactLogService.class);
    private final CandidatePresentationArtifactService artifactService =
            new CandidatePresentationArtifactService(
                    artifactRepo,
                    evidenceService,
                    generationContractService,
                    materializationService,
                    catalogService,
                    logService,
                    new ObjectMapper());

    @Test
    void recordGeneratedContentValidatesWritesPersistsAndMaterializesGeneratedArtifact() {
        CandidatePresentationArtifactEntity artifact = pendingArtifact();
        when(artifactRepo.findById(77L)).thenReturn(Optional.of(artifact));
        when(artifactRepo.save(any(CandidatePresentationArtifactEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CandidatePresentationGeneratedContentCommand command = new CandidatePresentationGeneratedContentCommand(
                77L,
                "Ignored client title",
                "{\"summary\":\"Customer-ready summary\"}",
                "{\"reviewNotes\":\"Ops review notes\"}",
                "[{\"source\":\"match evidence\"}]");

        CandidatePresentationArtifactView view = artifactService.recordGeneratedContent(command);

        assertThat(view.id()).isEqualTo(77L);
        assertThat(view.sourceCandidateToSlotMatchId()).isEqualTo(9001L);
        assertThat(view.artifactStatus()).isEqualTo(CandidatePresentationArtifactEntity.STATUS_GENERATED);
        assertThat(view.customerFacingContentJson()).isEqualTo(command.customerFacingContentJson());
        assertThat(view.opsReviewContentJson()).isEqualTo(command.opsReviewContentJson());
        assertThat(view.evidenceTraceJson()).isEqualTo(command.evidenceTraceJson());
        verify(generationContractService).validateGeneratedContentJson(
                command.customerFacingContentJson(),
                command.opsReviewContentJson(),
                command.evidenceTraceJson());
        verify(artifactRepo).save(artifact);
        verify(materializationService).materialize(artifact);
        verify(logService).logMcpWritebackRecorded(
                "recordGeneratedContent",
                artifact,
                CandidatePresentationArtifactEntity.STATUS_PENDING_GENERATION,
                "agent generated content accepted");
    }

    @Test
    void recordGeneratedContentRejectsArtifactsThatAreNotPendingGeneration() {
        CandidatePresentationArtifactEntity artifact = pendingArtifact();
        artifact.markGenerated(
                "Already generated",
                "{\"summary\":\"Existing\"}",
                "{\"reviewNotes\":\"Existing\"}",
                "[{\"source\":\"existing\"}]",
                Instant.parse("2026-01-01T10:05:00Z"));
        when(artifactRepo.findById(77L)).thenReturn(Optional.of(artifact));

        assertThatThrownBy(() -> artifactService.recordGeneratedContent(new CandidatePresentationGeneratedContentCommand(
                77L,
                "Generated",
                "{\"summary\":\"Customer-ready summary\"}",
                "{\"reviewNotes\":\"Ops review notes\"}",
                "[{\"source\":\"match evidence\"}]")))
                .isInstanceOf(CandidatePresentationException.class)
                .hasMessageContaining(CandidatePresentationArtifactEntity.STATUS_PENDING_GENERATION);
        verifyNoInteractions(generationContractService, materializationService);
    }

    private static CandidatePresentationArtifactEntity pendingArtifact() {
        CandidatePresentationArtifactEntity artifact = CandidatePresentationArtifactEntity.pendingGenerationDraft(
                9001L,
                3001L,
                4001L,
                5001L,
                "Draft presentation",
                "{\"summary\":\"Draft\"}",
                "{\"reviewNotes\":\"Draft\"}",
                "[{\"source\":\"draft\"}]",
                Instant.parse("2026-01-01T10:00:00Z"));
        setId(artifact, 77L);
        return artifact;
    }

    private static void setId(CandidatePresentationArtifactEntity artifact, Long id) {
        try {
            Field idField = CandidatePresentationArtifactEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(artifact, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
