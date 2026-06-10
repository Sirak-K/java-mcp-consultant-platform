package mcp.server.domain.matching.api;

import java.util.List;

public final class MatchViewerReadModel {

  private MatchViewerReadModel() {
  }

  public record MatchViewerView(
      String generatedAt,
      List<MatchViewerMissionView> missions) {

    public MatchViewerView {
      missions = missions == null ? List.of() : List.copyOf(missions);
    }
  }

  public record MatchViewerMissionView(
      long missionId,
      String missionTitle,
      String customerEmail,
      String customerName,
      String workMode,
      String status,
      String startDate,
      String endDate,
      List<MatchViewerMissionSlotView> slots) {

    public MatchViewerMissionView {
      slots = slots == null ? List.of() : List.copyOf(slots);
    }
  }

  public record MatchViewerMissionSlotView(
      long missionSlotId,
      int missionSlotNumber,
      String roleTitle,
      int requiredRoleExperienceYears,
      List<MatchViewerRequiredSkillView> requiredSkills,
      List<MatchViewerCandidateMatchView> matches) {

    public MatchViewerMissionSlotView {
      requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
      matches = matches == null ? List.of() : List.copyOf(matches);
    }
  }

  public record MatchViewerRequiredSkillView(
      long skillId,
      String skillTitle,
      short skillLevelId,
      String skillLevelName,
      String skillCategory) {
  }

  public record MatchViewerCandidateMatchView(
      long matchId,
      int score,
      String scoreLabel,
      boolean roleMatched,
      boolean workModeMatched,
      int matchedSkillCount,
      int requiredSkillCount,
      List<String> matchedSkills,
      String matchedAt,
      MatchViewerCandidateCardView candidateCard) {

    public MatchViewerCandidateMatchView {
      matchedSkills = matchedSkills == null ? List.of() : List.copyOf(matchedSkills);
    }
  }

  public record MatchViewerCandidateCardView(
      long candidateProfileId,
      String candidateName,
      String roleTitle,
      String roleExperienceLevel,
      Integer roleExperienceYears,
      String availabilityStatus,
      String country,
      String locationFlexibility,
      String workMode,
      List<MatchViewerCandidateSkillView> primarySkills,
      List<MatchViewerCandidateSkillView> secondarySkills) {

    public MatchViewerCandidateCardView {
      primarySkills = primarySkills == null ? List.of() : List.copyOf(primarySkills);
      secondarySkills = secondarySkills == null ? List.of() : List.copyOf(secondarySkills);
    }
  }

  public record MatchViewerCandidateSkillView(
      String title,
      String skillLevel) {
  }
}
