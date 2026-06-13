import { type FormEvent, useEffect, useState } from "react";
import { missionsApi } from "~/missions/api/missionsApi";
import { referenceDataApi } from "~/reference_data/api/referenceDataApi";
import { formatCompetencyLevelLabel } from "~/reference_data/competencyLevels";
import {
  limitWords,
  normalizeDateInput,
  wordCount,
} from "~/shared/formHelpers";
import {
  AUTOFILL_FLASH_FIELD_CLASS,
  PREVIEW_MISSING_FIELD_CLASS,
  autofillFieldPathsForSlots,
  normalizedPreviewPath,
} from "~/missions/intake/missionIntakeAutofill";
import {
  missionPresentationWithLimitedField,
  defaultMissionProposalInput as formDefaults,
  defaultMissionSlot,
  defaultMissionSkillRequirement as defaultSkill,
  normalizeMissionPreviewSlots as normalizePreviewSlots,
} from "~/missions/proposal_form/missionProposalFormState";
import type {
  MissionPresentationInput,
  MissionProposalInput,
  MissionProposalPreviewView,
  MissionSlotInput,
  MissionSkillRequirementInput,
} from "~/missions/types";
import type { ApiError } from "~/shared/api/apiErrors";
import type {
  ReferenceData,
  SkillCategory,
} from "~/reference_data/types";

const ROLE_REQUIREMENTS_MAX_WORDS = 200;
const MISSION_TITLE_MAX_WORDS = 20;

