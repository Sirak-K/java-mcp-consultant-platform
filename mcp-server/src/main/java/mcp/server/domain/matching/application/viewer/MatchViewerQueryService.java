package mcp.server.domain.matching.application.viewer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.candidate_profiles.api.CandidateProfileCardView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.matching.api.MatchViewerReadModel;
import mcp.server.domain.matching.model.CandidateToMissionSlotMatchScorer;
import mcp.server.domain.matching.persistence.CandidateToSlotMatchEntity;
import mcp.server.domain.matching.persistence.CandidateToSlotMatchJpaRepository;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.missions.application.MissionSpecification;
import mcp.server.domain.missions.application.RegisteredMissionQuery;

@Service
public class MatchViewerQueryService {

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

  private final CandidateToSlotMatchJpaRepository matchRepo;
  private final MissionQueryService missionQueryService;
  private final CandidateProfileQuery candidateProfileQuery;

  public MatchViewerQueryService(
      CandidateToSlotMatchJpaRepository matchRepo,
      MissionQueryService missionQueryService,
      CandidateProfileQuery candidateProfileQuery) {
    this.matchRepo = matchRepo;
    this.missionQueryService = missionQueryService;
    this.candidateProfileQuery = candidateProfileQuery;
  }

  @Transactional(readOnly = true)
  public MatchViewerReadModel.MatchViewerView matchViewer() {
    List<CandidateToSlotMatchEntity> matches = matchRepo.findAllByOrderByCreatedAtDesc();
    Map<Long, CandidateProfileCardView> candidatesById = loadCandidates(matches);
    Map<Long, RegisteredMissionQuery.MissionSlotReadView> slotsById = missionQueryService.missionSlotsById(
        matches.stream()
            .map(CandidateToSlotMatchEntity::getMissionSlotId)
            .filter(Objects::nonNull)
            .distinct()
            .toList());
    Map<Long, RegisteredMissionQuery.MissionReadView> missionsById = missionQueryService.missionsById(
        slotsById.values().stream()
            .map(RegisteredMissionQuery.MissionSlotReadView::missionId)
            .distinct()
            .toList());
    LinkedHashMap<Long, MissionBuilder> missionBuilders = new LinkedHashMap<>();

    for (CandidateToSlotMatchEntity match : matches) {
      CandidateProfileCardView candidate = candidatesById.get(match.getCandidateProfileId());
      if (candidate == null) {
        continue;
      }

      RegisteredMissionQuery.MissionSlotReadView slotRead = slotsById.get(match.getMissionSlotId());
      if (slotRead == null) {
        continue;
      }
      RegisteredMissionQuery.MissionReadView mission = missionsById.get(slotRead.missionId());
      if (!isMatchViewerEligible(mission)) {
        continue;
      }
      MissionSpecification.SlotSpecificationView slot = slotRead.specification();
      if (slot == null) {
        continue;
      }

      MissionBuilder missionBuilder = missionBuilders.computeIfAbsent(
          mission.missionId(),
          ignored -> new MissionBuilder(mission));
      missionBuilder.addMatch(slot, match, toMatchViewerCandidateCard(candidate));
    }

    return new MatchViewerReadModel.MatchViewerView(
        formatInstant(Instant.now()),
        missionBuilders.values().stream()
            .map(MissionBuilder::toView)
            .toList());
  }

