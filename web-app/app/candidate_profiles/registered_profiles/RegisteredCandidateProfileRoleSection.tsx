import {
  MAX_CANDIDATE_ROLE_EXPERIENCE_YEARS,
  defaultCandidateRoleWorkingCopy,
  normalizeCandidateRoleExperienceYears,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import type {
  CandidateCvProfileWorkingCopyInput,
  CandidateRoleWorkingCopyInput,
} from "~/candidate_profiles/types";
import type { MarketplaceReferenceData } from "~/reference_data/types";

type ProfileWorkingCopyUpdater = (
  updater: (current: CandidateCvProfileWorkingCopyInput) => CandidateCvProfileWorkingCopyInput,
) => void;

type CandidateRoleUpdater = (
  index: number,
  updater: (current: CandidateRoleWorkingCopyInput) => CandidateRoleWorkingCopyInput,
) => void;

type RegisteredCandidateProfileRoleSectionProps = {
  profileWorkingCopy: CandidateCvProfileWorkingCopyInput;
  updateProfileWorkingCopy: ProfileWorkingCopyUpdater;
  updateCandidateRole: CandidateRoleUpdater;
  canAddRole: boolean;
  referenceData: MarketplaceReferenceData | null;
};

export function RegisteredCandidateProfileRoleSection({
  profileWorkingCopy,
  updateProfileWorkingCopy,
  updateCandidateRole,
  canAddRole,
  referenceData,
}: RegisteredCandidateProfileRoleSectionProps) {
  return (
      <section className="mt-5 grid gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h4 className="text-section font-semibold">
            Kandidatroller
          </h4>
          <button
            type="button"
            className="btn btn-soft rounded px-3 py-1.5 text-sm font-medium"
            disabled={!canAddRole}
            onClick={() =>
              updateProfileWorkingCopy((current) => ({
                ...current,
                candidateRoles: [
                  ...current.candidateRoles,
                  defaultCandidateRoleWorkingCopy(referenceData),
                ],
              }))
            }
          >
            Lägg till roll
          </button>
        </div>

        {profileWorkingCopy.candidateRoles.length === 0 ? (
          <p className="text-soft text-xs">
            Inga strukturerade roller tillagda ännu.
          </p>
        ) : (
          profileWorkingCopy.candidateRoles.map(
            (role, roleIndex) => (
              <div
                key={roleIndex}
                className="panel grid gap-3 rounded-xl p-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h5 className="text-label font-semibold">
                    Roll {roleIndex + 1}
                  </h5>
                  <button
                    type="button"
                    className="btn btn-danger rounded px-3 py-1.5 text-sm font-medium"
                    onClick={() =>
                      updateProfileWorkingCopy((current) => ({
                        ...current,
                        candidateRoles:
                          current.candidateRoles.filter(
                            (_, index) => index !== roleIndex,
                          ),
                      }))
                    }
                  >
                    Ta bort
                  </button>
                </div>
                <div className="grid gap-3 md:grid-cols-[1fr_12rem]">
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Roll
                    </span>
                    <select
                      required
                      className="input rounded px-3 py-2"
                      value={role.roleId}
                      onChange={(event) => {
                        const roleId = Number(event.target.value);
                        const roleTitle =
                          referenceData?.roles.find(
                            (item) => item.id === roleId,
                          )?.title ?? "";
                        updateCandidateRole(
                          roleIndex,
                          (current) => ({
                            ...current,
                            roleId,
                            roleTitle,
                          }),
                        );
                      }}
                    >
                      {referenceData?.roles.map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.title}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      År
                    </span>
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
                          roleIndex,
                          (current) => ({
                            ...current,
                            roleExperienceYears:
                              normalizeCandidateRoleExperienceYears(
                                event.target.value,
                              ),
                          }),
                        )
                      }
                    />
                  </label>
                </div>
              </div>
            ),
          )
        )}
      </section>
  );
}