export function useMissionIntakeWorkflow() {
  const [form, setForm] = useState<MissionProposalInput>(formDefaults(null));
  const [referenceData, setReferenceData] =
    useState<ReferenceData | null>(null);
  const [loadingReferences, setLoadingReferences] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [receiptId, setReceiptId] = useState<number | null>(null);
  const [roleRequirementsText, setRoleRequirementsText] = useState("");
  const [previewingRequirements, setPreviewingRequirements] = useState(false);
  const [requirementsPreview, setRequirementsPreview] =
    useState<MissionProposalPreviewView | null>(null);
  const [requirementsPreviewError, setRequirementsPreviewError] = useState<
    string | null
  >(null);
  const [editedPreviewFields, setEditedPreviewFields] = useState<Set<string>>(
    () => new Set(),
  );
  const [autofillFlashFields, setAutofillFlashFields] = useState<Set<string>>(
    () => new Set(),
  );

  const roleRequirementsWords = wordCount(roleRequirementsText);
  const missionTitleWords = wordCount(form.missionTitle);

  useEffect(() => {
    let mounted = true;
    referenceDataApi
      .referenceData()
      .then((data) => {
        if (!mounted) return;
        setReferenceData(data);
        setForm((current) => ({
          ...formDefaults(data),
          customerName: current.customerName,
          customerEmail: current.customerEmail,
          missionTitle: current.missionTitle,
          startDate: current.startDate,
          endDate: current.endDate,
          workMode: current.workMode,
          missionPresentation: current.missionPresentation,
        }));
      })
      .catch((err: ApiError) => {
        if (mounted) {
          setError(err.message ?? "Kunde inte ladda referensdata.");
        }
      })
      .finally(() => {
        if (mounted) {
          setLoadingReferences(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (autofillFlashFields.size === 0) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      setAutofillFlashFields(new Set());
    }, 550);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [autofillFlashFields]);

  function markPreviewFieldEdited(...fieldPaths: string[]) {
    setEditedPreviewFields((current) => {
      const next = new Set(current);
      fieldPaths.forEach((fieldPath) =>
        next.add(normalizedPreviewPath(fieldPath)),
      );
      return next;
    });
  }

  function updateField<K extends keyof MissionProposalInput>(
    name: K,
    value: MissionProposalInput[K],
  ) {
    markPreviewFieldEdited(String(name));
    setForm((current) => ({ ...current, [name]: value }));
  }

  function updatePresentationField(
    name: keyof MissionPresentationInput,
    value: string,
  ) {
    setForm((current) => ({
      ...current,
      missionPresentation: missionPresentationWithLimitedField(
        current.missionPresentation,
        name,
        value,
      ),
    }));
  }

  function updateMissionSlot<K extends keyof MissionSlotInput>(
    slotIndex: number,
    name: K,
    value: MissionSlotInput[K],
  ) {
    markPreviewFieldEdited(`missionSlots[${slotIndex}].${String(name)}`);
    setForm((current) => ({
      ...current,
      missionSlots: current.missionSlots.map((slot, currentIndex) =>
        currentIndex === slotIndex ? { ...slot, [name]: value } : slot,
      ),
    }));
  }

  function updateSkillRequirement<K extends keyof MissionSkillRequirementInput>(
    slotIndex: number,
    skillIndex: number,
    name: K,
    value: MissionSkillRequirementInput[K],
  ) {
    markPreviewFieldEdited(
      `missionSlots[${slotIndex}].requiredSkills`,
      `missionSlots[${slotIndex}].requiredSkills[${skillIndex}].${String(name)}`,
    );
    setForm((current) => ({
      ...current,
      missionSlots: current.missionSlots.map((slot, currentSlotIndex) =>
        currentSlotIndex === slotIndex
          ? {
              ...slot,
              requiredSkills: slot.requiredSkills.map(
                (skill, currentSkillIndex) =>
                  currentSkillIndex === skillIndex
                    ? { ...skill, [name]: value }
                    : skill,
              ),
            }
          : slot,
      ),
    }));
  }

  function addMissionSlot() {
    setForm((current) => ({
      ...current,
      missionSlots: [...current.missionSlots, defaultMissionSlot(referenceData)],
    }));
  }

  function removeMissionSlot(slotIndex: number) {
    setForm((current) => ({
      ...current,
      missionSlots:
        current.missionSlots.length === 1
          ? current.missionSlots
          : current.missionSlots.filter(
              (_, currentIndex) => currentIndex !== slotIndex,
            ),
    }));
  }

  function addSkillRequirement(slotIndex: number) {
    setForm((current) => ({
      ...current,
      missionSlots: current.missionSlots.map((slot, currentSlotIndex) =>
        currentSlotIndex === slotIndex
          ? {
              ...slot,
              requiredSkills: [
                ...slot.requiredSkills,
                defaultSkill(referenceData),
              ],
            }
          : slot,
      ),
    }));
  }

  function removeSkillRequirement(slotIndex: number, skillIndex: number) {
    setForm((current) => ({
      ...current,
      missionSlots: current.missionSlots.map((slot, currentSlotIndex) =>
        currentSlotIndex === slotIndex
          ? {
              ...slot,
              requiredSkills:
                slot.requiredSkills.length === 1
                  ? slot.requiredSkills
                  : slot.requiredSkills.filter(
                      (_, currentSkillIndex) =>
                        currentSkillIndex !== skillIndex,
                    ),
            }
          : slot,
      ),
    }));
  }

  function roleTitle(roleId: number) {
    return (
      referenceData?.roles.find((role) => role.id === roleId)?.title ??
      `Roll #${roleId}`
    );
  }

  function skillTitle(skillId: number, skillCategory: SkillCategory) {
    return (
      referenceData?.skills.find(
        (skill) => skill.id === skillId && skill.category === skillCategory,
      )?.title ?? `Kompetens #${skillId}`
    );
  }

  function skillLevelName(skillLevelId: number) {
    const levelName = referenceData?.skillLevels.find(
      (level) => level.id === skillLevelId,
    )?.name;
    return levelName
      ? formatCompetencyLevelLabel(levelName)
      : `Nivå #${skillLevelId}`;
  }

  function isPreviewMissingField(fieldPath: string) {
    const normalizedTarget = normalizedPreviewPath(fieldPath);
    return Boolean(
      requirementsPreview?.missingFields.some((field) => {
        const normalizedField = normalizedPreviewPath(field);
        return (
          field === fieldPath ||
          normalizedField === normalizedTarget ||
          field.endsWith(`.${fieldPath}`) ||
          normalizedField.endsWith(`.${normalizedTarget}`)
        );
      }),
    );
  }

  function missingFieldClass(...fieldPaths: string[]) {
    const missing = fieldPaths.some(isPreviewMissingField);
    const edited = fieldPaths.some((fieldPath) =>
      editedPreviewFields.has(normalizedPreviewPath(fieldPath)),
    );
    return missing && !edited ? PREVIEW_MISSING_FIELD_CLASS : "";
  }

  function autofillFieldClass(...fieldPaths: string[]) {
    return fieldPaths.some((fieldPath) => autofillFlashFields.has(fieldPath))
      ? AUTOFILL_FLASH_FIELD_CLASS
      : "";
  }

  function previewFieldClass(...fieldPaths: string[]) {
    return [missingFieldClass(...fieldPaths), autofillFieldClass(...fieldPaths)]
      .filter(Boolean)
      .join(" ");
  }

  function previewSkillRows(category: "PRIMARY" | "SECONDARY") {
    return form.missionSlots.flatMap((slot, slotIndex) =>
      slot.requiredSkills
        .filter((skill) => skill.skillCategory === category)
        .map((skill, skillIndex) => ({
          key: `${category}-${slotIndex}-${skillIndex}`,
          slotLabel: `Slot ${slotIndex + 1}`,
          title: skillTitle(skill.skillId, skill.skillCategory),
          level: skillLevelName(skill.skillLevelId),
        })),
    );
  }

  function applyRequirementsPreview(preview: MissionProposalPreviewView) {
    const workingCopy = preview.proposalWorkingCopy;
    const normalizedSlots = normalizePreviewSlots(
      workingCopy.missionSlots,
      referenceData,
    );
    setEditedPreviewFields(new Set());
    setAutofillFlashFields(
      new Set([
        "customerName",
        "customerEmail",
        "missionTitle",
        "startDate",
        "endDate",
        "workMode",
        ...autofillFieldPathsForSlots(normalizedSlots),
      ]),
    );
    setRequirementsPreview(preview);
    setForm((current) => ({
      customerName: current.customerName.trim()
        ? current.customerName
        : workingCopy.customerName,
      customerEmail: current.customerEmail.trim()
        ? current.customerEmail
        : workingCopy.customerEmail,
      missionTitle: limitWords(
        workingCopy.missionTitle || current.missionTitle,
        MISSION_TITLE_MAX_WORDS,
      ),
      missionSlots: normalizedSlots,
      startDate: normalizeDateInput(workingCopy.startDate || current.startDate),
      endDate: normalizeDateInput(workingCopy.endDate || current.endDate),
      workMode: workingCopy.workMode || current.workMode,
      missionPresentation: current.missionPresentation,
    }));
  }

  function updateRoleRequirementsText(value: string) {
    setRoleRequirementsText(limitWords(value, ROLE_REQUIREMENTS_MAX_WORDS));
  }

  async function previewRoleRequirements() {
    const trimmedText = roleRequirementsText.trim();
    setRequirementsPreviewError(null);
    setReceiptId(null);

    if (!trimmedText) {
      setRequirementsPreviewError("Uppdragsbeskrivning krävs innan autofyll.");
      return;
    }

    setPreviewingRequirements(true);
    try {
      const input = { roleAndRequirementsText: trimmedText };
      const preview = await missionsApi.previewMissionProposal(input);
      applyRequirementsPreview(preview);
    } catch (err) {
      const apiError = err as ApiError;
      setRequirementsPreview(null);
      setRequirementsPreviewError(
        apiError.message ?? "Kunde inte tolka Uppdragsbeskrivning.",
      );
    } finally {
      setPreviewingRequirements(false);
    }
  }

  async function submitForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setReceiptId(null);
    setRequirementsPreviewError(null);

    try {
      const response = await missionsApi.submitMissionProposal(form);
      setReceiptId(response.id);
      setForm(formDefaults(referenceData));
      setRoleRequirementsText("");
      setRequirementsPreview(null);
    } catch (err) {
      const apiError = err as ApiError;
      setError(apiError.message ?? "Kunde inte skicka proposal.");
    } finally {
      setSubmitting(false);
    }
  }

  return {
    form,
    referenceData,
    loadingReferences,
    submitting,
    error,
    receiptId,
    roleRequirementsText,
    roleRequirementsWords,
    roleRequirementsMaxWords: ROLE_REQUIREMENTS_MAX_WORDS,
    missionTitleWords,
    missionTitleMaxWords: MISSION_TITLE_MAX_WORDS,
    previewingRequirements,
    requirementsPreview,
    requirementsPreviewError,
    primarySkillRows: previewSkillRows("PRIMARY"),
    secondarySkillRows: previewSkillRows("SECONDARY"),
    roleTitle,
    updateField,
    updatePresentationField,
    updateMissionSlot,
    updateSkillRequirement,
    addMissionSlot,
    removeMissionSlot,
    addSkillRequirement,
    removeSkillRequirement,
    previewFieldClass,
    updateRoleRequirementsText,
    previewRoleRequirements,
    submitForm,
  };
}
