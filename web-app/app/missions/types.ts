import type { SkillCategory } from "~/reference_data/types";

export interface MissionSkillRequirementInput {
  skillId: number;
  skillLevelId: number;
  skillCategory: SkillCategory;
}

export interface MissionSlotInput {
  roleId: number;
  requiredRoleExperienceYears: number;
  requiredSkills: MissionSkillRequirementInput[];
}

export interface MissionPresentationInput {
  oneDayAtWork: string;
  technicalLandscape: string;
  whoWeAreLookingFor: string;
  whatWeOffer: string;
  aboutCustomer: string;
  recruitmentProcess: string;
}

export interface MissionProposalInput {
  customerName: string;
  customerEmail: string;
  missionTitle: string;
  missionSlots: MissionSlotInput[];
  startDate: string;
  endDate: string;
  workMode: "ON_PREMISE" | "REMOTE" | "HYBRID";
  missionPresentation: MissionPresentationInput;
}

export interface MissionProposalPreviewInput {
  roleAndRequirementsText: string;
}

export interface MissionProposalPreviewEvidenceItem {
  field: string;
  value: string;
  sourceText: string;
  confidence?: number | null;
}

export interface MissionProposalPreviewView {
  proposalWorkingCopy: MissionProposalInput;
  evidence: MissionProposalPreviewEvidenceItem[];
  missingFields: string[];
}

export interface MissionProposalEditInput extends MissionProposalInput {
  outcome: string;
}

export interface MissionSkillRequirementView {
  skillId: number;
  skillTitle: string;
  skillLevelId: number;
  skillLevelName: string;
  skillCategory: SkillCategory;
}

export interface MissionSlotSpecificationView {
  slotNumber: number;
  roleId: number;
  roleTitle: string;
  requiredRoleExperienceYears: number;
  requiredSkills: MissionSkillRequirementView[];
}

export interface MissionSpecificationView {
  customerName: string;
  customerEmail: string;
  missionTitle: string;
  missionSlots: MissionSlotSpecificationView[];
  startDate: string;
  endDate: string;
  workMode: string;
  missionPresentation: MissionPresentationInput;
}

export interface FindCandResultView {
  missionSlotNumber: number;
  roleTitle: string;
  candProfileId: number;
  candName: string;
  score: number;
  scoreLabel: string;
  roleMatched: boolean;
  workModeMatched: boolean;
  matchedSkillCount: number;
  requiredSkillCount: number;
  matchedSkills: string[];
}

export interface MissionProposalView {
  id: number;
  status: string;
  specification: MissionSpecificationView;
  evidence?: MissionProposalPreviewEvidenceItem[];
  missingFields?: string[];
  findCandidateResults: FindCandResultView[];
  outcome: string;
  createdAt: string;
  updatedAt: string;
}

export interface RegisteredMissionView {
  id: number;
  status?: string | null;
  customerName?: string | null;
  specification: MissionSpecificationView;
  createdAt?: string | null;
  updatedAt?: string | null;
}
