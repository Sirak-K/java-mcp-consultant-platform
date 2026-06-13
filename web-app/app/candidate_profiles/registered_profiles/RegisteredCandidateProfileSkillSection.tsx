import { SkillOptionGroups } from "~/reference_data/components/SkillOptionGroups";
import { formatCompetencyLevelLabel } from "~/reference_data/competencyLevels";
import {
  defaultCandidateSkillWorkingCopy,
  parseCandidateSkillSelection,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import type {
  CandidateCvProfileWorkingCopyInput,
  CandidateSkillWorkingCopyInput,
} from "~/candidate_profiles/types";
import type { ReferenceData } from "~/reference_data/types";

type ProfileWorkingCopyUpdater = (
  updater: (current: CandidateCvProfileWorkingCopyInput) => CandidateCvProfileWorkingCopyInput,
) => void;

type CandidateSkillUpdater = (
  index: number,
  updater: (current: CandidateSkillWorkingCopyInput) => CandidateSkillWorkingCopyInput,
) => void;

type RegisteredCandidateProfileSkillSectionProps = {
  profileWorkingCopy: CandidateCvProfileWorkingCopyInput;
  updateProfileWorkingCopy: ProfileWorkingCopyUpdater;
  updateCandidateSkill: CandidateSkillUpdater;
  canAddSkill: boolean;
  referenceData: ReferenceData | null;
};

export function RegisteredCandidateProfileSkillSection({
  profileWorkingCopy,
  updateProfileWorkingCopy,
  updateCandidateSkill,
  canAddSkill,
  referenceData,
}: RegisteredCandidateProfileSkillSectionProps) {
  return (
      <section className="mt-5 grid gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h4 className="text-section font-semibold">
            Kandidatkompetenser
          </h4>
        </div>

        {profileWorkingCopy.candidateSkills.length === 0 ? (
          <p className="text-soft text-xs">
            Inga strukturerade kompetenser tillagda ännu.
          </p>
        ) : (
          profileWorkingCopy.candidateSkills.map(
            (skill, skillIndex) => (
              <div
                key={skillIndex}
                className="panel grid gap-3 rounded-xl p-4"
              >
                <div className="grid gap-3 md:grid-cols-[4.5rem_minmax(10rem,18rem)_10rem_auto] md:items-end">
                  <h5 className="text-label self-center font-semibold">
                    Kompetens {skillIndex + 1}
                  </h5>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Kompetens
                    </span>
                    <select
                      required
                      className="input rounded px-3 py-2"
                      value={`${skill.skillCategory ?? "PRIMARY"}:${skill.skillId}`}
                      onChange={(event) => {
                        const { skillCategory, skillId } =
                          parseCandidateSkillSelection(
                            event.target.value,
                          );
                        const skillTitle =
                          referenceData?.skills.find(
                            (item) =>
                              item.id === skillId &&
                              item.category === skillCategory,
                          )?.title ?? "";
                        updateCandidateSkill(
                          skillIndex,
                          (current) => ({
                            ...current,
                            skillId,
                            skillTitle,
                            skillCategory,
                          }),
                        );
                      }}
                    >
                      <SkillOptionGroups
                        options={referenceData?.skills ?? []}
                        compositeValue
                      />
                    </select>
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Nivå
                    </span>
                    <select
                      required
                      className="input rounded px-3 py-2"
                      value={skill.skillLevelId}
                      onChange={(event) => {
                        const skillLevelId = Number(
                          event.target.value,
                        );
                        const skillLevelName =
                          referenceData?.skillLevels.find(
                            (item) => item.id === skillLevelId,
                          )?.name ?? "";
                        updateCandidateSkill(
                          skillIndex,
                          (current) => ({
                            ...current,
                            skillLevelId,
                            skillLevelName,
                          }),
                        );
                      }}
                    >
                      {referenceData?.skillLevels.map((item) => (
                        <option key={item.id} value={item.id}>
                          {formatCompetencyLevelLabel(item.name)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button
                    type="button"
                    className="btn btn-danger rounded px-3 py-1.5 text-sm font-medium md:self-end"
                    onClick={() =>
                      updateProfileWorkingCopy((current) => ({
                        ...current,
                        candidateSkills:
                          current.candidateSkills.filter(
                            (_, index) => index !== skillIndex,
                          ),
                      }))
                    }
                  >
                    Ta bort
                  </button>
                </div>
              </div>
            ),
          )
        )}
        <button
          type="button"
          className="btn btn-soft w-fit rounded px-3 py-1.5 text-sm font-medium"
          disabled={!canAddSkill}
          onClick={() =>
            updateProfileWorkingCopy((current) => ({
              ...current,
              candidateSkills: [
                ...current.candidateSkills,
                defaultCandidateSkillWorkingCopy(referenceData),
              ],
            }))
          }
        >
          Lägg till kompetens
        </button>
      </section>
  );
}
