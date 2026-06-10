import { SkillOptionGroups } from "~/reference_data/components/SkillOptionGroups";
import { formatCompetencyLevelLabel } from "~/reference_data/competencyLevels";
import { normalizeDateInput, wordCount } from "~/shared/formHelpers";
import { ROLE_EXPERIENCE_OPTIONS as EXPERIENCE_OPTIONS } from "~/reference_data/roleExperience";
import {
  MISSION_PRESENTATION_DEFAULTS,
  MISSION_PRESENTATION_FIELDS,
  MISSION_PRESENTATION_MAX_WORDS,
} from "~/missions/mission_presentation/missionPresentationFields";
import {
  MISSION_FORM_WORK_MODE_LABELS,
  workModeOptions,
} from "~/reference_data/workMode";
import {
  missionPresentationWithLimitedField,
  defaultMissionSlot as defaultSlot,
  defaultMissionSkillRequirement as defaultSkill,
  parseMissionSkillSelection,
  type MissionProposalEditWorkingCopy,
} from "~/missions/proposal_form/missionProposalFormState";
import type {
  MissionPresentationInput,
  MissionProposalEditInput,
  MissionSlotInput,
} from "~/missions/types";
import type { MarketplaceReferenceData } from "~/reference_data/types";

type MissionProposalReviewEditFormProps = {
  editWorkingCopy: unknown;
  updateEditWorkingCopy: (updater: (current: unknown) => unknown) => void;
  referenceData: MarketplaceReferenceData | null;
};

