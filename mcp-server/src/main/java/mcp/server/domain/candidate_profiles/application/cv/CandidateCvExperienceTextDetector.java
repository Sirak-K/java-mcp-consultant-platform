package mcp.server.domain.candidate_profiles.application.cv;

import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import mcp.server.domain.reference_data.application.CompanyIdentityLookupService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
final class CandidateCvExperienceTextDetector {

  private static final Pattern YEARS_EXPERIENCE_PATTERN = Pattern
      .compile("(?i)\\b(\\d{1,2})\\+?\\s*(?:years?|yrs?|\\u00E5rs?)\\s+(?:of\\s+)?experience\\b");

  private final CompanyIdentityLookupService companyIdentityLookupService;
  private final CandidateCvLocationTextDetector locationTextDetector;
  private final CandidateCvDateRangeParser dateRangeParser;

  CandidateCvExperienceTextDetector(
      CompanyIdentityLookupService companyIdentityLookupService,
      CandidateCvLocationTextDetector locationTextDetector,
      CandidateCvDateRangeParser dateRangeParser) {
    this.companyIdentityLookupService = companyIdentityLookupService;
    this.locationTextDetector = locationTextDetector;
    this.dateRangeParser = dateRangeParser;
  }

  List<CandidateCvWebContract.CandidateEducationWorkingCopyView> detectEducations(
      List<String> educationLines) {

    List<CandidateCvWebContract.CandidateEducationWorkingCopyView> educations = new ArrayList<>();
    for (int index = 0; index < educationLines.size(); index++) {
      String line = educationLines.get(index);
      if (!dateRangeParser.parse(line).isEmpty()) {
        continue;
      }
      if (!line.contains(" \u2013 ") && !line.contains(" - ")) {
        continue;
      }
      String[] titleAndInstitution = line.split("\\s+[\\u2013-]\\s+", 2);
      CandidateCvDateRangeParser.ParsedDateRange dates = CandidateCvDateRangeParser.ParsedDateRange.empty();
      if (index + 1 < educationLines.size()) {
        CandidateCvDateRangeParser.ParsedDateRange candidateDates = dateRangeParser.parse(educationLines.get(index + 1));
        if (!candidateDates.isEmpty()) {
          dates = candidateDates;
          index++;
        }
      }
      String fieldOfStudy = titleAndInstitution[0].trim();
      String institution = titleAndInstitution.length > 1 ? titleAndInstitution[1].trim() : "";
      if (dateRangeParser.parse(fieldOfStudy).isEmpty()
          && dateRangeParser.parse(institution).isEmpty()
          && !fieldOfStudy.isBlank()
          && !institution.isBlank()) {
        educations.add(new CandidateCvWebContract.CandidateEducationWorkingCopyView(
            institution,
            fieldOfStudy,
            dates.startDate(),
            dates.endDate(),
            dates.current()));
      }
    }
    return educations;
  }

  List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> detectWorkExperiences(
      List<String> workLines,
      CandidateCvLocationTextDetector.DetectedLocation candidateLocation) {

    List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> experiences = new ArrayList<>();
    for (int index = 0; index < workLines.size(); index++) {
      String line = workLines.get(index);
      if (line.isBlank() || !dateRangeParser.parse(line).isEmpty()) {
        continue;
      }
      String[] titleAndCompany = line.split("\\s+[\\u2013-]\\s+", 2);
      String jobTitle = titleAndCompany[0].trim();
      String workCompanyName = titleAndCompany.length > 1 ? titleAndCompany[1].trim() : "";
      CompanyIdentityLookupService.CompanyIdentityResolution companyIdentity = companyIdentityLookupService
          .resolve(workCompanyName);
      String workExpCompany = companyIdentity.organisationName().isBlank()
          ? workCompanyName
          : companyIdentity.organisationName();
      CandidateCvLocationTextDetector.DetectedLocation workLocation = locationTextDetector.detectLocationFromText(line);
      String companyIdentityCity = safeText(companyIdentity.organisationCity()).trim();
      if (!companyIdentityCity.isBlank()) {
        workLocation = new CandidateCvLocationTextDetector.DetectedLocation(companyIdentityCity, "Sweden");
      } else if (workLocation.isEmpty() && "Sweden".equals(candidateLocation.country())) {
        workLocation = new CandidateCvLocationTextDetector.DetectedLocation("", "Sweden");
      }
      CandidateCvDateRangeParser.ParsedDateRange dates = CandidateCvDateRangeParser.ParsedDateRange.empty();
      if (index + 1 < workLines.size()) {
        CandidateCvDateRangeParser.ParsedDateRange candidateDates = dateRangeParser.parse(workLines.get(index + 1));
        if (!candidateDates.isEmpty()) {
          dates = candidateDates;
          index++;
        }
      }
      experiences.add(new CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView(
          jobTitle,
          workExpCompany,
          companyIdentity.organisationNumber(),
          companyIdentityOptions(companyIdentity.options()),
          workLocation.city(),
          workLocation.country(),
          dates.startDate(),
          dates.endDate(),
          dates.current()));
    }
    return experiences;
  }

  String detectYearsOfExperience(
      String extractedText,
      List<CandidateCvWebContract.CandidateEducationWorkingCopyView> educations,
      List<CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView> workExperiences) {

    Matcher matcher = YEARS_EXPERIENCE_PATTERN.matcher(extractedText);
    if (matcher.find()) {
      return matcher.group(1);
    }
    ParsedEducation firstEducation = educations.stream()
        .findFirst()
        .map(this::toParsedEducation)
        .orElse(ParsedEducation.empty());
    if (!firstEducation.startDate().isBlank()) {
      try {
        int startYear = LocalDate.parse(firstEducation.startDate()).getYear();
        return Integer.toString(Math.max(0, Year.now().getValue() - startYear));
      } catch (DateTimeParseException ignored) {
        // Try work-experience start dates next.
      }
    }
    Integer earliestStartYear = null;
    for (CandidateCvWebContract.CandidateWorkExperienceWorkingCopyView workExperience : workExperiences) {
      if (workExperience.startDate().isBlank()) {
        continue;
      }
      try {
        int startYear = LocalDate.parse(workExperience.startDate()).getYear();
        earliestStartYear = earliestStartYear == null ? startYear : Math.min(earliestStartYear, startYear);
      } catch (DateTimeParseException ignored) {
        // Continue to other entries.
      }
    }
    if (earliestStartYear != null) {
      return Integer.toString(Math.max(0, Year.now().getValue() - earliestStartYear));
    }
    return "";
  }

  private List<CandidateCvWebContract.CandidateCompanyIdentityOptionView> companyIdentityOptions(
      List<CompanyIdentityLookupService.CompanyIdentityOption> options) {
    if (options == null || options.isEmpty()) {
      return List.of();
    }
    return options.stream()
        .map(option -> new CandidateCvWebContract.CandidateCompanyIdentityOptionView(
            option.organisationName(),
            option.organisationNumber(),
            option.organisationCity()))
        .toList();
  }

  private ParsedEducation toParsedEducation(
      CandidateCvWebContract.CandidateEducationWorkingCopyView education) {

    return new ParsedEducation(
        safeText(education.institution()).trim(),
        safeText(education.fieldOfStudy()).trim(),
        safeText(education.startDate()).trim(),
        safeText(education.endDate()).trim(),
        education.currentlyStudying());
  }

  private record ParsedEducation(
      String institution,
      String fieldOfStudy,
      String startDate,
      String endDate,
      boolean current) {

    static ParsedEducation empty() {
      return new ParsedEducation("", "", "", "", false);
    }
  }
}
