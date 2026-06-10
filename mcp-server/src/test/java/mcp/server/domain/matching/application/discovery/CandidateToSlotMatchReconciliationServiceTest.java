package mcp.server.domain.matching.application.discovery;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import mcp.server.domain.matching.api.CandidateToSlotMatchesRecorded;
import mcp.server.domain.matching.model.CandidateToMissionSlotMatchScorer;
import mcp.server.domain.matching.persistence.CandidateToSlotMatchEntity;
import mcp.server.domain.matching.persistence.CandidateToSlotMatchJpaRepository;

class CandidateToSlotMatchReconciliationServiceTest {

  @Test
  void qualifiedScoreUpsertsPersistedMatchSnapshot() {
    CandidateToSlotMatchJpaRepository matchRepo = mock(CandidateToSlotMatchJpaRepository.class);
    ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    CandidateToSlotMatchReconciliationService service =
        new CandidateToSlotMatchReconciliationService(matchRepo, eventPublisher);
    Instant matchedAt = Instant.parse("2026-01-01T10:15:30Z");
    when(matchRepo.findByCandidateProfileIdAndMissionSlotId(10L, 555L)).thenReturn(Optional.of(new CandidateToSlotMatchEntity(
        9001L,
        10L,
        555L,
        75,
        "Qualified",
        true,
        true,
        2,
        "42,43",
        "Java | Spring",
        matchedAt)));

    service.reconcilePair(
        10L,
        555L,
        new CandidateToMissionSlotMatchScorer.Result(
            75,
            "Qualified",
            true,
            true,
            2,
            3,
            List.of(42L, 43L),
            List.of("Java", "Spring")),
        matchedAt);

    verify(matchRepo).upsertQualifiedMatch(
        eq(10L),
        eq(555L),
        eq(75),
        eq("Qualified"),
        eq(true),
        eq(true),
        eq(2),
        eq("42,43"),
        eq("Java | Spring"),
        eq(matchedAt));
    verify(eventPublisher).publishEvent(new CandidateToSlotMatchesRecorded(List.of(9001L)));
  }

  @Test
  void nonQualifiedScoreDeletesPersistedMatchPair() {
    CandidateToSlotMatchJpaRepository matchRepo = mock(CandidateToSlotMatchJpaRepository.class);
    ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    CandidateToSlotMatchReconciliationService service =
        new CandidateToSlotMatchReconciliationService(matchRepo, eventPublisher);

    service.reconcilePair(
        10L,
        555L,
        new CandidateToMissionSlotMatchScorer.Result(
            50,
            "Insufficient",
            true,
            false,
            0,
            3,
            List.of(),
            List.of()),
        Instant.parse("2026-01-01T10:15:30Z"));

    verify(matchRepo).deleteByCandidateProfileIdAndMissionSlotId(
        eq(10L),
        eq(555L));
    verifyNoInteractions(eventPublisher);
  }
}
