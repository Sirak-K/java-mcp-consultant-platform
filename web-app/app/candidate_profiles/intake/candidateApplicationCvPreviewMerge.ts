import {
  hasCertificationData,
  hasEducationData,
  hasWorkExperienceData,
  joinLabelValues,
  mergeText,
  normalizeLocationFlexibility,
  parseLabelValues,
  type CandidateCertificationFormFields,
  type CandidateCvProfileFormFields,
  type CandidateEducationFormFields,
  type CandidateRoleFormFields,
  type CandidateSkillFormFields,
  type CandidateWorkExperienceFormFields,
} from "~/candidate_profiles/intake/candidateApplicationIntakeState";
import type { CandidateCvPreviewView } from "~/candidate_profiles/types";

type CandidateCvPreviewWorkingCopy =
  CandidateCvPreviewView["profileWorkingCopy"];

export function mergeCandidateCvProfile(
  current: CandidateCvProfileFormFields,
  preview: CandidateCvPreviewWorkingCopy,
): CandidateCvProfileFormFields {
  return {
    firstName: mergeText(current.firstName, preview.firstName),
    lastName: mergeText(current.lastName, preview.lastName),
    phoneNumber: mergeText(current.phoneNumber, preview.phoneNumber),
    country: mergeText(current.country, preview.country),
    city: mergeText(current.city, preview.city),
    languages: current.languages.trim()
      ? current.languages
      : joinLabelValues(parseLabelValues(preview.languages)),
    profileSummary: mergeText(current.profileSummary, preview.profileSummary),
    expectedSalary: mergeText(current.expectedSalary, preview.expectedSalary),
    hourlyRate: mergeText(current.hourlyRate, preview.hourlyRate),
    workMode: preview.workMode || current.workMode || "REMOTE",
    locationFlexibility: mergeText(
      current.locationFlexibility,
      normalizeLocationFlexibility(preview.locationFlexibility),
    ),
    willingToRelocate: current.willingToRelocate || preview.willingToRelocate,
    gdprConsent: current.gdprConsent || preview.gdprConsent,
  };
}

export function mergeCandidateRoles(
  current: CandidateRoleFormFields[],
  preview: CandidateCvPreviewWorkingCopy,
): CandidateRoleFormFields[] {
  if (current.length > 0) {
    return current;
  }
  return (preview.candidateRoles ?? []).map((role) => ({
    roleId: role.roleId,
    roleExperienceYears: String(role.roleExperienceYears ?? 0),
  }));
}

export function mergeCandidateSkills(
  current: CandidateSkillFormFields[],
  preview: CandidateCvPreviewWorkingCopy,
): CandidateSkillFormFields[] {
  if (current.length > 0) {
    return current;
  }
  return (preview.candidateSkills ?? []).map((skill) => ({
    skillId: skill.skillId,
    skillCategory: skill.skillCategory,
    skillLevelId: skill.skillLevelId,
  }));
}

export function mergeWorkExperiences(
  current: CandidateWorkExperienceFormFields[],
  previewItems: CandidateCvPreviewWorkingCopy["workExperiences"],
): CandidateWorkExperienceFormFields[] {
  const currentHasData = current.some(hasWorkExperienceData);
  if (currentHasData || previewItems.length === 0) {
    return current;
  }
  return previewItems.map((item) => ({
    jobTitle: item.jobTitle,
    workExpCompany: item.workExpCompany,
    workExpCompanyOrgNr: item.workExpCompanyOrgNr,
    companyIdentityOptions: item.companyIdentityOptions ?? [],
    city: item.city,
    country: item.country,
    startDate: item.startDate,
    endDate: item.currentlyHere ? "" : item.endDate,
    currentlyHere: item.currentlyHere,
  }));
}

export function mergeEducations(
  current: CandidateEducationFormFields[],
  previewItems: CandidateCvPreviewWorkingCopy["educations"],
): CandidateEducationFormFields[] {
  const currentHasData = current.some(hasEducationData);
  if (currentHasData || previewItems.length === 0) {
    return current;
  }
  return previewItems.map((item) => ({
    institution: item.institution,
    fieldOfStudy: item.fieldOfStudy,
    startDate: item.startDate,
    endDate: item.currentlyStudying ? "" : item.endDate,
    currentlyStudying: item.currentlyStudying,
  }));
}

export function mergeCertifications(
  current: CandidateCertificationFormFields[],
  previewItems: CandidateCvPreviewWorkingCopy["certifications"],
): CandidateCertificationFormFields[] {
  const currentHasData = current.some(hasCertificationData);
  if (currentHasData || previewItems.length === 0) {
    return current;
  }
  return previewItems.map((item) => ({
    name: item.name,
    documentAttached: item.documentAttached,
    documentFileName: item.documentFileName,
    documentContentType: item.documentContentType,
    documentSizeBytes: item.documentSizeBytes,
  }));
}

export function mergeCertificationFiles(
  current: Array<File | null>,
  previewItems: CandidateCvPreviewWorkingCopy["certifications"],
) {
  const currentHasFile = current.some((file) => file !== null);
  if (currentHasFile || previewItems.length === 0) {
    return current;
  }
  return previewItems.map(() => null);
}
