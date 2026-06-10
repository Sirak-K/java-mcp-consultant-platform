import {
  hasCertificationData,
  hasEducationData,
  hasWorkExperienceData,
  type CandidateCertificationFormFields,
  type CandidateCvProfileFormFields,
  type CandidateEducationFormFields,
  type CandidateRoleFormFields,
  type CandidateSkillFormFields,
  type CandidateSummaryFormFields,
  type CandidateWorkExperienceFormFields,
} from "~/candidate_profiles/intake/candidateApplicationIntakeState";
import { formatCompetencyLevelLabel } from "~/reference_data/competencyLevels";
import type { MarketplaceReferenceData } from "~/reference_data/types";
import { formatWorkMode } from "~/reference_data/workMode";

type CandidateApplicationSummaryInput = {
  cvProfile: CandidateCvProfileFormFields;
  candidateRoles: CandidateRoleFormFields[];
  candidateSkills: CandidateSkillFormFields[];
  workExperiences: CandidateWorkExperienceFormFields[];
  educations: CandidateEducationFormFields[];
  certifications: CandidateCertificationFormFields[];
  referenceData: MarketplaceReferenceData | null;
};

export function stripCandidateCompetencyYearThresholds(value: string) {
  return value.replace(
    /\s*\(\s*(?:>=\s*\d+\s*YRS?|Mer än\s*\d+\s*år)\s*\)/gi,
    "",
  );
}

export function hasCandidateApplicationSummarySourceData({
  cvProfile,
  candidateRoles,
  candidateSkills,
  workExperiences,
  educations,
  certifications,
}: CandidateApplicationSummaryInput) {
  return (
    [cvProfile.profileSummary, cvProfile.city, cvProfile.country].some(
      (value) => value.trim().length > 0,
    ) ||
    candidateRoles.length > 0 ||
    candidateSkills.length > 0 ||
    workExperiences.some(hasWorkExperienceData) ||
    educations.some(hasEducationData) ||
    certifications.some(hasCertificationData)
  );
}

export function buildCandidateApplicationGeneratedSummary({
  cvProfile,
  candidateRoles,
  candidateSkills,
  certifications,
  referenceData,
}: CandidateApplicationSummaryInput): CandidateSummaryFormFields {
  const roles = candidateRoles
    .map((role) => displayRoleTitle(referenceData, role.roleId))
    .filter(Boolean);
  const skills = candidateSkills
    .map((skill) => {
      const title = displaySkillTitle(
        referenceData,
        skill.skillId,
        skill.skillCategory,
      );
      const level = displaySummarySkillLevel(referenceData, skill.skillLevelId);
      return title && level ? `${title} (${level})` : title;
    })
    .filter(Boolean);

  return {
    coreCompetenceOverview: [
      roles.length > 0 ? `Roles: ${roles.join(", ")}.` : "",
      skills.length > 0 ? `Skills: ${skills.join(", ")}.` : "",
      certificationSummaryText(certifications),
    ]
      .filter(Boolean)
      .join(" "),
    location: [
      [cvProfile.city.trim(), cvProfile.country.trim()]
        .filter(Boolean)
        .join(", "),
      cvProfile.locationFlexibility.trim()
        ? `Flexibility: ${cvProfile.locationFlexibility.trim()}.`
        : "",
      cvProfile.workMode.trim()
        ? `Arbetsläge: ${formatWorkMode(cvProfile.workMode.trim())}.`
        : "",
    ]
      .filter(Boolean)
      .join(" "),
    otherDetails: [
      cvProfile.profileSummary.trim(),
      cvProfile.expectedSalary.trim()
        ? `Expected salary: ${cvProfile.expectedSalary.trim()}.`
        : "",
      cvProfile.hourlyRate.trim()
        ? `Hourly rate: ${cvProfile.hourlyRate.trim()}.`
        : "",
    ]
      .filter(Boolean)
      .join(" "),
  };
}

function displayRoleTitle(
  referenceData: MarketplaceReferenceData | null,
  roleId: number,
) {
  return referenceData?.roles.find((role) => role.id === roleId)?.title ?? "";
}

function displaySkillTitle(
  referenceData: MarketplaceReferenceData | null,
  skillId: number,
  skillCategory: "PRIMARY" | "SECONDARY",
) {
  return (
    referenceData?.skills.find(
      (skill) => skill.id === skillId && skill.category === skillCategory,
    )?.title ?? ""
  );
}

function displaySkillLevel(
  referenceData: MarketplaceReferenceData | null,
  skillLevelId: number,
) {
  const levelName = referenceData?.skillLevels.find(
    (level) => level.id === skillLevelId,
  )?.name;
  return formatCompetencyLevelLabel(levelName);
}

function displaySummarySkillLevel(
  referenceData: MarketplaceReferenceData | null,
  skillLevelId: number,
) {
  return stripCandidateCompetencyYearThresholds(
    displaySkillLevel(referenceData, skillLevelId),
  ).trim();
}

function certificationSummaryText(
  certifications: CandidateCertificationFormFields[],
) {
  const activeCertifications = certifications.filter(hasCertificationData);
  if (activeCertifications.length === 0) {
    return "";
  }

  const certificationNames = activeCertifications
    .map(
      (certification) =>
        certification.name.trim() || certification.documentFileName.trim(),
    )
    .filter(Boolean);

  return certificationNames.length > 0
    ? `Certifications: ${certificationNames.join(", ")}.`
    : `Certifications: ${activeCertifications.length}.`;
}
