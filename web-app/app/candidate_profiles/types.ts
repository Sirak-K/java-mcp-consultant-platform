import type { SkillCategory } from "~/reference_data/types";

export interface CandidateCompanyIdentityOptionView {
  organisationName: string;
  organisationNumber: string;
  organisationCity: string;
}

export interface CandidateWorkExperienceWorkingCopyInput {
  jobTitle: string;
  workExpCompany: string;
  workExpCompanyOrgNr: string;
  companyIdentityOptions?: CandidateCompanyIdentityOptionView[];
  city: string;
  country: string;
  startDate: string;
  endDate: string;
  currentlyHere: boolean;
}

export interface CandidateCertificationWorkingCopyInput {
  name: string;
  documentAttached: boolean;
  documentFileName: string;
  documentContentType: string;
  documentSizeBytes: number | null;
}

export interface CandidateEducationWorkingCopyInput {
  institution: string;
  fieldOfStudy: string;
  startDate: string;
  endDate: string;
  currentlyStudying: boolean;
}

export interface CandidateRoleWorkingCopyInput {
  roleId: number;
  roleTitle: string;
  roleExperienceYears: number;
}

export interface CandidateSkillWorkingCopyInput {
  skillId: number;
  skillTitle: string;
  skillCategory: SkillCategory;
  skillLevelId: number;
  skillLevelName: string;
}

export interface CandidateCvProfileWorkingCopyInput {
  contactEmail: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  country: string;
  city: string;
  workStatus: string;
  languages: string;
  roleTitle: string;
  profileSummary: string;
  yearsOfExperience: string;
  expectedSalary: string;
  hourlyRate: string;
  skills: string;
  candidateRoles: CandidateRoleWorkingCopyInput[];
  candidateSkills: CandidateSkillWorkingCopyInput[];
  workExperiences: CandidateWorkExperienceWorkingCopyInput[];
  workMode: string;
  locationFlexibility: string;
  preferredLocation: string;
  willingToRelocate: boolean;
  educations: CandidateEducationWorkingCopyInput[];
  certifications: CandidateCertificationWorkingCopyInput[];
  gdprConsent: boolean;
}

export interface CandidateApplicationInput {
  contactEmail: string;
  cvFileName: string;
  cvContentType: string;
  cvSizeBytes: number | null;
  profileWorkingCopy: CandidateCvProfileWorkingCopyInput;
  generatedSummary?: CandidateProfileSummaryInput;
}

export interface CandidateApplicationEditInput extends CandidateApplicationInput {
  outcome: string;
}

export interface RegisteredCandidateProfileCardSkillView {
  title: string;
  skillLevel: string;
}

export interface RegisteredCandidateProfileCardView {
  candidateProfileId: number;
  candidateName?: string | null;
  roleTitle?: string | null;
  roleExperienceLevel?: string | null;
  roleExperienceYears?: number | null;
  availabilityStatus?: string | null;
  country?: string | null;
  locationFlexibility?: string | null;
  workMode?: string | null;
  primarySkills: RegisteredCandidateProfileCardSkillView[];
  secondarySkills: RegisteredCandidateProfileCardSkillView[];
}

export interface CandidateCvExtractionView {
  status: "METADATA_ONLY" | "EXTRACTED" | "EMPTY_TEXT" | "UNSUPPORTED_CONTENT_TYPE" | "FAILED" | string;
  extractedTextPreview: string;
  error: string;
  extractedAt: string | null;
}

export interface CandidateWorkExperienceWorkingCopyView {
  jobTitle: string;
  workExpCompany: string;
  workExpCompanyOrgNr: string;
  companyIdentityOptions: CandidateCompanyIdentityOptionView[];
  city: string;
  country: string;
  startDate: string;
  endDate: string;
  currentlyHere: boolean;
}

export interface CandidateCertificationWorkingCopyView {
  name: string;
  documentAttached: boolean;
  documentFileName: string;
  documentContentType: string;
  documentSizeBytes: number | null;
}

export interface CandidateEducationWorkingCopyView {
  institution: string;
  fieldOfStudy: string;
  startDate: string;
  endDate: string;
  currentlyStudying: boolean;
}

export interface CandidateRoleWorkingCopyView {
  roleId: number;
  roleTitle: string;
  roleExperienceYears: number;
}

export interface CandidateSkillWorkingCopyView {
  skillId: number;
  skillTitle: string;
  skillCategory: SkillCategory;
  skillLevelId: number;
  skillLevelName: string;
}

export interface CandidateCvProfileWorkingCopyView {
  contactEmail: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  country: string;
  city: string;
  workStatus: string;
  languages: string;
  roleTitle: string;
  profileSummary: string;
  yearsOfExperience: string;
  expectedSalary: string;
  hourlyRate: string;
  skills: string;
  candidateRoles: CandidateRoleWorkingCopyView[];
  candidateSkills: CandidateSkillWorkingCopyView[];
  workExperiences: CandidateWorkExperienceWorkingCopyView[];
  workMode: string;
  locationFlexibility: string;
  preferredLocation: string;
  willingToRelocate: boolean;
  educations: CandidateEducationWorkingCopyView[];
  certifications: CandidateCertificationWorkingCopyView[];
  gdprConsent: boolean;
}

export interface CandidateCvPreviewView {
  cvExtraction: CandidateCvExtractionView;
  profileWorkingCopy: CandidateCvProfileWorkingCopyView;
}

export interface CandidateProfileSummaryInput {
  coreCompetenceOverview: string;
  location: string;
  otherDetails: string;
}

export interface CandidateProfileSummaryView {
  status: "NOT_GENERATED" | "GENERATED" | "STALE" | string;
  coreCompetenceOverview: string;
  location: string;
  otherDetails: string;
  generatedAt: string | null;
}

export interface CandidateProfileSpecificationView {
  contactEmail: string;
  cvFileName: string;
  cvContentType: string;
  cvSizeBytes: number | null;
  cvExtractionPending: boolean;
  profileWorkingCopy: CandidateCvProfileWorkingCopyView;
}

export interface FindMissionResultView {
  missionSlotId: number;
  missionTitle: string;
  missionSlotNumber: number;
  roleTitle: string;
  requiredRoleExperienceYears: number;
  score: number;
  scoreLabel: string;
  roleMatched: boolean;
  workModeMatched: boolean;
  matchedSkillCount: number;
  requiredSkillCount: number;
  matchedSkills: string[];
  readiness: string;
}

export interface CandidateApplicationView {
  id: number;
  specification: CandidateProfileSpecificationView;
  cvExtraction: CandidateCvExtractionView;
  generatedSummary: CandidateProfileSummaryView;
  findMissionResults: FindMissionResultView[];
  outcome: string;
  createdAt: string;
  updatedAt: string;
}

export interface RegisteredCandidateProfileView extends Omit<CandidateApplicationView, "id"> {
  candidateProfileId: number;
}

export type CandidateProfileReviewItem =
  | CandidateApplicationView
  | RegisteredCandidateProfileView;
