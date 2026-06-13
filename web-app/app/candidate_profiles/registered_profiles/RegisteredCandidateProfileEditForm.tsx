import { RegisteredCandidateProfileBasicFields } from "~/candidate_profiles/registered_profiles/RegisteredCandidateProfileBasicFields";
import { RegisteredCandidateProfileEducationSection } from "~/candidate_profiles/registered_profiles/RegisteredCandidateProfileEducationSection";
import { RegisteredCandidateProfileRoleSection } from "~/candidate_profiles/registered_profiles/RegisteredCandidateProfileRoleSection";
import { RegisteredCandidateProfileSkillSection } from "~/candidate_profiles/registered_profiles/RegisteredCandidateProfileSkillSection";
import { RegisteredCandidateProfileWorkExperienceSection } from "~/candidate_profiles/registered_profiles/RegisteredCandidateProfileWorkExperienceSection";
import {
  type CandidateApplicationEditWorkingCopy,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import type {
  CandidateRoleWorkingCopyInput,
  CandidateSkillWorkingCopyInput,
  CandidateEducationWorkingCopyInput,
  CandidateWorkExperienceWorkingCopyInput,
  CandidateCvProfileWorkingCopyInput,
} from "~/candidate_profiles/types";
import type { ReferenceData } from "~/reference_data/types";

type RegisteredCandidateProfileEditFormProps = {
  editWorkingCopy: unknown;
  updateEditWorkingCopy: (updater: (current: unknown) => unknown) => void;
  referenceData: ReferenceData | null;
};

export function RegisteredCandidateProfileEditForm({
  editWorkingCopy,
  updateEditWorkingCopy,
  referenceData,
}: RegisteredCandidateProfileEditFormProps) {
  const workingCopy =
    editWorkingCopy as CandidateApplicationEditWorkingCopy;
  const profileWorkingCopy = workingCopy.profileWorkingCopy;
  const canAddRole = Boolean(referenceData?.roles.length);
  const canAddSkill = Boolean(
    referenceData?.skills.length &&
    referenceData.skillLevels.length,
  );
  const updateWorkingCopy = (
    updater: (
      current: CandidateApplicationEditWorkingCopy,
    ) => CandidateApplicationEditWorkingCopy,
  ) =>
    updateEditWorkingCopy((current) =>
      updater(
        (current ??
          workingCopy) as CandidateApplicationEditWorkingCopy,
      ),
    );
  const updateProfileWorkingCopy = (
    updater: (
      current: CandidateCvProfileWorkingCopyInput,
    ) => CandidateCvProfileWorkingCopyInput,
  ) =>
    updateWorkingCopy((current) => ({
      ...current,
      profileWorkingCopy: updater(current.profileWorkingCopy),
    }));
  const updateCandidateRole = (
    roleIndex: number,
    updater: (
      current: CandidateRoleWorkingCopyInput,
    ) => CandidateRoleWorkingCopyInput,
  ) =>
    updateProfileWorkingCopy((current) => ({
      ...current,
      candidateRoles: current.candidateRoles.map((role, index) =>
        index === roleIndex ? updater(role) : role,
      ),
    }));
  const updateCandidateSkill = (
    skillIndex: number,
    updater: (
      current: CandidateSkillWorkingCopyInput,
    ) => CandidateSkillWorkingCopyInput,
  ) =>
    updateProfileWorkingCopy((current) => ({
      ...current,
      candidateSkills: current.candidateSkills.map(
        (skill, index) =>
          index === skillIndex ? updater(skill) : skill,
      ),
    }));
  const updateWorkExperience = (
    workExperienceIndex: number,
    updater: (
      current: CandidateWorkExperienceWorkingCopyInput,
    ) => CandidateWorkExperienceWorkingCopyInput,
  ) =>
    updateProfileWorkingCopy((current) => ({
      ...current,
      workExperiences: current.workExperiences.map(
        (workExperience, index) =>
          index === workExperienceIndex
            ? updater(workExperience)
            : workExperience,
      ),
    }));
  const updateEducation = (
    educationIndex: number,
    updater: (
      current: CandidateEducationWorkingCopyInput,
    ) => CandidateEducationWorkingCopyInput,
  ) =>
    updateProfileWorkingCopy((current) => ({
      ...current,
      educations: current.educations.map((education, index) =>
        index === educationIndex ? updater(education) : education,
      ),
    }));

  return (
    <section className="panel-soft rounded p-4 text-sm">
      <div className="mb-4">
        <h3 className="text-section font-semibold">
          Redigera kandidatunderlag
        </h3>
        <p className="text-soft mt-1 text-xs">
          Ändringar sparas först när Spara ändringar klickas.
          Matchresultat räknas om efter sparning.
        </p>
      </div>

      <RegisteredCandidateProfileBasicFields
        workingCopy={workingCopy}
        profileWorkingCopy={profileWorkingCopy}
        updateWorkingCopy={updateWorkingCopy}
        updateProfileWorkingCopy={updateProfileWorkingCopy}
      />

      <RegisteredCandidateProfileWorkExperienceSection
        profileWorkingCopy={profileWorkingCopy}
        updateProfileWorkingCopy={updateProfileWorkingCopy}
        updateWorkExperience={updateWorkExperience}
      />

      <RegisteredCandidateProfileEducationSection
        profileWorkingCopy={profileWorkingCopy}
        updateProfileWorkingCopy={updateProfileWorkingCopy}
        updateEducation={updateEducation}
      />

      <RegisteredCandidateProfileRoleSection
        profileWorkingCopy={profileWorkingCopy}
        updateProfileWorkingCopy={updateProfileWorkingCopy}
        updateCandidateRole={updateCandidateRole}
        canAddRole={canAddRole}
        referenceData={referenceData}
      />

      <RegisteredCandidateProfileSkillSection
        profileWorkingCopy={profileWorkingCopy}
        updateProfileWorkingCopy={updateProfileWorkingCopy}
        updateCandidateSkill={updateCandidateSkill}
        canAddSkill={canAddSkill}
        referenceData={referenceData}
      />
    </section>
  );
}
