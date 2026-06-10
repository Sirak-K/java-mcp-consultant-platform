package mcp.server.domain.candidate_profiles.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentRequirement;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfilePrimarySkillEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileRoleEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileSecondarySkillEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateProfileJpaRepo;

class CandidateProfileQueryServiceTest {

    @Test
    void matchableCandidateProfilesOnlyReturnsProfilesWithCoreMatchingInputs() {
        CandidateProfileJpaRepo repo = mock(CandidateProfileJpaRepo.class);
        CandidateProfileQueryService service = new CandidateProfileQueryService(repo);
        CandidateProfileEntity matchable = candidateProfile(
                10L,
                "Ada",
                "Lovelace",
                "ada@example.test",
                "REMOTE",
                List.of(role(7L, "Backend Developer", (short) 5)),
                List.of(primarySkill(42L, "Java", (short) 4)),
                List.of());
        CandidateProfileEntity incomplete = candidateProfile(
                11L,
                "",
                "Incomplete",
                "incomplete@example.test",
                "REMOTE",
                List.of(role(7L, "Backend Developer", (short) 5)),
                List.of(primarySkill(42L, "Java", (short) 4)),
                List.of());
        when(repo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(incomplete, matchable));

        assertThat(service.matchableCandidateProfiles())
                .singleElement()
                .satisfies(profile -> {
                    assertThat(profile.candidateProfileId()).isEqualTo(10L);
                    assertThat(profile.displayName()).isEqualTo("Ada Lovelace");
                    assertThat(profile.roles()).singleElement()
                            .satisfies(role -> assertThat(role.roleId()).isEqualTo(7L));
                    assertThat(profile.skills()).singleElement()
                            .satisfies(skill -> assertThat(skill.skillId()).isEqualTo(42L));
                });
    }

    @Test
    void assignmentEligibilityRequiresRequiredRoleAndSkillLevel() {
        CandidateProfileJpaRepo repo = mock(CandidateProfileJpaRepo.class);
        CandidateProfileQueryService service = new CandidateProfileQueryService(repo);
        CandidateProfileEntity candidate = candidateProfile(
                10L,
                "Ada",
                "Lovelace",
                "ada@example.test",
                "REMOTE",
                List.of(role(7L, "Backend Developer", (short) 5)),
                List.of(primarySkill(42L, "Java", (short) 4)),
                List.of(secondarySkill(84L, "PostgreSQL", (short) 2)));
        when(repo.findById(10L)).thenReturn(Optional.of(candidate));

        var eligible = service.evaluateAssignmentEligibility(new CandidateProfileAssignmentRequirement(
                10L,
                7L,
                List.of(new CandidateProfileAssignmentRequirement.RequiredSkill(42L, (short) 3))));

        assertThat(eligible.eligible()).isTrue();
        assertThat(eligible.assignmentAvailable()).isTrue();
        assertThat(eligible.roleMatched()).isTrue();
        assertThat(eligible.requiredSkillMatched()).isTrue();
        assertThat(eligible.rejectionReasons()).isEmpty();

        var wrongRole = service.evaluateAssignmentEligibility(new CandidateProfileAssignmentRequirement(
                10L,
                99L,
                List.of(new CandidateProfileAssignmentRequirement.RequiredSkill(42L, (short) 3))));

        assertThat(wrongRole.eligible()).isFalse();
        assertThat(wrongRole.assignmentAvailable()).isTrue();
        assertThat(wrongRole.roleMatched()).isFalse();
        assertThat(wrongRole.requiredSkillMatched()).isTrue();
        assertThat(wrongRole.rejectionReasons()).containsExactly(
                "Candidate profile does not contain the required role.");

        var weakSkill = service.evaluateAssignmentEligibility(new CandidateProfileAssignmentRequirement(
                10L,
                7L,
                List.of(new CandidateProfileAssignmentRequirement.RequiredSkill(42L, (short) 5))));

        assertThat(weakSkill.eligible()).isFalse();
        assertThat(weakSkill.assignmentAvailable()).isTrue();
        assertThat(weakSkill.roleMatched()).isTrue();
        assertThat(weakSkill.requiredSkillMatched()).isFalse();
        assertThat(weakSkill.rejectionReasons()).containsExactly(
                "Candidate profile does not satisfy a required skill at the required level.");
    }

    @Test
    void assignmentEligibilityRejectsUnavailableCandidateProfile() {
        CandidateProfileJpaRepo repo = mock(CandidateProfileJpaRepo.class);
        CandidateProfileQueryService service = new CandidateProfileQueryService(repo);
        CandidateProfileEntity candidate = candidateProfile(
                10L,
                "Ada",
                "Lovelace",
                "ada@example.test",
                "UNAVAILABLE",
                "REMOTE",
                List.of(role(7L, "Backend Developer", (short) 5)),
                List.of(primarySkill(42L, "Java", (short) 4)),
                List.of());
        when(repo.findById(10L)).thenReturn(Optional.of(candidate));

        var eligibility = service.evaluateAssignmentEligibility(new CandidateProfileAssignmentRequirement(
                10L,
                7L,
                List.of(new CandidateProfileAssignmentRequirement.RequiredSkill(42L, (short) 3))));

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.assignmentAvailable()).isFalse();
        assertThat(eligibility.roleMatched()).isTrue();
        assertThat(eligibility.requiredSkillMatched()).isTrue();
        assertThat(eligibility.rejectionReasons()).containsExactly(
                "Candidate profile is unavailable for assignment.");
    }

    private static CandidateProfileEntity candidateProfile(
            Long id,
            String firstName,
            String lastName,
            String contactEmail,
            String workMode,
            List<CandidateProfileRoleEntity> roles,
            List<CandidateProfilePrimarySkillEntity> primarySkills,
            List<CandidateProfileSecondarySkillEntity> secondarySkills) {
        return candidateProfile(
                id,
                firstName,
                lastName,
                contactEmail,
                "AVAILABLE",
                workMode,
                roles,
                primarySkills,
                secondarySkills);
    }

    private static CandidateProfileEntity candidateProfile(
            Long id,
            String firstName,
            String lastName,
            String contactEmail,
            String workStatus,
            String workMode,
            List<CandidateProfileRoleEntity> roles,
            List<CandidateProfilePrimarySkillEntity> primarySkills,
            List<CandidateProfileSecondarySkillEntity> secondarySkills) {

        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        return new CandidateProfileEntity(
                id,
                contactEmail,
                "cv.pdf",
                "application/pdf",
                123L,
                false,
                "READY",
                "",
                "",
                now,
                firstName,
                lastName,
                "+46700000000",
                "Sweden",
                "Stockholm",
                workStatus,
                "Swedish, English",
                "",
                "Profile summary",
                "5",
                "",
                "",
                "",
                workMode,
                "Remote",
                "Stockholm",
                true,
                true,
                "READY",
                "Core competence",
                "Stockholm",
                "",
                now,
                now,
                now,
                List.of(),
                List.of(),
                roles,
                primarySkills,
                secondarySkills,
                List.of());
    }

    private static CandidateProfileRoleEntity role(Long roleId, String title, short years) {
        return new CandidateProfileRoleEntity(null, 1, roleId, title, years);
    }

    private static CandidateProfilePrimarySkillEntity primarySkill(Long skillId, String title, short levelId) {
        return new CandidateProfilePrimarySkillEntity(null, 1, skillId, title, levelId, "Advanced");
    }

    private static CandidateProfileSecondarySkillEntity secondarySkill(Long skillId, String title, short levelId) {
        return new CandidateProfileSecondarySkillEntity(null, 1, skillId, title, levelId, "Working");
    }
}
