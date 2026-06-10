import type {
  CandidateCompanyIdentityOptionView,
} from "~/candidate_profiles/types";
import { MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS } from "~/candidate_profiles/profile_form/candidateProfileFormState";
import type { MarketplaceReferenceData } from "~/reference_data/types";

export function hasSkillReferenceOptions(
  data: MarketplaceReferenceData | null,
) {
  return Boolean(data?.skills.length && data.skillLevels.length);
}

export type CandidateApplicationFormFields = {
  contactEmail: string;
  cvFileName: string;
  cvContentType: string;
  cvSizeBytes: number | null;
};

export type CandidateWorkExperienceFormFields = {
  jobTitle: string;
  workExpCompany: string;
  workExpCompanyOrgNr: string;
  companyIdentityOptions: CandidateCompanyIdentityOptionView[];
  city: string;
  country: string;
  startDate: string;
  endDate: string;
  currentlyHere: boolean;
};

export type CandidateCertificationFormFields = {
  name: string;
  documentAttached: boolean;
  documentFileName: string;
  documentContentType: string;
  documentSizeBytes: number | null;
};

export type CandidateEducationFormFields = {
  institution: string;
  fieldOfStudy: string;
  startDate: string;
  endDate: string;
  currentlyStudying: boolean;
};

export type CandidateRoleFormFields = {
  roleId: number;
  roleExperienceYears: string;
};

export type CandidateSkillFormFields = {
  skillId: number;
  skillCategory: "PRIMARY" | "SECONDARY";
  skillLevelId: number;
};

export type CandidateCvProfileFormFields = {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  country: string;
  city: string;
  languages: string;
  profileSummary: string;
  expectedSalary: string;
  hourlyRate: string;
  workMode: string;
  locationFlexibility: string;
  willingToRelocate: boolean;
  gdprConsent: boolean;
};

export type CandidateSummaryFormFields = {
  coreCompetenceOverview: string;
  location: string;
  otherDetails: string;
};

export const initialCandidateApplicationForm: CandidateApplicationFormFields = {
  contactEmail: "",
  cvFileName: "",
  cvContentType: "application/octet-stream",
  cvSizeBytes: null,
};

export const blankCandidateWorkExperience: CandidateWorkExperienceFormFields = {
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

export const blankCandidateCertification: CandidateCertificationFormFields = {
  name: "",
  documentAttached: false,
  documentFileName: "",
  documentContentType: "",
  documentSizeBytes: null,
};

export const blankCandidateEducation: CandidateEducationFormFields = {
  institution: "",
  fieldOfStudy: "",
  startDate: "",
  endDate: "",
  currentlyStudying: false,
};

export const initialCandidateCvProfile: CandidateCvProfileFormFields = {
  firstName: "",
  lastName: "",
  phoneNumber: "",
  country: "",
  city: "",
  languages: "",
  profileSummary: "",
  expectedSalary: "",
  hourlyRate: "",
  workMode: "REMOTE",
  locationFlexibility: "",
  willingToRelocate: false,
  gdprConsent: false,
};

export const initialCandidateSummary: CandidateSummaryFormFields = {
  coreCompetenceOverview: "",
  location: "",
  otherDetails: "",
};

export const LOCATION_FLEXIBILITY_OPTIONS = [
  "Specific City",
  "Country-wide",
  "Region-wide",
  "Global",
] as const;

export function parseLabelValues(value: string): string[] {
  const uniqueValues = new Set<string>();
  const labels: string[] = [];

  for (const item of value.split(/[,\n;\u2022]+/)) {
    const trimmed = item.trim();
    if (!trimmed) {
      continue;
    }
    const normalized = trimmed.toLocaleLowerCase();
    if (uniqueValues.has(normalized)) {
      continue;
    }
    uniqueValues.add(normalized);
    labels.push(trimmed);
  }

  return labels;
}

export function joinLabelValues(values: string[]): string {
  return parseLabelValues(values.join(", ")).join(", ");
}

export function mergeText(currentValue: string, previewValue: string) {
  return currentValue.trim() ? currentValue : previewValue;
}

export function hasWorkExperienceData(item: CandidateWorkExperienceFormFields) {
  return (
    item.currentlyHere ||
    item.jobTitle.trim().length > 0 ||
    item.workExpCompany.trim().length > 0 ||
    item.workExpCompanyOrgNr.trim().length > 0 ||
    item.city.trim().length > 0 ||
    item.country.trim().length > 0 ||
    item.startDate.trim().length > 0 ||
    item.endDate.trim().length > 0
  );
}

export function hasCertificationData(item: CandidateCertificationFormFields) {
  return (
    item.documentAttached ||
    item.name.trim().length > 0 ||
    item.documentFileName.trim().length > 0 ||
    item.documentContentType.trim().length > 0 ||
    item.documentSizeBytes !== null
  );
}

export function hasEducationData(item: CandidateEducationFormFields) {
  return (
    item.currentlyStudying ||
    item.institution.trim().length > 0 ||
    item.fieldOfStudy.trim().length > 0 ||
    item.startDate.trim().length > 0 ||
    item.endDate.trim().length > 0
  );
}

export function normalizeLocationFlexibility(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) {
    return "";
  }

  const normalized = trimmed.toLocaleLowerCase();
  if (normalized.includes("global")) {
    return "Global";
  }
  if (normalized.includes("region")) {
    return "Region-wide";
  }
  if (normalized.includes("country")) {
    return "Country-wide";
  }
  if (normalized.includes("city")) {
    return "Specific City";
  }
  return trimmed;
}

export function normalizeCandidateRoleExperienceYearsText(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 3);
  if (!digits) {
    return "";
  }
  return String(Math.min(Number(digits), MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS));
}

export function defaultCandidateRoleFormFields(
  referenceData: MarketplaceReferenceData | null,
): CandidateRoleFormFields {
  return {
    roleId: referenceData?.roles[0]?.id ?? 0,
    roleExperienceYears: "0",
  };
}

export function defaultCandidateSkillFormFields(
  referenceData: MarketplaceReferenceData | null,
): CandidateSkillFormFields {
  const skill = referenceData?.skills[0];
  return {
    skillId: skill?.id ?? 0,
    skillCategory: skill?.category ?? "PRIMARY",
    skillLevelId: referenceData?.skillLevels[0]?.id ?? 0,
  };
}
