import { roleExperienceTitleLabel } from "~/reference_data/roleExperience";
import { formatCompetencyLevelLabel } from "~/reference_data/competencyLevels";
import {
  MISSION_FORM_WORK_MODE_LABELS,
  formatWorkMode,
} from "~/reference_data/workMode";
import type {
  MissionPresentationInput,
  MissionSlotSpecificationView,
  RegisteredMissionView,
} from "~/missions/types";

export const registeredMissionPlaceholder =
  "Kontrollerad registeredMissionPlaceholder: data saknas i nuvarande mission specification.";

export function hasText(value: string | null | undefined): value is string {
  return Boolean(value?.trim());
}

export function displayText(value: string | null | undefined): string {
  return hasText(value) ? value.trim() : registeredMissionPlaceholder;
}

export function presentationText(
  mission: RegisteredMissionView,
  field: keyof MissionPresentationInput,
): string {
  return displayText(
    mission.specification.missionPresentation?.[field],
  );
}

export const displayWorkMode = (value: string | null | undefined): string =>
  hasText(value)
    ? formatWorkMode(value, MISSION_FORM_WORK_MODE_LABELS)
    : registeredMissionPlaceholder;

export function formatPeriod(mission: RegisteredMissionView): string {
  const { startDate, endDate } = mission.specification;
  if (hasText(startDate) && hasText(endDate)) {
    return `${startDate} till ${endDate}`;
  }
  if (hasText(startDate)) {
    return `Start ${startDate}`;
  }
  if (hasText(endDate)) {
    return `Slut ${endDate}`;
  }
  return registeredMissionPlaceholder;
}

export function displayCustomerName(mission: RegisteredMissionView): string {
  if (hasText(mission.customerName)) {
    return mission.customerName;
  }
  if (hasText(mission.specification.customerName)) {
    return mission.specification.customerName;
  }
  return "Uppdragsgivare saknas";
}

export function slotTitle(slot: MissionSlotSpecificationView): string {
  return `Position ${slot.slotNumber}: ${displayText(slot.roleTitle)}`;
}

export function roleExperienceLabel(years: number): string {
  if (years > 0) {
    return roleExperienceTitleLabel(years);
  }
  return registeredMissionPlaceholder;
}

export function requiredSkillsText(
  slot: MissionSlotSpecificationView,
): string {
  const requiredSkills = slot.requiredSkills ?? [];
  if (requiredSkills.length === 0) {
    return registeredMissionPlaceholder;
  }
  return requiredSkills
    .map(
      (skill) =>
        `${displayText(skill.skillTitle)} (${displayText(formatCompetencyLevelLabel(skill.skillLevelName))})`,
    )
    .join(", ");
}

export function roleExperienceText(
  slot: MissionSlotSpecificationView,
): string {
  if (slot.requiredRoleExperienceYears > 0) {
    return `${slot.requiredRoleExperienceYears} år - ${roleExperienceLabel(
      slot.requiredRoleExperienceYears,
    )}`;
  }
  return registeredMissionPlaceholder;
}
