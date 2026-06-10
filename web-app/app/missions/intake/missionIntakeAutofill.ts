import type { MissionSlotInput } from "~/missions/types";

export const PREVIEW_MISSING_FIELD_CLASS =
  "border-orange-400 ring-1 ring-orange-400/70 focus:border-orange-300 focus:ring-orange-300/80";

export const AUTOFILL_FLASH_FIELD_CLASS = "autofill-shade";

export function normalizedPreviewPath(fieldPath: string) {
  return fieldPath.replace(/\[\d+\]/g, "[]");
}

export function autofillFieldPathsForSlots(slots: MissionSlotInput[]) {
  return slots.flatMap((slot, slotIndex) => [
    `missionSlots[${slotIndex}].roleId`,
    `missionSlots[${slotIndex}].requiredRoleExperienceYears`,
    ...slot.requiredSkills.flatMap((_, skillIndex) => [
      `missionSlots[${slotIndex}].requiredSkills`,
      `missionSlots[${slotIndex}].requiredSkills[${skillIndex}].skillId`,
      `missionSlots[${slotIndex}].requiredSkills[${skillIndex}].skillLevelId`,
    ]),
  ]);
}