export function MissionProposalReviewEditForm({
  editWorkingCopy,
  updateEditWorkingCopy,
  referenceData,
}: MissionProposalReviewEditFormProps) {
const workingCopy =
  editWorkingCopy as MissionProposalEditWorkingCopy;
const updateWorkingCopy = (
  updater: (
    current: MissionProposalEditWorkingCopy,
  ) => MissionProposalEditWorkingCopy,
) =>
  updateEditWorkingCopy((current) =>
    updater(
      (current ??
        workingCopy) as MissionProposalEditWorkingCopy,
    ),
  );
const updateSlot = (
  slotIndex: number,
  updater: (slot: MissionSlotInput) => MissionSlotInput,
) => {
  updateWorkingCopy((current) => ({
    ...current,
    missionSlots: current.missionSlots.map((slot, index) =>
      index === slotIndex ? updater(slot) : slot,
    ),
  }));
};
const updatePresentationField = (
  field: keyof MissionPresentationInput,
  value: string,
) => {
  updateWorkingCopy((current) => ({
    ...current,
    missionPresentation:
      missionPresentationWithLimitedField(
        current.missionPresentation,
        field,
        value,
      ),
  }));
};

return (
  <section className="panel-soft rounded p-4 text-sm">
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h3 className="text-section font-semibold">
          Redigera uppdragsunderlag
        </h3>
        <p className="text-soft mt-1 text-xs">
          Ändringar här sparas först när Save edit är aktiv och klickas.
        </p>
      </div>
    </div>

    <div className="grid gap-4 md:grid-cols-2">
      <label className="grid gap-2">
        <span className="text-label font-medium">
          Uppdragsgivarens namn
        </span>
        <input
          required
          className="input rounded px-3 py-2"
          value={workingCopy.customerName}
          onChange={(event) =>
            updateWorkingCopy((current) => ({
              ...current,
              customerName: event.target.value,
            }))
          }
        />
      </label>
      <label className="grid gap-2">
        <span className="text-label font-medium">Kundens e-post</span>
        <input
          required
          type="email"
          className="input rounded px-3 py-2"
          value={workingCopy.customerEmail}
          onChange={(event) =>
            updateWorkingCopy((current) => ({
              ...current,
              customerEmail: event.target.value,
            }))
          }
        />
      </label>
      <label className="grid gap-2">
        <span className="text-label font-medium">Uppdrag</span>
        <input
          required
          className="input rounded px-3 py-2"
          value={workingCopy.missionTitle}
          onChange={(event) =>
            updateWorkingCopy((current) => ({
              ...current,
              missionTitle: event.target.value,
            }))
          }
        />
      </label>
      <label className="grid gap-2">
        <span className="text-label font-medium">Startdatum</span>
        <input
          required
          type="text"
          inputMode="numeric"
          maxLength={10}
          pattern="\d{4}-\d{2}-\d{2}"
          placeholder="åååå-mm-dd"
          title="Ange datum som åååå-mm-dd"
          className="input rounded px-3 py-2"
          value={workingCopy.startDate}
          onChange={(event) =>
            updateWorkingCopy((current) => ({
              ...current,
              startDate: normalizeDateInput(event.target.value),
            }))
          }
        />
      </label>
      <label className="grid gap-2">
        <span className="text-label font-medium">Slutdatum</span>
        <input
          required
          type="text"
          inputMode="numeric"
          maxLength={10}
          pattern="\d{4}-\d{2}-\d{2}"
          placeholder="åååå-mm-dd"
          title="Ange datum som åååå-mm-dd"
          className="input rounded px-3 py-2"
          value={workingCopy.endDate}
          onChange={(event) =>
            updateWorkingCopy((current) => ({
              ...current,
              endDate: normalizeDateInput(event.target.value),
            }))
          }
        />
      </label>
      <label className="grid gap-2">
        <span className="text-label font-medium">Arbetsläge</span>
        <select
          required
          className="input rounded px-3 py-2"
          value={workingCopy.workMode}
          onChange={(event) =>
            updateWorkingCopy((current) => ({
              ...current,
              workMode: event.target
                .value as MissionProposalEditInput["workMode"],
            }))
          }
        >
          {workModeOptions(MISSION_FORM_WORK_MODE_LABELS).map(
            (option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ),
          )}
        </select>
      </label>
    </div>

    <div className="mt-5 grid gap-4">
      <div>
        <h4 className="text-section font-semibold">
          Uppdragspresentation
        </h4>
        <p className="text-soft mt-1 text-xs">
          Textfält som följer med missionen vid approval.
        </p>
      </div>
      {MISSION_PRESENTATION_FIELDS.map((field) => {
        const value =
          workingCopy.missionPresentation?.[field.key] ??
          MISSION_PRESENTATION_DEFAULTS[field.key];
        return (
          <label key={field.key} className="grid gap-2">
            <span className="text-label font-medium">
              {field.label}
            </span>
            <textarea
              required
              className="input min-h-24 rounded px-3 py-2 leading-6"
              value={value}
              onChange={(event) =>
                updatePresentationField(field.key, event.target.value)
              }
            />
            <span className="text-soft justify-self-end text-xs">
              {wordCount(value)} /{" "}
              {MISSION_PRESENTATION_MAX_WORDS} ord
            </span>
          </label>
        );
      })}
    </div>

    <div className="mt-5 grid gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h4 className="text-section font-semibold">
          Uppdragspositioner
        </h4>
        <button
          type="button"
          className="btn btn-soft rounded px-3 py-1.5 text-sm font-medium"
          onClick={() =>
            updateWorkingCopy((current) => ({
              ...current,
              missionSlots: [
                ...current.missionSlots,
                defaultSlot(referenceData),
              ],
            }))
          }
          disabled={!referenceData}
        >
          Lägg till uppdragsposition
        </button>
      </div>

      {workingCopy.missionSlots.map((slot, slotIndex) => (
        <section key={slotIndex} className="panel rounded-xl p-4">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
            <h5 className="text-section font-medium">
              Uppdragsposition {slotIndex + 1}
            </h5>
            <button
              type="button"
              className="btn btn-danger rounded px-3 py-1.5 text-sm font-medium"
              disabled={workingCopy.missionSlots.length === 1}
              onClick={() =>
                updateWorkingCopy((current) => ({
                  ...current,
                  missionSlots: current.missionSlots.filter(
                    (_, index) => index !== slotIndex,
                  ),
                }))
              }
            >
              Ta bort slot
            </button>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <label className="grid gap-2">
              <span className="text-label font-medium">Slot-roll</span>
              <select
                required
                className="input rounded px-3 py-2"
                value={slot.roleId}
                onChange={(event) =>
                  updateSlot(slotIndex, (current) => ({
                    ...current,
                    roleId: Number(event.target.value),
                  }))
                }
              >
                {referenceData?.roles.map((role) => (
                  <option key={role.id} value={role.id}>
                    {role.title}
                  </option>
                ))}
              </select>
            </label>
            <label className="grid gap-2">
              <span className="text-label font-medium">
                Slot-erfarenhetsår
              </span>
              <select
                required
                className="input rounded px-3 py-2"
                value={slot.requiredRoleExperienceYears}
                onChange={(event) =>
                  updateSlot(slotIndex, (current) => ({
                    ...current,
                    requiredRoleExperienceYears: Number(
                      event.target.value,
                    ),
                  }))
                }
              >
                {EXPERIENCE_OPTIONS.map((option) => (
                  <option key={option.years} value={option.years}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="mt-4 grid gap-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h6 className="text-section font-medium">
                Slot-kompetenskrav
              </h6>
              <button
                type="button"
                className="btn btn-soft rounded px-3 py-1.5 text-sm font-medium"
                onClick={() =>
                  updateSlot(slotIndex, (current) => ({
                    ...current,
                    requiredSkills: [
                      ...current.requiredSkills,
                      defaultSkill(referenceData),
                    ],
                  }))
                }
                disabled={!referenceData}
              >
                Lägg till kompetens
              </button>
            </div>

            {slot.requiredSkills.map((skill, skillIndex) => (
              <div
                key={skillIndex}
                className="grid gap-3 md:grid-cols-[1fr_1fr_auto]"
              >
                <label className="grid gap-2">
                  <span className="text-label font-medium">
                    Kompetens
                  </span>
                  <select
                    required
                    className="input rounded px-3 py-2"
                    value={`${skill.skillCategory}:${skill.skillId}`}
                    onChange={(event) => {
                      const selection =
                        parseMissionSkillSelection(
                          event.target.value,
                        );
                      updateSlot(slotIndex, (current) => ({
                        ...current,
                        requiredSkills: current.requiredSkills.map(
                          (item, index) =>
                            index === skillIndex
                              ? { ...item, ...selection }
                              : item,
                        ),
                      }));
                    }}
                  >
                    <SkillOptionGroups
                      options={referenceData?.skills ?? []}
                      compositeValue
                    />
                  </select>
                </label>
                <label className="grid gap-2">
                  <span className="text-label font-medium">Nivå</span>
                  <select
                    required
                    className="input rounded px-3 py-2"
                    value={skill.skillLevelId}
                    onChange={(event) =>
                      updateSlot(slotIndex, (current) => ({
                        ...current,
                        requiredSkills: current.requiredSkills.map(
                          (item, index) =>
                            index === skillIndex
                              ? {
                                  ...item,
                                  skillLevelId: Number(
                                    event.target.value,
                                  ),
                                }
                              : item,
                        ),
                      }))
                    }
                  >
                    {referenceData?.skillLevels.map((level) => (
                      <option key={level.id} value={level.id}>
                        {formatCompetencyLevelLabel(level.name)}
                      </option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  className="btn btn-danger self-end rounded px-3 py-2 text-sm font-medium"
                  disabled={slot.requiredSkills.length === 1}
                  onClick={() =>
                    updateSlot(slotIndex, (current) => ({
                      ...current,
                      requiredSkills: current.requiredSkills.filter(
                        (_, index) => index !== skillIndex,
                      ),
                    }))
                  }
                >
                  Ta bort
                </button>
              </div>
            ))}
          </div>
        </section>
      ))}
    </div>
  </section>
);
}
