package mcp.server.domain.candidate_profiles.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CandidateProfileEvidence(
        long candidateProfileId,
        String displayName,
        String firstName,
        String lastName,
        String contactEmail,
        String primaryRoleTitle,
        String primaryRoleExperienceLevel,
        Integer primaryRoleExperienceYears,
        String workStatus,
        String country,
        String city,
        String locationFlexibility,
        String workMode,
        String profileSummary,
        String yearsOfExperience,
        GeneratedSummary generatedSummary,
        List<CandidateProfileSkillView> primarySkills,
        List<CandidateProfileSkillView> secondarySkills,
        List<WorkExperience> workExperiences,
        List<Education> educations,
        List<Certification> certifications) {

    public CandidateProfileEvidence {
        generatedSummary = generatedSummary == null ? GeneratedSummary.empty() : generatedSummary;
        primarySkills = primarySkills == null ? List.of() : List.copyOf(primarySkills);
        secondarySkills = secondarySkills == null ? List.of() : List.copyOf(secondarySkills);
        workExperiences = workExperiences == null ? List.of() : List.copyOf(workExperiences);
        educations = educations == null ? List.of() : List.copyOf(educations);
        certifications = certifications == null ? List.of() : List.copyOf(certifications);
    }

    public record GeneratedSummary(
            String status,
            String coreCompetenceOverview,
            String location,
            String otherDetails,
            Instant generatedAt) {

        public static GeneratedSummary empty() {
            return new GeneratedSummary("", "", "", "", null);
        }
    }

    public record WorkExperience(
            int order,
            String jobTitle,
            String organizationName,
            String organizationRegistrationNumber,
            String city,
            String country,
            LocalDate startDate,
            LocalDate endDate,
            Boolean current) {
    }

    public record Education(
            int order,
            String institution,
            String fieldOfStudy,
            LocalDate startDate,
            LocalDate endDate,
            Boolean current) {
    }

    public record Certification(
            int order,
            String certificationName) {
    }
}
