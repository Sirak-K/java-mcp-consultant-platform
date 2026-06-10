package mcp.server.domain.matching.application.discovery;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.matching.api.CandidateToSlotMatchCleanup;
import mcp.server.domain.matching.api.CandidateToSlotMatchesRecorded;
import mcp.server.domain.matching.model.CandidateToMissionSlotMatchScorer;
import mcp.server.domain.matching.persistence.CandidateToSlotMatchJpaRepository;

@Service
public class CandidateToSlotMatchReconciliationService implements CandidateToSlotMatchCleanup {

  private final CandidateToSlotMatchJpaRepository matchRepo;
  private final ApplicationEventPublisher eventPublisher;

  public CandidateToSlotMatchReconciliationService(
      CandidateToSlotMatchJpaRepository matchRepo,
      ApplicationEventPublisher eventPublisher) {
    this.matchRepo = matchRepo;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public Optional<Long> reconcilePair(
      Long candidateProfileId,
      Long missionSlotId,
      CandidateToMissionSlotMatchScorer.Result score,
      Instant discoveryCreatedAt) {

    if (candidateProfileId == null
        || missionSlotId == null
        || score == null
        || discoveryCreatedAt == null) {
      return Optional.empty();
    }

    if (!score.qualifiedMatch()) {
      matchRepo.deleteByCandidateProfileIdAndMissionSlotId(
          candidateProfileId,
          missionSlotId);
      return Optional.empty();
    }

    matchRepo.upsertQualifiedMatch(
        candidateProfileId,
        missionSlotId,
        score.score(),
        score.scoreLabel(),
        score.roleMatched(),
        score.workModeMatched(),
        score.matchedSkillCount(),
        matchedSkillIds(score),
        matchedSkillTitles(score),
        discoveryCreatedAt);

    Optional<Long> matchId = matchRepo
        .findByCandidateProfileIdAndMissionSlotId(candidateProfileId, missionSlotId)
        .map(match -> match.getId());
    matchId.ifPresent(id -> eventPublisher.publishEvent(new CandidateToSlotMatchesRecorded(List.of(id))));
    return matchId;
  }

  @Override
  @Transactional
  public int removeMatchesForCandidateProfile(long candidateProfileId) {
    if (candidateProfileId <= 0) {
      return 0;
    }
    return matchRepo.deleteByCandidateProfileId(candidateProfileId);
  }

  @Override
  @Transactional
  public void removeMatchesForMissionSlots(Collection<Long> missionSlotIds) {
    if (missionSlotIds == null || missionSlotIds.isEmpty()) {
      return;
    }
    missionSlotIds.stream()
        .filter(id -> id != null)
        .forEach(matchRepo::deleteByMissionSlotId);
  }

  private String matchedSkillIds(CandidateToMissionSlotMatchScorer.Result score) {
    return score.matchedSkillIds().stream()
        .map(String::valueOf)
        .collect(Collectors.joining(","));
  }

  private String matchedSkillTitles(CandidateToMissionSlotMatchScorer.Result score) {
    return score.matchedSkillTitles().stream()
        .filter(title -> title != null && !title.isBlank())
        .collect(Collectors.joining(" | "));
  }
}
