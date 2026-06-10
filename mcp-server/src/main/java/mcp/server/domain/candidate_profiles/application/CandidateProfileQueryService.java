package mcp.server.domain.candidate_profiles.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentEligibility;
import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentRequirement;
import mcp.server.domain.candidate_profiles.api.CandidateProfileCardView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileEvidence;
import mcp.server.domain.candidate_profiles.api.CandidateProfileMatchingView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.candidate_profiles.api.CandidateProfileRoleView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileSkillView;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileRoleEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileJpaRepo;

@Service
@Transactional(readOnly = true)
public class CandidateProfileQueryService implements CandidateProfileQuery {

    private static final String PRIMARY_SKILL_CATEGORY = "PRIMARY";
    private static final String SECONDARY_SKILL_CATEGORY = "SECONDARY";
    private static final String CV_SUMMARY_NOT_GENERATED = "NOT_GENERATED";

    private final CandidateProfileJpaRepo candidateProfileRepo;

    public CandidateProfileQueryService(CandidateProfileJpaRepo candidateProfileRepo) {
        this.candidateProfileRepo = candidateProfileRepo;
    }

    @Override
    public long countRegisteredCandidateProfiles() {
        return candidateProfileRepo.count();
    }

    @Override
    public List<CandidateProfileMatchingView> matchableCandidateProfiles() {
        return candidateProfileRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toMatchingView)
                .filter(this::hasMatchingInputs)
                .toList();
    }

    @Override
    public Optional<CandidateProfileMatchingView> findMatchingProfile(long candidateProfileId) {
        return candidateProfileRepo.findById(candidateProfileId)
                .map(this::toMatchingView)
                .filter(this::hasMatchingInputs);
    }

    @Override
    public Map<Long, CandidateProfileCardView> candidateProfileCardsById(Collection<Long> candidateProfileIds) {
        if (candidateProfileIds == null || candidateProfileIds.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = candidateProfileIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, CandidateProfileCardView> cardsById = new LinkedHashMap<>();
        for (CandidateProfileEntity candidateProfile : candidateProfileRepo.findAllById(ids)) {
            CandidateProfileCardView card = toCardView(candidateProfile);
            cardsById.put(card.candidateProfileId(), card);
        }
        return Map.copyOf(cardsById);
    }

    @Override
    public Optional<CandidateProfileEvidence> findEvidenceProfile(long candidateProfileId) {
        return candidateProfileRepo.findById(candidateProfileId)
                .map(this::toEvidence);
    }

    @Override
    public CandidateProfileAssignmentEligibility evaluateAssignmentEligibility(
            CandidateProfileAssignmentRequirement requirement) {

        Optional<CandidateProfileEntity> candidateProfile = candidateProfileRepo.findById(requirement.candidateProfileId());
        if (candidateProfile.isEmpty()) {
            return new CandidateProfileAssignmentEligibility(
                    requirement.candidateProfileId(),
                    false,
                    false,
                    false,
                    false,
                    false,
                    List.of("Candidate profile was not found."));
        }

        CandidateProfileMatchingView matchingProfile = toMatchingView(candidateProfile.get());
        boolean assignmentAvailable = !"UNAVAILABLE".equalsIgnoreCase(matchingProfile.workStatus());
        boolean roleMatched = matchingProfile.roles().stream()
                .anyMatch(role -> role.roleId() == requirement.requiredRoleId());
        boolean requiredSkillMatched = requirement.requiredSkills().stream()
                .anyMatch(requiredSkill -> matchingProfile.skills().stream()
                        .anyMatch(candidateSkill -> candidateSkill.skillId() == requiredSkill.skillId()
                                && candidateSkill.skillLevelId() >= requiredSkill.minimumSkillLevelId()));

        List<String> rejectionReasons = new ArrayList<>();
        if (!assignmentAvailable) {
            rejectionReasons.add("Candidate profile is unavailable for assignment.");
        }
        if (!roleMatched) {
            rejectionReasons.add("Candidate profile does not contain the required role.");
        }
        if (!requiredSkillMatched) {
            rejectionReasons.add("Candidate profile does not satisfy a required skill at the required level.");
        }

        return new CandidateProfileAssignmentEligibility(
                requirement.candidateProfileId(),
                true,
                assignmentAvailable,
                roleMatched,
                requiredSkillMatched,
                assignmentAvailable && roleMatched && requiredSkillMatched,
                rejectionReasons);
    }

    private CandidateProfileMatchingView toMatchingView(CandidateProfileEntity entity) {
        return new CandidateProfileMatchingView(
                safeLong(entity.getId()),
                displayName(entity),
                safeText(entity.getFirstName()),
                safeText(entity.getLastName()),
                safeText(entity.getContactEmail()),
                safeText(entity.getWorkStatus()),
                safeText(entity.getWorkMode()),
                roleViews(entity),
                allSkillViews(entity));
    }

    private CandidateProfileCardView toCardView(CandidateProfileEntity entity) {
        CandidateProfileRoleEntity primaryRole = primaryRole(entity).orElse(null);
        Integer roleExperienceYears = primaryRole == null ? null : safeInt(primaryRole.getRoleExperienceYears());
        return new CandidateProfileCardView(
                safeLong(entity.getId()),
                displayName(entity),
                primaryRole == null ? "" : safeText(primaryRole.getRoleTitle()),
                roleExperienceLevel(roleExperienceYears),
                roleExperienceYears,
                safeText(entity.getWorkStatus()),
                safeText(entity.getCountry()),
                locationFlexibility(entity),
                safeText(entity.getWorkMode()),
                roleViews(entity),
                primarySkillViews(entity),
                secondarySkillViews(entity));
    }

    private CandidateProfileEvidence toEvidence(CandidateProfileEntity entity) {
        CandidateProfileRoleEntity primaryRole = primaryRole(entity).orElse(null);
        Integer roleExperienceYears = primaryRole == null ? null : safeInt(primaryRole.getRoleExperienceYears());
        return new CandidateProfileEvidence(
                safeLong(entity.getId()),
                displayName(entity),
                safeText(entity.getFirstName()),
                safeText(entity.getLastName()),
                safeText(entity.getContactEmail()),
                primaryRole == null ? "" : safeText(primaryRole.getRoleTitle()),
                roleExperienceLevel(roleExperienceYears),
                roleExperienceYears,
                safeText(entity.getWorkStatus()),
                safeText(entity.getCountry()),
                safeText(entity.getCity()),
                locationFlexibility(entity),
                safeText(entity.getWorkMode()),
                safeText(entity.getProfileSummary()),
                safeText(entity.getYearsOfExperience()),
                generatedSummary(entity),
                primarySkillViews(entity),
                secondarySkillViews(entity),
                workExperienceViews(entity),
                educationViews(entity),
                certificationViews(entity));
    }

    private List<CandidateProfileRoleView> roleViews(CandidateProfileEntity entity) {
        return entity.getCandidateRoles().stream()
                .sorted(Comparator.comparing(role -> safeInt(role.getRoleNumber())))
                .map(role -> new CandidateProfileRoleView(
                        safeInt(role.getRoleNumber()),
                        safeLong(role.getRoleId()),
                        safeText(role.getRoleTitle()),
                        safeInt(role.getRoleExperienceYears()),
                        safeShort(role.getCompetencyLevelId())))
                .toList();
    }

    private List<CandidateProfileSkillView> allSkillViews(CandidateProfileEntity entity) {
        return Stream.concat(primarySkillViews(entity).stream(), secondarySkillViews(entity).stream())
                .toList();
    }

    private List<CandidateProfileSkillView> primarySkillViews(CandidateProfileEntity entity) {
        return entity.getPrimarySkills().stream()
                .sorted(Comparator.comparing(skill -> safeInt(skill.getSkillNumber())))
                .map(skill -> new CandidateProfileSkillView(
                        PRIMARY_SKILL_CATEGORY,
                        safeLong(skill.getSkillId()),
                        safeText(skill.getSkillTitle()),
                        safeShort(skill.getSkillLevelId()),
                        safeText(skill.getSkillLevelName())))
                .toList();
    }

    private List<CandidateProfileSkillView> secondarySkillViews(CandidateProfileEntity entity) {
        return entity.getSecondarySkills().stream()
                .sorted(Comparator.comparing(skill -> safeInt(skill.getSkillNumber())))
                .map(skill -> new CandidateProfileSkillView(
                        SECONDARY_SKILL_CATEGORY,
                        safeLong(skill.getSkillId()),
                        safeText(skill.getSkillTitle()),
                        safeShort(skill.getSkillLevelId()),
                        safeText(skill.getSkillLevelName())))
                .toList();
    }

    private List<CandidateProfileEvidence.WorkExperience> workExperienceViews(CandidateProfileEntity entity) {
        return entity.getWorkExperiences().stream()
                .sorted(Comparator.comparing(workExperience -> safeInt(workExperience.getWorkExperienceNumber())))
                .map(workExperience -> new CandidateProfileEvidence.WorkExperience(
                        safeInt(workExperience.getWorkExperienceNumber()),
                        safeText(workExperience.getJobTitle()),
                        safeText(workExperience.getWorkExpCompany()),
                        safeText(workExperience.getWorkExpCompanyOrgNr()),
                        safeText(workExperience.getCity()),
                        safeText(workExperience.getCountry()),
                        workExperience.getStartDate(),
                        workExperience.getEndDate(),
                        workExperience.getCurrentlyHere()))
                .toList();
    }

    private List<CandidateProfileEvidence.Education> educationViews(CandidateProfileEntity entity) {
        return entity.getEducations().stream()
                .sorted(Comparator.comparing(education -> safeInt(education.getEducationNumber())))
                .map(education -> new CandidateProfileEvidence.Education(
                        safeInt(education.getEducationNumber()),
                        safeText(education.getInstitution()),
                        safeText(education.getFieldOfStudy()),
                        education.getStartDate(),
                        education.getEndDate(),
                        education.getCurrentlyStudying()))
                .toList();
    }

    private List<CandidateProfileEvidence.Certification> certificationViews(CandidateProfileEntity entity) {
        return entity.getCertifications().stream()
                .sorted(Comparator.comparing(certification -> safeInt(certification.getCertificationNumber())))
                .map(certification -> new CandidateProfileEvidence.Certification(
                        safeInt(certification.getCertificationNumber()),
                        safeText(certification.getCertificationName())))
                .toList();
    }

    private CandidateProfileEvidence.GeneratedSummary generatedSummary(CandidateProfileEntity entity) {
        String status = safeText(entity.getCvSummaryStatus()).isBlank()
                ? CV_SUMMARY_NOT_GENERATED
                : safeText(entity.getCvSummaryStatus());
        return new CandidateProfileEvidence.GeneratedSummary(
                status,
                safeText(entity.getCvSummaryCoreCompetenceOverview()),
                safeText(entity.getCvSummaryLocation()),
                safeText(entity.getCvSummaryOtherDetails()),
                entity.getCvSummaryGeneratedAt());
    }

    private Optional<CandidateProfileRoleEntity> primaryRole(CandidateProfileEntity entity) {
        return entity.getCandidateRoles().stream()
                .min(Comparator.comparing(role -> safeInt(role.getRoleNumber())));
    }

    private boolean hasMatchingInputs(CandidateProfileMatchingView profile) {
        return !profile.roles().isEmpty()
                && !profile.skills().isEmpty()
                && profile.skills().stream().allMatch(skill -> skill.skillLevelId() > 0)
                && !profile.workMode().isBlank()
                && !profile.firstName().isBlank()
                && !profile.lastName().isBlank()
                && !profile.contactEmail().isBlank();
    }

    private String displayName(CandidateProfileEntity entity) {
        String fullName = (safeText(entity.getFirstName()).trim() + " " + safeText(entity.getLastName()).trim())
                .trim();
        return fullName.isBlank() ? safeText(entity.getContactEmail()).trim() : fullName;
    }

    private String roleExperienceLevel(Integer years) {
        if (years == null) {
            return "";
        }
        if (years >= 5) {
            return "SENIOR";
        }
        if (years >= 3) {
            return "INTERMEDIATE";
        }
        return "JUNIOR";
    }

    private String locationFlexibility(CandidateProfileEntity entity) {
        String explicitFlexibility = safeText(entity.getLocationFlexibility()).trim();
        if (!explicitFlexibility.isBlank()) {
            return explicitFlexibility;
        }
        return Boolean.TRUE.equals(entity.getWillingToRelocate())
                ? "Willing to Relocate"
                : "Unwilling to Relocate";
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static int safeInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    private static short safeShort(Number value) {
        return value == null ? 0 : value.shortValue();
    }
}
