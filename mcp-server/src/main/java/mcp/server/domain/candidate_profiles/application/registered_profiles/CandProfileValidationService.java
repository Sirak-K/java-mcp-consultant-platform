package mcp.server.domain.candidate_profiles.application.registered_profiles;

import org.springframework.stereotype.Service;

import mcp.server.domain.candidate_profiles.web.CandidateCvWebContract;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.requireText;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.requireWorkMode;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Service
public class CandProfileValidationService {

  private static final int MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS = 100;
  public void requireCandidateRegistrationCore(
      CandidateCvWebContract.CandidateCvProfileWorkingCopyView profileWorkingCopy) {
    if (profileWorkingCopy == null) {
      throw reject("profileWorkingCopy is required");
    }
    requireText(profileWorkingCopy.firstName(), "firstName is required");
    requireText(profileWorkingCopy.lastName(), "lastName is required");
    requireWorkMode(workMode(profileWorkingCopy));
    if (profileWorkingCopy.candidateRoles() == null || profileWorkingCopy.candidateRoles().isEmpty()) {
      throw reject("at least one structured candidate role is required");
    }
    if (profileWorkingCopy.candidateSkills() == null || profileWorkingCopy.candidateSkills().isEmpty()) {
      throw reject("at least one structured candidate skill with level is required");
    }
    for (CandidateCvWebContract.CandidateRoleWorkingCopyView role : profileWorkingCopy.candidateRoles()) {
      requireCandRole(role);
    }
    for (CandidateCvWebContract.CandidateSkillWorkingCopyView skill : profileWorkingCopy.candidateSkills()) {
      requireCandSkill(skill);
    }
  }

  private String workMode(CandidateCvWebContract.CandidateCvProfileWorkingCopyView profileWorkingCopy) {
    return safeText(profileWorkingCopy.workMode()).trim();
  }

  private void requireCandRole(CandidateCvWebContract.CandidateRoleWorkingCopyView role) {
    if (role == null) {
      throw reject("candidateRoles must not contain null entries");
    }
    if (role.roleId() <= 0) {
      throw reject("candidateRoles.roleId is required");
    }
    requireText(role.roleTitle(), "candidateRoles.roleTitle is required");
    if (role.roleExperienceYears() < 0) {
      throw reject("candidateRoles.roleExperienceYears must be zero or positive");
    }
    if (role.roleExperienceYears() > MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS) {
      throw reject("candidateRoles.roleExperienceYears must be 100 or less");
    }
  }

  private void requireCandSkill(CandidateCvWebContract.CandidateSkillWorkingCopyView skill) {
    if (skill == null) {
      throw reject("candidateSkills must not contain null entries");
    }
    if (skill.skillId() <= 0) {
      throw reject("candidateSkills.skillId is required");
    }
    requireText(skill.skillTitle(), "candidateSkills.skillTitle is required");
    if (skill.skillLevelId() <= 0) {
      throw reject("candidateSkills.skillLevelId is required");
    }
    requireText(skill.skillLevelName(), "candidateSkills.skillLevelName is required");
  }
}
