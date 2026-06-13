import { SkillOptionGroups } from "~/reference_data/components/SkillOptionGroups";
import { formatCompetencyLevelLabel } from "~/reference_data/competencyLevels";
import { parseMissionSkillSelection } from "~/missions/proposal_form/missionProposalFormState";
import { ROLE_EXPERIENCE_OPTIONS as EXPERIENCE_OPTIONS } from "~/reference_data/roleExperience";
import type {
  MissionProposalInput,
  MissionSlotInput,
  MissionSkillRequirementInput,
} from "~/missions/types";
import type { ReferenceData } from "~/reference_data/types";

type PreviewFieldClass = (...fieldPaths: string[]) => string;

type MissionIntakeSlotsSectionProps = {
  form: MissionProposalInput;
  referenceData: ReferenceData | null;
  loadingReferences: boolean;
  previewFieldClass: PreviewFieldClass;
  updateMissionSlot: <K extends keyof MissionSlotInput>(
    slotIndex: number,
    name: K,
    value: MissionSlotInput[K],
  ) => void;
  updateSkillRequirement: <K extends keyof MissionSkillRequirementInput>(
    slotIndex: number,
    skillIndex: number,
    name: K,
    value: MissionSkillRequirementInput[K],
  ) => void;
  addMissionSlot: () => void;
  removeMissionSlot: (slotIndex: number) => void;
  addSkillRequirement: (slotIndex: number) => void;
  removeSkillRequirement: (slotIndex: number, skillIndex: number) => void;
};

export function MissionIntakeSlotsSection({
  form,
  referenceData,
  loadingReferences,
  previewFieldClass,
  updateMissionSlot,
  updateSkillRequirement,
  addMissionSlot,
  removeMissionSlot,
  addSkillRequirement,
  removeSkillRequirement,
}: MissionIntakeSlotsSectionProps) {
  return (
    <section className="grid gap-4">
      <div>
        <h2 className="text-section text-lg font-semibold">
          Uppdragspositioner
        </h2>
        <p className="text-muted mt-1 text-sm">
          En position representerar en specifik konsultplats inom uppdraget.
          Minst en position krävs för ett uppdrag och flera kan läggas till om
          uppdraget behöver flera konsulter.
        </p>
      </div>

      {form.missionSlots.map((slot, slotIndex) => (
        <article
          key={slotIndex}
          className="panel-soft grid gap-6 rounded-xl p-5"
        >
          <h3 className="text-section text-xl font-semibold">
            Uppdragsposition {slotIndex + 1}
          </h3>

          <section className="grid gap-4 rounded-lg border border-cyan-400/15 bg-cyan-400/5 p-4">
            <h4 className="text-label text-lg font-semibold">
              Rollkrav för Uppdragsposition {slotIndex + 1}
            </h4>
            <div className="grid gap-4 md:grid-cols-[1fr_1fr_auto]">
              <label className="grid gap-2 text-sm">
                <span className="text-label font-medium">
                  Position {slotIndex + 1} - Roll - Titel
                </span>
                <select
                  required
                  disabled={loadingReferences || !referenceData?.roles.length}
                  className={`input rounded px-3 py-2 ${previewFieldClass(`missionSlots[${slotIndex}].roleId`)}`}
                  value={slot.roleId}
                  onChange={(event) =>
                    updateMissionSlot(
                      slotIndex,
                      "roleId",
                      Number(event.target.value),
                    )
                  }
                >
                  {referenceData?.roles.map((role) => (
                    <option key={role.id} value={role.id}>
                      {role.title}
                    </option>
                  ))}
                </select>
              </label>
              <label className="grid gap-2 text-sm">
                <span className="text-label font-medium">
                  Position {slotIndex + 1} - Roll - Nivå
                </span>
                <select
                  required
                  className={`input rounded px-3 py-2 ${previewFieldClass(
                    `missionSlots[${slotIndex}].requiredRoleExperienceYears`,
                  )}`}
                  value={slot.requiredRoleExperienceYears}
                  onChange={(event) =>
                    updateMissionSlot(
                      slotIndex,
                      "requiredRoleExperienceYears",
                      Number(event.target.value),
                    )
                  }
                >
                  {EXPERIENCE_OPTIONS.map((option) => (
                    <option key={option.years} value={option.years}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
              <button
                type="button"
                className="btn btn-danger self-end rounded px-3 py-2 text-sm font-medium"
                disabled={form.missionSlots.length === 1}
                onClick={() => removeMissionSlot(slotIndex)}
              >
                Ta bort
              </button>
            </div>
          </section>

          <section className="grid gap-3 rounded-lg border border-cyan-400/15 bg-cyan-400/5 p-4">
            <h4 className="text-label text-lg font-semibold">
              Kompetenskrav för Uppdragsposition {slotIndex + 1}
            </h4>

            {slot.requiredSkills.map((skillRequirement, skillIndex) => (
              <div
                key={`${slotIndex}-${skillIndex}`}
                className="grid gap-3 md:grid-cols-[1fr_1fr_auto]"
              >
                <label className="grid gap-2 text-sm">
                  <span className="text-label font-medium">
                    Position {slotIndex + 1} - Kompetens {skillIndex + 1}{" "}
                    - Titel
                  </span>
                  <select
                    required
                    className={`input rounded px-3 py-2 ${previewFieldClass(
                      `missionSlots[${slotIndex}].requiredSkills`,
                      `missionSlots[${slotIndex}].requiredSkills[${skillIndex}].skillId`,
                    )}`}
                    value={`${skillRequirement.skillCategory}:${skillRequirement.skillId}`}
                    onChange={(event) => {
                      const selection = parseMissionSkillSelection(
                        event.target.value,
                      );
                      updateSkillRequirement(
                        slotIndex,
                        skillIndex,
                        "skillCategory",
                        selection.skillCategory,
                      );
                      updateSkillRequirement(
                        slotIndex,
                        skillIndex,
                        "skillId",
                        selection.skillId,
                      );
                    }}
                  >
                    <SkillOptionGroups
                      options={referenceData?.skills ?? []}
                      compositeValue
                    />
                  </select>
                </label>

                <label className="grid gap-2 text-sm">
                  <span className="text-label font-medium">
                    Position {slotIndex + 1} - Kompetens {skillIndex + 1}{" "}
                    - Nivå
                  </span>
                  <select
                    required
                    className={`input rounded px-3 py-2 ${previewFieldClass(
                      `missionSlots[${slotIndex}].requiredSkills.skillLevelId`,
                      `missionSlots[${slotIndex}].requiredSkills[${skillIndex}].skillLevelId`,
                    )}`}
                    value={skillRequirement.skillLevelId}
                    onChange={(event) =>
                      updateSkillRequirement(
                        slotIndex,
                        skillIndex,
                        "skillLevelId",
                        Number(event.target.value),
                      )
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
                  onClick={() => removeSkillRequirement(slotIndex, skillIndex)}
                >
                  Ta bort
                </button>
              </div>
            ))}

            <div className="mt-2 flex justify-end">
              <button
                type="button"
                className="btn btn-soft rounded px-3 py-1.5 text-sm font-medium"
                onClick={() => addSkillRequirement(slotIndex)}
                disabled={loadingReferences}
              >
                Lägg till kompetens
              </button>
            </div>
          </section>
        </article>
      ))}

      <div className="flex justify-center">
        <button
          type="button"
          className="btn btn-soft rounded px-3 py-1.5 text-sm font-medium"
          onClick={addMissionSlot}
          disabled={loadingReferences}
        >
          Lägg till Uppdragsposition
        </button>
      </div>
    </section>
  );
}
