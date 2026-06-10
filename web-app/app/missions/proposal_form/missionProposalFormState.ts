import { limitWords, normalizeDateInput } from "~/shared/formHelpers";
import {
  ROLE_EXPERIENCE_OPTIONS,
  normalizeRoleExperienceYears,
} from "~/reference_data/roleExperience";
import {
  MISSION_PRESENTATION_MAX_WORDS,
  defaultMissionPresentation,
} from "../mission_presentation/missionPresentationFields";
import type {
  MissionPresentationInput,
  MissionProposalEditInput,
  MissionProposalInput,
  MissionProposalView,
  MissionSlotInput,
  MissionSkillRequirementInput,
} from "~/missions/types";
import type { MarketplaceReferenceData } from "~/reference_data/types";
import { workModeForEdit } from "~/reference_data/workMode";

export type MissionProposalEditWorkingCopy = Omit<
  MissionProposalEditInput,
  "outcome"
>;

export function defaultMissionSkillRequirement(
  referenceData: MarketplaceReferenceData | null,
): MissionSkillRequirementInput {
  const skill = referenceData?.skills[0];
  return {
    skillId: skill?.id ?? 0,
    skillLevelId: referenceData?.skillLevels[0]?.id ?? 0,
    skillCategory: skill?.category ?? "PRIMARY",
  };
}

export function parseMissionSkillSelection(value: string): Pick<
  MissionSkillRequirementInput,
  "skillCategory" | "skillId"
> {
  const [rawCategory, rawSkillId] = value.split(":");
  return {
    skillCategory: rawCategory === "SECONDARY" ? "SECONDARY" : "PRIMARY",
    skillId: Number(rawSkillId),
  };
}

export function defaultMissionSlot(
  referenceData: MarketplaceReferenceData | null,
): MissionSlotInput {
  return {
    roleId: referenceData?.roles[0]?.id ?? 0,
    requiredRoleExperienceYears: ROLE_EXPERIENCE_OPTIONS[0].years,
    requiredSkills: [defaultMissionSkillRequirement(referenceData)],
  };
}

export function defaultMissionProposalInput(
  referenceData: MarketplaceReferenceData | null,
): MissionProposalInput {
  return {
    customerName: "",
    customerEmail: "",
    missionTitle: "",
    missionSlots: [defaultMissionSlot(referenceData)],
    startDate: "",
    endDate: "",
    workMode: "REMOTE",
    missionPresentation: defaultMissionPresentation(),
  };
}

export function missionPresentationWithLimitedField(
  current: MissionPresentationInput | null | undefined,
  field: keyof MissionPresentationInput,
  value: string,
): MissionPresentationInput {
  return {
    ...(current ?? defaultMissionPresentation()),
    [field]: limitWords(value, MISSION_PRESENTATION_MAX_WORDS),
  };
}

function normalizePreviewSkill(
  skill: MissionSkillRequirementInput | undefined,
  referenceData: MarketplaceReferenceData | null,
): MissionSkillRequirementInput {
  const fallbackSkill = defaultMissionSkillRequirement(referenceData);
  return {
    skillId: skill && skill.skillId > 0 ? skill.skillId : fallbackSkill.skillId,
    skillLevelId:
      skill && skill.skillLevelId > 0
        ? skill.skillLevelId
        : fallbackSkill.skillLevelId,
    skillCategory: skill?.skillCategory ?? fallbackSkill.skillCategory,
  };
}

function normalizePreviewSlot(
  slot: MissionSlotInput,
  referenceData: MarketplaceReferenceData | null,
): MissionSlotInput {
  const requiredSkills =
    slot.requiredSkills.length > 0
      ? slot.requiredSkills.map((skill) =>
          normalizePreviewSkill(skill, referenceData),
        )
      : [defaultMissionSkillRequirement(referenceData)];

  return {
    roleId: slot.roleId > 0 ? slot.roleId : (referenceData?.roles[0]?.id ?? 0),
    requiredRoleExperienceYears: normalizeRoleExperienceYears(
      Number(slot.requiredRoleExperienceYears),
    ),
    requiredSkills,
  };
}

export function normalizeMissionPreviewSlots(
  slots: MissionSlotInput[],
  referenceData: MarketplaceReferenceData | null,
): MissionSlotInput[] {
  return slots.length > 0
    ? slots.map((slot) => normalizePreviewSlot(slot, referenceData))
    : [defaultMissionSlot(referenceData)];
}

export function buildMissionProposalEditWorkingCopy(
  item: MissionProposalView,
): MissionProposalEditWorkingCopy {
  return {
    customerName: item.specification.customerName,
    customerEmail: item.specification.customerEmail,
    missionTitle: item.specification.missionTitle,
    missionSlots: item.specification.missionSlots.map((slot) => ({
      roleId: slot.roleId,
      requiredRoleExperienceYears: normalizeRoleExperienceYears(
        slot.requiredRoleExperienceYears,
      ),
      requiredSkills: slot.requiredSkills.map((skill) => ({
        skillId: skill.skillId,
        skillLevelId: skill.skillLevelId,
        skillCategory: skill.skillCategory,
      })),
    })),
    startDate: normalizeDateInput(item.specification.startDate),
    endDate: normalizeDateInput(item.specification.endDate),
    workMode: workModeForEdit(item.specification.workMode),
    missionPresentation:
      item.specification.missionPresentation ??
      defaultMissionPresentation(),
  };
}

export function buildMissionProposalEditInput(
  item: MissionProposalView,
  outcome: string,
  editWorkingCopy: unknown,
): MissionProposalEditInput {
  const workingCopy =
    (editWorkingCopy ?? buildMissionProposalEditWorkingCopy(item)) as
      MissionProposalEditWorkingCopy;
  return {
    ...workingCopy,
    outcome,
  };
}
