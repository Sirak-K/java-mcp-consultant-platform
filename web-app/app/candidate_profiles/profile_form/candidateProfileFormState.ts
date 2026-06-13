import type {
  CandidateApplicationEditInput,
  CandidateCvProfileWorkingCopyInput,
  CandidateEducationWorkingCopyInput,
  CandidateProfileReviewItem,
  CandidateRoleWorkingCopyInput,
  CandidateSkillWorkingCopyInput,
  CandidateWorkExperienceWorkingCopyInput,
} from "~/candidate_profiles/types";
import type { ReferenceData } from "~/reference_data/types";

export type CandidateApplicationEditWorkingCopy = Omit<
  CandidateApplicationEditInput,
  "outcome"
>;

export const MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS = 100;

export function normalizeCandidateRoleExperienceYears(value: string): number {
  const digits = value.replace(/\D/g, "").slice(0, 3);
  if (!digits) {
    return 0;
  }
  return Math.min(Number(digits), MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS);
}

export function endDateHasPassed(value: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) {
    return false;
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const endDate = new Date(year, month - 1, day);
  if (
    endDate.getFullYear() !== year ||
    endDate.getMonth() !== month - 1 ||
    endDate.getDate() !== day
  ) {
    return false;
  }

  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return endDate.getTime() < today.getTime();
}

export function currentlyStudyingAvailable(
  item: Pick<CandidateEducationWorkingCopyInput, "endDate">,
): boolean {
  return !endDateHasPassed(item.endDate);
}

export function currentlyWorkingHereAvailable(
  item: Pick<CandidateWorkExperienceWorkingCopyInput, "endDate">,
): boolean {
  return !endDateHasPassed(item.endDate);
}

export function defaultCandidateRoleWorkingCopy(
  referenceData: ReferenceData | null,
): CandidateRoleWorkingCopyInput {
  const role = referenceData?.roles[0];
  return {
    roleId: role?.id ?? 0,
    roleTitle: role?.title ?? "",
    roleExperienceYears: 0,
  };
}

export function defaultCandidateSkillWorkingCopy(
  referenceData: ReferenceData | null,
): CandidateSkillWorkingCopyInput {
  const skill = referenceData?.skills[0];
  const level = referenceData?.skillLevels[0];
  return {
    skillId: skill?.id ?? 0,
    skillTitle: skill?.title ?? "",
    skillCategory: skill?.category ?? "PRIMARY",
    skillLevelId: level?.id ?? 0,
    skillLevelName: level?.name ?? "",
  };
}

export function defaultCandidateWorkExperienceWorkingCopy(): CandidateWorkExperienceWorkingCopyInput {
  return {
    jobTitle: "",
    workExpCompany: "",
    workExpCompanyOrgNr: "",
    companyIdentityOptions: [],
    city: "",
    country: "",
    startDate: "",
    endDate: "",
    currentlyHere: false,
  };
}

export function defaultCandidateEducationWorkingCopy(): CandidateEducationWorkingCopyInput {
  return {
    institution: "",
    fieldOfStudy: "",
    startDate: "",
    endDate: "",
    currentlyStudying: false,
  };
}

export function parseCandidateSkillSelection(value: string): {
  skillCategory: "PRIMARY" | "SECONDARY";
  skillId: number;
} {
  const [skillCategory, rawSkillId] = value.split(":");
  return {
    skillCategory: skillCategory === "SECONDARY" ? "SECONDARY" : "PRIMARY",
    skillId: Number(rawSkillId),
  };
}

export function normalizeCandidateProfileWorkingCopyForEdit(
  item: CandidateProfileReviewItem,
): CandidateCvProfileWorkingCopyInput {
  const profileWorkingCopy = item.specification.profileWorkingCopy;
  return {
    ...profileWorkingCopy,
    candidateRoles: profileWorkingCopy.candidateRoles ?? [],
    candidateSkills: profileWorkingCopy.candidateSkills ?? [],
    workExperiences: (profileWorkingCopy.workExperiences ?? []).map(
      (workExperience) => ({
        ...workExperience,
        workExpCompany: workExperience.workExpCompany ?? "",
        workExpCompanyOrgNr: workExperience.workExpCompanyOrgNr ?? "",
        companyIdentityOptions: workExperience.companyIdentityOptions ?? [],
      }),
    ),
    educations: profileWorkingCopy.educations ?? [],
    workMode: profileWorkingCopy.workMode || "",
  };
}

export function normalizeCandidateProfileWorkingCopyForSave(
  profileWorkingCopy: CandidateCvProfileWorkingCopyInput,
): CandidateCvProfileWorkingCopyInput {
  return {
    ...profileWorkingCopy,
    roleTitle: "",
    skills: "",
    workExperiences: (profileWorkingCopy.workExperiences ?? []).map(
      (workExperience) => ({
        ...workExperience,
        companyIdentityOptions: [],
      }),
    ),
    educations: (profileWorkingCopy.educations ?? []).map((education) => ({
      ...education,
      currentlyStudying:
        education.currentlyStudying && currentlyStudyingAvailable(education),
    })),
    workMode: profileWorkingCopy.workMode || "",
  };
}

export function buildCandidateApplicationEditWorkingCopy(
  item: CandidateProfileReviewItem,
): CandidateApplicationEditWorkingCopy {
  return {
    contactEmail: item.specification.contactEmail,
    cvFileName: item.specification.cvFileName,
    cvContentType: item.specification.cvContentType,
    cvSizeBytes: item.specification.cvSizeBytes,
    profileWorkingCopy: normalizeCandidateProfileWorkingCopyForEdit(item),
  };
}

export function buildCandidateApplicationEditInput(
  item: CandidateProfileReviewItem,
  outcome: string,
  editWorkingCopy: unknown,
): CandidateApplicationEditInput {
  const workingCopy = (editWorkingCopy ??
    buildCandidateApplicationEditWorkingCopy(
      item,
    )) as CandidateApplicationEditWorkingCopy;
  return {
    ...workingCopy,
    profileWorkingCopy: normalizeCandidateProfileWorkingCopyForSave(
      workingCopy.profileWorkingCopy,
    ),
    outcome,
  };
}
