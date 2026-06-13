import {
  MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import {
  normalizeCandidateRoleExperienceYearsText,
  type CandidateCvProfileFormFields,
  type CandidateRoleFormFields,
  type CandidateSkillFormFields,
} from "~/candidate_profiles/intake/candidateApplicationIntakeState";
import { SkillOptionGroups } from "~/reference_data/components/SkillOptionGroups";
import { formatCompetencyLevelLabel } from "~/reference_data/competencyLevels";
import type { ReferenceData } from "~/reference_data/types";

type CandidateApplicationProfileSectionProps = {
  cvProfile: CandidateCvProfileFormFields;
  candidateRoles: CandidateRoleFormFields[];
  candidateSkills: CandidateSkillFormFields[];
  referenceData: ReferenceData | null;
  loadingReferences: boolean;
  updateProfileField: <K extends keyof CandidateCvProfileFormFields>(
    name: K,
    value: CandidateCvProfileFormFields[K],
  ) => void;
  updateCandidateRole: <K extends keyof CandidateRoleFormFields>(
    index: number,
    name: K,
    value: CandidateRoleFormFields[K],
  ) => void;
  addCandidateRole: () => void;
  removeCandidateRole: (index: number) => void;
  updateCandidateSkill: <K extends keyof CandidateSkillFormFields>(
    index: number,
    name: K,
    value: CandidateSkillFormFields[K],
  ) => void;
  selectCandidateSkill: (index: number, value: string) => void;
  addCandidateSkill: () => void;
  removeCandidateSkill: (index: number) => void;
};

export function CandidateApplicationProfileSection({
  cvProfile,
  candidateRoles,
  candidateSkills,
  referenceData,
  loadingReferences,
  updateProfileField,
  updateCandidateRole,
  addCandidateRole,
  removeCandidateRole,
  updateCandidateSkill,
  selectCandidateSkill,
  addCandidateSkill,
  removeCandidateSkill,
}: CandidateApplicationProfileSectionProps) {
  return (
    <section className="panel-soft grid gap-4 rounded-xl p-5 text-sm">
      <h2 className="text-section text-lg font-semibold">
        2. Kandidatprofil
      </h2>
      <label className="grid gap-2">
        <span className="text-label font-medium">Bio / Summary</span>
        <textarea
          className="input min-h-28 rounded px-3 py-2"
          value={cvProfile.profileSummary}
          onChange={(event) =>
            updateProfileField("profileSummary", event.target.value)
          }
        />
      </label>
      <div className="grid gap-4 md:grid-cols-2">
        <label className="grid gap-2">
          <span className="text-label font-medium">Önskad lön</span>
          <input
            className="input min-w-0 rounded px-3 py-2"
            value={cvProfile.expectedSalary}
            onChange={(event) =>
              updateProfileField("expectedSalary", event.target.value)
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Timpris</span>
          <input
            className="input min-w-0 rounded px-3 py-2"
            value={cvProfile.hourlyRate}
            onChange={(event) =>
              updateProfileField("hourlyRate", event.target.value)
            }
          />
        </label>
      </div>
      <div className="grid gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h3 className="text-label text-base font-semibold">
            Kandidatprofil - Roller
          </h3>
          <button
            type="button"
            onClick={addCandidateRole}
            disabled={loadingReferences || !referenceData}
            className="btn btn-soft rounded px-4 py-2 text-xs font-semibold disabled:opacity-50"
          >
            Lägg till roll
          </button>
        </div>
        {candidateRoles.length === 0 ? (
          <p className="text-soft text-xs">Inga roller tillagda.</p>
        ) : (
          <div className="grid gap-3">
            {candidateRoles.map((role, index) => (
              <div key={index} className="panel grid gap-3 rounded-xl p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h4 className="text-label text-sm font-semibold">
                    Roll {index + 1}
                  </h4>
                  <button
                    type="button"
                    onClick={() => removeCandidateRole(index)}
                    className="btn btn-danger rounded px-3 py-2 text-xs font-semibold"
                  >
                    Ta bort
                  </button>
                </div>
                <div className="grid gap-3 md:grid-cols-[1fr_10rem]">
                  <label className="grid gap-2">
                    <span className="text-label font-medium">Titel</span>
                    <select
                      className="input rounded px-3 py-2"
                      value={role.roleId}
                      onChange={(event) =>
                        updateCandidateRole(
                          index,
                          "roleId",
                          Number(event.target.value),
                        )
                      }
                    >
                      {(referenceData?.roles ?? []).map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.title}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">Antal år</span>
                    <input
                      type="number"
                      min={0}
                      max={MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS}
                      step={1}
                      inputMode="numeric"
                      className="input rounded px-3 py-2"
                      value={role.roleExperienceYears}
                      onChange={(event) =>
                        updateCandidateRole(
                          index,
                          "roleExperienceYears",
                          normalizeCandidateRoleExperienceYearsText(
                            event.target.value,
                          ),
                        )
                      }
                    />
                  </label>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
      <div className="grid gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h3 className="text-label text-base font-semibold">
            Kandidatprofil - Kompetenser
          </h3>
        </div>
        {candidateSkills.length === 0 ? (
          <p className="text-soft text-xs">No structured skills added yet.</p>
        ) : (
          <div className="grid gap-3">
            {candidateSkills.map((skill, index) => (
              <div key={index} className="panel grid gap-3 rounded-xl p-4">
                <div className="grid gap-3 md:grid-cols-[4.5rem_minmax(0,1fr)_minmax(0,1fr)_5rem] md:items-end">
                  <div className="text-label grid justify-items-center gap-2 text-center text-sm font-semibold">
                    <span>Kompetens</span>
                    <span className="flex min-h-10 items-center text-[21px]">
                      {index + 1}
                    </span>
                  </div>
                  <label className="grid gap-2">
                    <span className="text-label text-center font-medium">
                      Teknologi
                    </span>
                    <select
                      className="input rounded px-3 py-2"
                      value={`${skill.skillCategory}:${skill.skillId}`}
                      onChange={(event) =>
                        selectCandidateSkill(index, event.target.value)
                      }
                    >
                      <SkillOptionGroups
                        options={referenceData?.skills ?? []}
                        compositeValue
                      />
                    </select>
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label text-center font-medium">
                      Erfarenhet
                    </span>
                    <select
                      className="input rounded px-3 py-2"
                      value={skill.skillLevelId}
                      onChange={(event) =>
                        updateCandidateSkill(
                          index,
                          "skillLevelId",
                          Number(event.target.value),
                        )
                      }
                    >
                      {(referenceData?.skillLevels ?? []).map((item) => (
                        <option key={item.id} value={item.id}>
                          {formatCompetencyLevelLabel(item.name)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button
                    type="button"
                    onClick={() => removeCandidateSkill(index)}
                    className="btn btn-danger w-20 rounded px-2 py-2 text-xs font-semibold md:self-end"
                  >
                    Ta bort
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
        <button
          type="button"
          onClick={addCandidateSkill}
          disabled={loadingReferences || !referenceData}
          className="btn btn-soft w-fit rounded px-4 py-2 text-xs font-semibold disabled:opacity-50"
        >
          Lägg till kompetens
        </button>
      </div>
    </section>
  );
}
