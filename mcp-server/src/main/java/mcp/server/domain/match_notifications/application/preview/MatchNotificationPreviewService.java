package mcp.server.domain.match_notifications.application.preview;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.candidate_profiles.api.CandidateProfileCardView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.candidate_profiles.api.CandidateProfileRoleView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileSkillView;
import mcp.server.domain.match_notifications.application.delivery.MatchNotificationDeliveryService;
import mcp.server.domain.match_notifications.exception.MatchNotificationNotFoundException;
import mcp.server.domain.match_notifications.model.MatchNotificationSendSource;
import mcp.server.domain.match_notifications.web.MatchNotificationWebContract;
import mcp.server.domain.matching.api.CandidateMissionMatchEvidenceGroup;
import mcp.server.domain.matching.api.CandidateToSlotMatchEvidence;
import mcp.server.domain.matching.api.CandidateToSlotMatchQuery;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.missions.application.RegisteredMissionQuery;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MatchNotificationPreviewService {

  private record MatchPreviewCtx(
      CandidateToSlotMatchEvidence match,
      RegisteredMissionQuery.MissionReadView mission,
      RegisteredMissionQuery.MissionSlotReadView slot,
      String roleTitle,
      String matchGrade,
      String matchedSkills) {
  }

  private record MatchPreviewGroup(
      CandidateProfileCardView candidateProfile,
      RegisteredMissionQuery.MissionReadView mission,
      List<MatchPreviewCtx> contexts,
      String deliveryGroupKey) {
  }

  private static final int MAX_RATIONALE_WORDS = 100;
  private static final Pattern TEMPLATE_TOKEN_PATTERN = Pattern.compile("\\{([A-Za-z0-9_]+)}");
  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

  private final CandidateToSlotMatchQuery matchQuery;
  private final CandidateProfileQuery candidateProfileQuery;
  private final MissionQueryService missionQueryService;
  private final MatchNotificationDeliveryService deliveryService;
  private final MatchNotificationEmailContentBuilder emailContentBuilder;
  private final MatchNotificationEmailTemplateCatalogService mailTemplateCatalog;

  public MatchNotificationPreviewService(
      CandidateToSlotMatchQuery matchQuery,
      CandidateProfileQuery candidateProfileQuery,
      MissionQueryService missionQueryService,
      MatchNotificationDeliveryService deliveryService,
      MatchNotificationEmailContentBuilder emailContentBuilder,
      MatchNotificationEmailTemplateCatalogService mailTemplateCatalog) {
    this.matchQuery = Objects.requireNonNull(matchQuery, "matchQuery");
    this.candidateProfileQuery = Objects.requireNonNull(candidateProfileQuery, "candidateProfileQuery");
    this.missionQueryService = Objects.requireNonNull(missionQueryService, "missionQueryService");
    this.deliveryService = Objects.requireNonNull(deliveryService, "deliveryService");
    this.emailContentBuilder = Objects.requireNonNull(emailContentBuilder, "emailContentBuilder");
    this.mailTemplateCatalog = Objects.requireNonNull(mailTemplateCatalog, "mailTemplateCatalog");
  }

  @Transactional(readOnly = true)
  public List<MatchNotificationWebContract.MatchNotificationPreviewView> previewMatches() {
    return matchQuery.findRecentCandidateMissionMatchEvidenceGroups().stream()
        .map(this::buildPreviewGroup)
        .filter(group -> isOpenMission(group.mission()))
        .map(this::previewGroup)
        .toList();
  }

  @Transactional(readOnly = true)
  public MatchNotificationWebContract.MatchNotificationPreviewView previewMatch(long matchId) {
    CandidateMissionMatchEvidenceGroup matchGroup = matchQuery.findCandidateMissionMatchEvidenceGroup(matchId)
        .orElseThrow(() -> notFound("candidate-to-slot match not found"));
    MatchPreviewGroup group = buildPreviewGroup(matchGroup);
    if (!isOpenMission(group.mission())) {
      throw notFound("mission not found");
    }
    return previewGroup(group);
  }

  @Transactional(readOnly = true)
  public MatchNotificationWebContract.MatchNotificationSendView sendMatchNotificationMockEmail(long matchId) {
    MatchNotificationWebContract.MatchNotificationPreviewView preview = previewMatch(matchId);
    return deliveryService.sendMock(preview);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public MatchNotificationWebContract.MatchNotificationSendView sendMatchNotificationEmail(
      long matchId,
      MatchNotificationSendSource realSendSource) {

    Objects.requireNonNull(realSendSource, "realSendSource");

    CandidateMissionMatchEvidenceGroup matchGroup = matchQuery.findCandidateMissionMatchEvidenceGroup(matchId)
        .orElseThrow(() -> notFound("candidate-to-slot match not found"));
    MatchPreviewGroup group = buildPreviewGroup(matchGroup);
    if (!isOpenMission(group.mission())) {
      throw notFound("mission not found");
    }
    MatchNotificationWebContract.MatchNotificationPreviewView preview = previewGroup(group);
    return deliveryService.sendRealEmail(
        preview,
        new MatchNotificationDeliveryService.RealEmailDeliveryContext(
            group.deliveryGroupKey(),
            group.contexts().get(0).match().matchedAt()),
        realSendSource);
  }

  private MatchPreviewGroup buildPreviewGroup(CandidateMissionMatchEvidenceGroup matchGroup) {
    CandidateToSlotMatchEvidence primaryMatch = matchGroup.primaryMatch();
    CandidateProfileCardView candidateProfile = candidateProfileQuery
        .candidateProfileCardsById(List.of(primaryMatch.candidateProfileId()))
        .get(primaryMatch.candidateProfileId());
    if (candidateProfile == null) {
      throw notFound("candidateProfile not found");
    }
    RegisteredMissionQuery.MissionReadView mission = missionQueryService.requireMissionForSlot(
        primaryMatch.missionSlotId());
    List<MatchPreviewCtx> contexts = matchGroup.matches().stream()
        .map(candidateMatch -> buildPreviewContext(candidateMatch, mission, candidateProfile))
        .sorted(Comparator
            .comparingInt((MatchPreviewCtx context) -> context.slot().missionSlotNumber())
            .thenComparing(context -> context.match().matchId()))
        .toList();
    if (contexts.isEmpty()) {
      contexts = List.of(buildPreviewContext(primaryMatch, mission, candidateProfile));
    }
    return new MatchPreviewGroup(
        candidateProfile,
        mission,
        contexts,
        matchGroup.deliveryGroupKey());
  }

  private MatchPreviewCtx buildPreviewContext(
      CandidateToSlotMatchEvidence match,
      RegisteredMissionQuery.MissionReadView mission,
      CandidateProfileCardView candidateProfile) {
    RegisteredMissionQuery.MissionSlotReadView slot = missionQueryService.requireMissionSlot(match.missionSlotId());
    String roleTitle = slot.specification().roleTitle();
    return new MatchPreviewCtx(
        match,
        mission,
        slot,
        roleTitle,
        matchGrade(match),
        matchedSkillsWithLevels(match, candidateProfile));
  }

  private MatchNotificationWebContract.MatchNotificationPreviewView previewGroup(MatchPreviewGroup group) {
    CandidateProfileCardView candidateProfile = group.candidateProfile();
    MatchPreviewCtx primaryContext = group.contexts().get(0);

    String candidateName = candidateProfile.displayName();
    String matchedRoles = matchedRoles(candidateProfile);
    String customerName = displayMissionTitle(group.mission().specification().customerName());
    String missionTitle = displayMissionTitle(group.mission().specification().missionTitle());
    String matchGrade = groupMatchGrade(group.contexts());
    String matchedSkills = groupMatchedSkills(group.contexts());
    String generatedAt = formatInstant(primaryContext.match().matchedAt());
    String evidenceBrief = limitWords(buildEvidenceBrief(
        candidateName,
        primaryContext.roleTitle(),
        missionTitle,
        primaryContext.match(),
        matchedSkills));
    MatchNotificationEmailContentBuilder.MailContent mailContent = emailContentBuilder.build(
        new MatchNotificationEmailContentBuilder.MailContentInput(
            candidateName,
            matchedRoles,
            missionSlotLines(group.contexts()),
            customerName,
            missionTitle,
            matchGrade,
            matchedSkills,
            evidenceBrief));

    return new MatchNotificationWebContract.MatchNotificationPreviewView(
        primaryContext.match().matchId(),
        group.contexts().stream().map(context -> context.match().matchId()).toList(),
        primaryContext.match().candidateProfileId(),
        group.mission().missionId(),
        group.contexts().size(),
        mailContent.subject(),
        evidenceBrief,
        mailContent.textBody(),
        mailContent.htmlBody(),
        generatedAt);
  }

  private static boolean isOpenMission(RegisteredMissionQuery.MissionReadView mission) {
    return mission != null && "OPEN".equals(mission.missionAvailability());
  }

  private String groupMatchGrade(List<MatchPreviewCtx> contexts) {
    if (contexts.size() == 1) {
      return contexts.get(0).matchGrade();
    }
    MatchPreviewCtx best = contexts.stream()
        .max(Comparator.comparing(context -> context.match().score()))
        .orElse(contexts.get(0));
    return renderTemplate(mailTemplateCatalog.multiSlotMatchGradeTemplate(), Map.of(
        "count", String.valueOf(contexts.size()),
        "bestMatchGrade", best.matchGrade()));
  }

  private String groupMatchedSkills(List<MatchPreviewCtx> contexts) {
    String noMatchedSkills = mailTemplateCatalog.noMatchedSkillsText();
    String joined = contexts.stream()
        .map(MatchPreviewCtx::matchedSkills)
        .filter(value -> !noMatchedSkills.equalsIgnoreCase(value))
        .distinct()
        .collect(Collectors.joining(" | "));
    return textOrFallback(joined, noMatchedSkills);
  }

  private List<MatchNotificationEmailContentBuilder.MissionSlotLine> missionSlotLines(List<MatchPreviewCtx> contexts) {
    return contexts.stream()
        .map(context -> new MatchNotificationEmailContentBuilder.MissionSlotLine(
            context.slot().missionSlotNumber(),
            context.roleTitle(),
            context.matchGrade(),
            context.matchedSkills()))
        .toList();
  }

  private String buildEvidenceBrief(
      String candidateName,
      String roleTitle,
      String missionTitle,
      CandidateToSlotMatchEvidence match,
      String matchedSkills) {
    String skillEvidence = mailTemplateCatalog.noMatchedSkillsText().equalsIgnoreCase(matchedSkills)
        ? mailTemplateCatalog.skillEvidenceNoMatchedSkillsTemplate()
        : renderTemplate(mailTemplateCatalog.skillEvidenceMatchedSkillsTemplate(), Map.of(
            "matchedSkills", matchedSkills));
    String fitEvidence = match.roleMatched() && match.workModeMatched()
        ? mailTemplateCatalog.fitEvidenceAlignedTemplate()
        : mailTemplateCatalog.fitEvidenceReviewRequiredTemplate();
    return renderTemplate(mailTemplateCatalog.evidenceBriefTemplate(), Map.of(
        "candidateName", candidateName,
        "roleTitle", roleTitle,
        "missionTitle", missionTitle,
        "fitEvidence", fitEvidence,
        "skillEvidence", skillEvidence));
  }

  private String matchGrade(CandidateToSlotMatchEvidence match) {
    int score = match.score();
    String label = textOrFallback(match.scoreLabel(), "");
    String grade = "Excellent Match".equalsIgnoreCase(label) || score >= 90
        ? "Great"
        : textOrFallback(label, "Qualified");
    return grade + " (" + score + " / 100)";
  }

  private String matchedSkillsWithLevels(
      CandidateToSlotMatchEvidence match,
      CandidateProfileCardView candidateProfile) {

    List<Long> matchedSkillIds = match.matchedSkillIds();
    List<String> matchedSkillTitles = deduplicatedSkillTitles(match.matchedSkillTitles());
    if (matchedSkillTitles.isEmpty()) {
      return mailTemplateCatalog.noMatchedSkillsText();
    }

    Map<Long, String> levelBySkillId = new LinkedHashMap<>();
    Map<String, String> levelBySkillTitle = new LinkedHashMap<>();
    candidateProfile.primarySkills().forEach(skill -> recordSkillLevel(skill, levelBySkillId, levelBySkillTitle));
    candidateProfile.secondarySkills().forEach(skill -> recordSkillLevel(skill, levelBySkillId, levelBySkillTitle));

    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < matchedSkillTitles.size(); index++) {
      String title = matchedSkillTitles.get(index);
      String level = "N/A";
      if (index < matchedSkillIds.size()) {
        level = levelBySkillId.getOrDefault(matchedSkillIds.get(index), level);
      }
      if ("N/A".equals(level)) {
        level = levelBySkillTitle.getOrDefault(title.trim().toLowerCase(), level);
      }
      if (builder.length() > 0) {
        builder.append(" | ");
      }
      builder.append(title).append(" (").append(level).append(")");
    }
    return builder.toString();
  }

  private void recordSkillLevel(
      CandidateProfileSkillView skill,
      Map<Long, String> levelBySkillId,
      Map<String, String> levelBySkillTitle) {
    if (skill.skillId() > 0) {
      levelBySkillId.putIfAbsent(skill.skillId(), textOrFallback(skill.skillLevelName(), "N/A"));
    }
    if (skill.skillTitle() != null && !skill.skillTitle().isBlank()) {
      levelBySkillTitle.putIfAbsent(
          skill.skillTitle().trim().toLowerCase(),
          textOrFallback(skill.skillLevelName(), "N/A"));
    }
  }

  private List<String> deduplicatedSkillTitles(List<String> matchedSkillTitles) {
    Set<String> seen = new java.util.LinkedHashSet<>();
    if (matchedSkillTitles == null || matchedSkillTitles.isEmpty()) {
      return List.of();
    }
    return matchedSkillTitles.stream()
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .filter(seen::add)
        .toList();
  }

  private String matchedRoles(CandidateProfileCardView candidateProfile) {
    String roles = candidateProfile.roles().stream()
        .filter(role -> role.roleTitle() != null && !role.roleTitle().isBlank())
        .sorted(Comparator.comparing(CandidateProfileRoleView::roleOrder))
        .map(this::formatMatchedRole)
        .distinct()
        .collect(Collectors.joining(" | "));
    return textOrFallback(roles, fallbackMatchedRole(candidateProfile));
  }

  private String formatMatchedRole(CandidateProfileRoleView role) {
    return role.roleTitle().trim()
        + " ("
        + roleExperienceLabel(role.competencyLevelId(), role.roleExperienceYears())
        + ")";
  }

  private String fallbackMatchedRole(CandidateProfileCardView candidateProfile) {
    String roleTitle = textOrFallback(candidateProfile.primaryRoleTitle(), "N/A");
    String roleExperienceLevel = textOrFallback(candidateProfile.primaryRoleExperienceLevel(), "N/A");
    return "N/A".equals(roleTitle) ? roleTitle : roleTitle + " (" + roleExperienceLevel + ")";
  }

  private String roleExperienceLabel(short lookupId, int years) {
    if (lookupId > 0) {
      return switch (lookupId) {
        case 3 -> "SENIOR";
        case 2 -> "INTERMEDIATE";
        case 1 -> "JUNIOR";
        default -> "N/A";
      };
    }
    if (years <= 0) {
      return "N/A";
    }
    if (years >= 5) {
      return "SENIOR";
    }
    if (years >= 3) {
      return "INTERMEDIATE";
    }
    return "JUNIOR";
  }

  private MatchNotificationNotFoundException notFound(String reason) {
    return new MatchNotificationNotFoundException(reason);
  }

  private static String limitWords(String value) {
    String[] words = Arrays.stream(textOrFallback(value, "").trim().split("\\s+"))
        .filter(word -> !word.isBlank())
        .toArray(String[]::new);
    if (words.length <= MAX_RATIONALE_WORDS) {
      return String.join(" ", words);
    }
    return String.join(" ", Arrays.copyOf(words, MAX_RATIONALE_WORDS));
  }

  private static String textOrFallback(String value, String fallback) {
    return value == null || value.trim().isBlank() ? fallback : value.trim();
  }

  private static String renderTemplate(String template, Map<String, String> values) {
    Objects.requireNonNull(template, "template");
    Map<String, String> safeValues = values == null ? Map.of() : values;
    Matcher matcher = TEMPLATE_TOKEN_PATTERN.matcher(template);
    StringBuilder rendered = new StringBuilder();
    while (matcher.find()) {
      String token = matcher.group(1);
      String value = safeValues.get(token);
      if (value == null) {
        throw new IllegalStateException("Match notification email template token is missing: " + token);
      }
      matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(rendered);
    return rendered.toString();
  }

  private static String displayMissionTitle(String value) {
    return textOrFallback(value, "Untitled mission")
        .replace(":::", " - ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private static String formatInstant(Instant instant) {
    return instant == null
        ? null
        : TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
  }
}
