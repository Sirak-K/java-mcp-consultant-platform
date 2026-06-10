package mcp.server.domain.missions.application.intake;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
final class MissionProposalScheduleTextDetector {

  private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
  private static final Pattern DURATION_MONTHS_PATTERN = Pattern
      .compile("(?iu)\\b(\\d{1,2})\\s*(?:m\\u00E5nader|manader|months?|mos?)\\b");

  private final MissionProposalTextDetectionCatalogService textDetectionCatalogService;
  private final MissionProposalTextEvidenceRecorder evidenceRecorder;
  private final Pattern writtenDatePattern;
  private final Pattern monthYearDatePattern;

  MissionProposalScheduleTextDetector(
      MissionProposalTextDetectionCatalogService textDetectionCatalogService,
      MissionProposalTextDetectionPatternFactory patternFactory,
      MissionProposalTextEvidenceRecorder evidenceRecorder) {
    this.textDetectionCatalogService = textDetectionCatalogService;
    this.evidenceRecorder = evidenceRecorder;
    this.writtenDatePattern = patternFactory.writtenDatePattern();
    this.monthYearDatePattern = patternFactory.monthYearDatePattern();
  }

  DetectedDateRange detectDateRange(
      String sourceText,
      List<MissionProposalIntake.WorkingCopyEvidenceView> evidence) {

    List<DetectedDate> dates = detectDates(sourceText);
    DetectedDate start = dates.isEmpty() ? null : dates.get(0);
    DetectedDate end = dates.size() < 2 ? null : dates.get(1);

    String startDate = start == null ? "" : start.date();
    String endDate = end == null ? "" : end.date();
    if (!startDate.isBlank()
        && !endDate.isBlank()
        && LocalDate.parse(endDate).isBefore(LocalDate.parse(startDate))) {
      endDate = "";
      end = null;
    }

    if (!startDate.isBlank() && endDate.isBlank()) {
      DetectedDuration duration = detectDurationMonths(sourceText);
      if (duration.detected()) {
        endDate = LocalDate.parse(startDate)
            .plusMonths(duration.months())
            .minusDays(1)
            .toString();
        evidenceRecorder.add(evidence, "endDate", endDate, duration.sourceText());
      }
    }
    if (start != null && !startDate.isBlank()) {
      evidenceRecorder.add(evidence, "startDate", startDate, start.sourceText());
    }
    if (end != null && !endDate.isBlank()) {
      evidenceRecorder.add(evidence, "endDate", endDate, end.sourceText());
    }
    return new DetectedDateRange(startDate, endDate);
  }

  private List<DetectedDate> detectDates(String sourceText) {
    List<DetectedDate> dates = new ArrayList<>();
    Matcher isoMatcher = ISO_DATE_PATTERN.matcher(sourceText);
    while (isoMatcher.find()) {
      addDateCandidate(dates, isoMatcher.group(), isoMatcher.group(), isoMatcher.start());
    }

    Matcher writtenMatcher = writtenDatePattern.matcher(sourceText);
    while (writtenMatcher.find()) {
      String date = toIsoDate(
          Integer.parseInt(writtenMatcher.group(1)),
          textDetectionCatalogService.missionMonthNumber(writtenMatcher.group(2)),
          Integer.parseInt(writtenMatcher.group(3)));
      addDateCandidate(dates, date, writtenMatcher.group(), writtenMatcher.start());
    }

    Matcher monthYearMatcher = monthYearDatePattern.matcher(sourceText);
    while (monthYearMatcher.find()) {
      String date = toIsoDate(
          1,
          textDetectionCatalogService.missionMonthNumber(monthYearMatcher.group(1)),
          Integer.parseInt(monthYearMatcher.group(2)));
      addDateCandidate(dates, date, monthYearMatcher.group(), monthYearMatcher.start());
    }

    return dates.stream()
        .sorted(Comparator.comparingInt(DetectedDate::sourceIndex))
        .distinct()
        .limit(2)
        .toList();
  }

  private void addDateCandidate(
      List<DetectedDate> dates,
      String date,
      String sourceText,
      int sourceIndex) {

    if (isValidDate(date) && dates.stream().noneMatch(existing -> existing.date().equals(date))) {
      dates.add(new DetectedDate(date, sourceText, sourceIndex));
    }
  }

  private DetectedDuration detectDurationMonths(String sourceText) {
    Matcher matcher = DURATION_MONTHS_PATTERN.matcher(sourceText);
    if (!matcher.find()) {
      return new DetectedDuration(0, "", false);
    }
    int months = Integer.parseInt(matcher.group(1));
    if (months < 1 || months > 60) {
      return new DetectedDuration(0, "", false);
    }
    return new DetectedDuration(months, matcher.group(), true);
  }

  private String toIsoDate(int day, int month, int year) {
    if (month < 1 || year < 1900 || year > 2100) {
      return "";
    }
    YearMonth yearMonth = YearMonth.of(year, month);
    if (day < 1 || day > yearMonth.lengthOfMonth()) {
      return "";
    }
    return LocalDate.of(year, month, day).toString();
  }

  private boolean isValidDate(String value) {
    try {
      LocalDate.parse(value);
      return true;
    } catch (DateTimeParseException exception) {
      return false;
    }
  }

  record DetectedDateRange(String startDate, String endDate) {
  }

  private record DetectedDate(String date, String sourceText, int sourceIndex) {
  }

  private record DetectedDuration(int months, String sourceText, boolean detected) {
  }
}