  private Map<Long, CandidateProfileCardView> loadCandidates(
      List<CandidateToSlotMatchEntity> matches) {
    List<Long> candidateProfileIds = matches.stream()
        .map(CandidateToSlotMatchEntity::getCandidateProfileId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    return candidateProfileQuery.candidateProfileCardsById(candidateProfileIds);
  }

  private static boolean isMatchViewerEligible(RegisteredMissionQuery.MissionReadView mission) {
    return mission != null && "OPEN".equals(mission.missionAvailability());
  }

  private static MatchViewerReadModel.MatchViewerCandidateCardView toMatchViewerCandidateCard(
      CandidateProfileCardView card) {
    return new MatchViewerReadModel.MatchViewerCandidateCardView(
        card.candidateProfileId(),
        card.displayName(),
        card.primaryRoleTitle(),
        card.primaryRoleExperienceLevel(),
        card.primaryRoleExperienceYears(),
        card.workStatus(),
        card.country(),
        card.locationFlexibility(),
        card.workMode(),
        card.primarySkills().stream()
            .map(skill -> new MatchViewerReadModel.MatchViewerCandidateSkillView(
                skill.skillTitle(),
                skill.skillLevelName()))
            .toList(),
        card.secondarySkills().stream()
            .map(skill -> new MatchViewerReadModel.MatchViewerCandidateSkillView(
                skill.skillTitle(),
                skill.skillLevelName()))
            .toList());
  }

  private static MatchViewerReadModel.MatchViewerRequiredSkillView requiredSkill(
      MissionSpecification.SkillRequirementView skill) {
    return new MatchViewerReadModel.MatchViewerRequiredSkillView(
        skill.skillId(),
        skill.skillTitle(),
        skill.skillLevelId(),
        skill.skillLevelName(),
        skill.skillCategory());
  }

  private static String scoreLabel(CandidateToSlotMatchEntity match) {
    return CandidateToMissionSlotMatchScorer.scoreLabel(safeInt(match.getMatchScore()));
  }

  private static int safeInt(Integer value) {
    return value == null ? 0 : value;
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

  private static String formatInstant(Instant instant) {
    return instant == null
        ? null
        : TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
  }

  private static final class MissionBuilder {
    private final RegisteredMissionQuery.MissionReadView mission;
    private final LinkedHashMap<Long, SlotBuilder> slotBuilders = new LinkedHashMap<>();

    private MissionBuilder(RegisteredMissionQuery.MissionReadView mission) {
      this.mission = mission;
    }

    private void addMatch(
        MissionSpecification.SlotSpecificationView slot,
        CandidateToSlotMatchEntity match,
        MatchViewerReadModel.MatchViewerCandidateCardView candidateCard) {
      SlotBuilder slotBuilder = slotBuilders.computeIfAbsent(
          match.getMissionSlotId(),
          ignored -> new SlotBuilder(match.getMissionSlotId(), slot));
      slotBuilder.addMatch(match, candidateCard);
    }

    private MatchViewerReadModel.MatchViewerMissionView toView() {
      return new MatchViewerReadModel.MatchViewerMissionView(
          mission.missionId(),
          mission.specification().missionTitle(),
          mission.specification().customerEmail(),
          mission.specification().customerName(),
          mission.specification().workMode(),
          mission.missionAvailability(),
          mission.specification().startDate(),
          mission.specification().endDate(),
          slotBuilders.values().stream()
              .sorted(Comparator.comparingInt(slotBuilder -> slotBuilder.slot.slotNumber()))
              .map(SlotBuilder::toView)
              .toList());
    }
  }

  private static final class SlotBuilder {
    private final long missionSlotId;
    private final MissionSpecification.SlotSpecificationView slot;
    private final List<MatchViewerReadModel.MatchViewerCandidateMatchView> candidateMatches = new ArrayList<>();

    private SlotBuilder(
        long missionSlotId,
        MissionSpecification.SlotSpecificationView slot) {
      this.missionSlotId = missionSlotId;
      this.slot = slot;
    }

    private void addMatch(
        CandidateToSlotMatchEntity match,
        MatchViewerReadModel.MatchViewerCandidateCardView candidateCard) {
      candidateMatches.add(new MatchViewerReadModel.MatchViewerCandidateMatchView(
          match.getId(),
          match.getMatchScore(),
          scoreLabel(match),
          Boolean.TRUE.equals(match.getRoleMatched()),
          Boolean.TRUE.equals(match.getWorkModeMatched()),
          match.getMatchedSkillCount(),
          slot.requiredSkills().size(),
          matchedSkills(match),
          formatInstant(match.getCreatedAt()),
          candidateCard));
    }

    private MatchViewerReadModel.MatchViewerMissionSlotView toView() {
      return new MatchViewerReadModel.MatchViewerMissionSlotView(
          missionSlotId,
          slot.slotNumber(),
          slot.roleTitle(),
          slot.requiredRoleExperienceYears(),
          slot.requiredSkills().stream()
              .map(MatchViewerQueryService::requiredSkill)
              .toList(),
          candidateMatches.stream()
              .sorted(Comparator
                  .comparing(MatchViewerReadModel.MatchViewerCandidateMatchView::score, Comparator.reverseOrder())
                  .thenComparing(match -> match.candidateCard().candidateName()))
              .toList());
    }
  }
}
