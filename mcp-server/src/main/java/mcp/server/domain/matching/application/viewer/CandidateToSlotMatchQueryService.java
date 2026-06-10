package mcp.server.domain.matching.application.viewer;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.matching.api.CandidateMissionMatchEvidenceGroup;
import mcp.server.domain.matching.api.CandidateToSlotMatchEvidence;
import mcp.server.domain.matching.api.CandidateToSlotMatchQuery;
import mcp.server.domain.matching.api.MatchScoreBreakdownView;
import mcp.server.domain.matching.model.CandidateToMissionSlotMatchScorer;
import mcp.server.domain.matching.persistence.CandidateToSlotMatchEntity;
import mcp.server.domain.matching.persistence.CandidateToSlotMatchJpaRepository;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.missions.application.RegisteredMissionQuery;

@Service
public class CandidateToSlotMatchQueryService implements CandidateToSlotMatchQuery {

  private final CandidateToSlotMatchJpaRepository matchRepo;
  private final MissionQueryService missionQueryService;
  private final MatchScoreBreakdownQueryService scoreBreakdownQueryService;

  public CandidateToSlotMatchQueryService(
      CandidateToSlotMatchJpaRepository matchRepo,
      MissionQueryService missionQueryService,
      MatchScoreBreakdownQueryService scoreBreakdownQueryService) {
    this.matchRepo = matchRepo;
    this.missionQueryService = missionQueryService;
    this.scoreBreakdownQueryService = scoreBreakdownQueryService;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CandidateToSlotMatchEvidence> findMatchEvidence(long matchId) {
    return matchRepo.findById(matchId)
        .map(CandidateToSlotMatchQueryService::toMatchEvidence);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CandidateMissionMatchEvidenceGroup> findCandidateMissionMatchEvidenceGroup(long matchId) {
    return matchRepo.findById(matchId)
        .map(this::candidateMissionMatchEvidenceGroup);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CandidateMissionMatchEvidenceGroup> findRecentCandidateMissionMatchEvidenceGroups() {
    LinkedHashMap<String, CandidateMissionMatchEvidenceGroup> groupsByKey = new LinkedHashMap<>();
    matchRepo.findAllByOrderByCreatedAtDesc().stream()
        .map(this::candidateMissionMatchEvidenceGroup)
        .forEach(group -> groupsByKey.putIfAbsent(group.deliveryGroupKey(), group));
    return List.copyOf(groupsByKey.values());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Long> findMatchId(long candidateProfileId, long missionSlotId) {
    return matchRepo.findByCandidateProfileIdAndMissionSlotId(candidateProfileId, missionSlotId)
        .map(CandidateToSlotMatchEntity::getId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Long> findMatchIdsForCandidateAndMissionSlots(
      long candidateProfileId,
      Collection<Long> missionSlotIds) {
    if (candidateProfileId <= 0 || missionSlotIds == null || missionSlotIds.isEmpty()) {
      return List.of();
    }
    return missionSlotIds.stream()
        .filter(Objects::nonNull)
        .map(missionSlotId -> findMatchId(candidateProfileId, missionSlotId))
        .flatMap(Optional::stream)
        .distinct()
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public MatchScoreBreakdownView inspectScoreBreakdown(long matchId) {
    return scoreBreakdownQueryService.inspectScoreBreakdown(matchId);
  }

  private CandidateMissionMatchEvidenceGroup candidateMissionMatchEvidenceGroup(CandidateToSlotMatchEntity match) {
    RegisteredMissionQuery.MissionReadView mission = missionQueryService.requireMissionForSlot(
        match.getMissionSlotId());
    Set<Long> missionSlotIds = mission.slots().stream()
        .map(RegisteredMissionQuery.MissionSlotReadView::missionSlotId)
        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    CandidateToSlotMatchEvidence primaryMatch = toMatchEvidence(match);
    List<CandidateToSlotMatchEvidence> groupedMatches = matchRepo
        .findAllByCandidateProfileIdAndCreatedAtOrderByMissionSlotIdAsc(
            match.getCandidateProfileId(),
            match.getCreatedAt())
        .stream()
        .filter(candidateMatch -> missionSlotIds.contains(candidateMatch.getMissionSlotId()))
        .map(CandidateToSlotMatchQueryService::toMatchEvidence)
        .toList();
    return new CandidateMissionMatchEvidenceGroup(
        primaryMatch,
        mission.missionId(),
        deliveryGroupKey(primaryMatch.candidateProfileId(), mission.missionId(), primaryMatch.matchedAt()),
        primaryMatch.matchedAt(),
        groupedMatches);
  }

  private static CandidateToSlotMatchEvidence toMatchEvidence(CandidateToSlotMatchEntity match) {
    return new CandidateToSlotMatchEvidence(
        safeLong(match.getId()),
        safeLong(match.getCandidateProfileId()),
        safeLong(match.getMissionSlotId()),
        safeInt(match.getMatchScore()),
        scoreLabel(match),
        Boolean.TRUE.equals(match.getRoleMatched()),
        Boolean.TRUE.equals(match.getWorkModeMatched()),
        safeInt(match.getMatchedSkillCount()),
        matchedSkillIds(match),
        matchedSkills(match),
        match.getCreatedAt());
  }

  private static List<String> matchedSkills(CandidateToSlotMatchEntity match) {
    String titles = match.getMatchedSkillTitles();
    if (titles == null || titles.isBlank()) {
      return List.of();
    }
    return Arrays.stream(titles.split("\\|"))
        .map(String::trim)
        .filter(title -> !title.isBlank())
        .toList();
  }

  private static List<Long> matchedSkillIds(CandidateToSlotMatchEntity match) {
    String ids = match.getMatchedSkillIds();
    if (ids == null || ids.isBlank()) {
      return List.of();
    }
    return Arrays.stream(ids.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .map(CandidateToSlotMatchQueryService::parseLongOrNull)
        .filter(Objects::nonNull)
        .toList();
  }

  private static Long parseLongOrNull(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException excep) {
      return null;
    }
  }

  private static int safeInt(Integer value) {
    return value == null ? 0 : value;
  }

  private static long safeLong(Long value) {
    return value == null ? 0L : value;
  }

  private static String scoreLabel(CandidateToSlotMatchEntity match) {
    return CandidateToMissionSlotMatchScorer.scoreLabel(safeInt(match.getMatchScore()));
  }

  private static String deliveryGroupKey(long candidateProfileId, long missionId, Instant createdAt) {
    return "candProfile:" + candidateProfileId
        + "|mission:" + missionId
        + "|createdAt:" + (createdAt == null ? "unknown" : createdAt.toString());
  }
}
