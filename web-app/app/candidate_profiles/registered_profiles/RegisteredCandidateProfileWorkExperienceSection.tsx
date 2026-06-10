import {
  currentlyWorkingHereAvailable,
  defaultCandidateWorkExperienceWorkingCopy,
  endDateHasPassed,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import type {
  CandidateCvProfileWorkingCopyInput,
  CandidateWorkExperienceWorkingCopyInput,
} from "~/candidate_profiles/types";
import { normalizeDateInput } from "~/shared/formHelpers";

type ProfileWorkingCopyUpdater = (
  updater: (current: CandidateCvProfileWorkingCopyInput) => CandidateCvProfileWorkingCopyInput,
) => void;

type WorkExperienceUpdater = (
  index: number,
  updater: (current: CandidateWorkExperienceWorkingCopyInput) => CandidateWorkExperienceWorkingCopyInput,
) => void;

type RegisteredCandidateProfileWorkExperienceSectionProps = {
  profileWorkingCopy: CandidateCvProfileWorkingCopyInput;
  updateProfileWorkingCopy: ProfileWorkingCopyUpdater;
  updateWorkExperience: WorkExperienceUpdater;
};

export function RegisteredCandidateProfileWorkExperienceSection({
  profileWorkingCopy,
  updateProfileWorkingCopy,
  updateWorkExperience,
}: RegisteredCandidateProfileWorkExperienceSectionProps) {
  return (
      <section className="mt-5 grid gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h4 className="text-section font-semibold">
            Arbetslivserfarenheter
          </h4>
          <button
            type="button"
            className="btn btn-soft rounded px-3 py-1.5 text-sm font-medium"
            onClick={() =>
              updateProfileWorkingCopy((current) => ({
                ...current,
                workExperiences: [
                  ...current.workExperiences,
                  defaultCandidateWorkExperienceWorkingCopy(),
                ],
              }))
            }
          >
            Lägg till arbetslivserfarenhet
          </button>
        </div>

        {profileWorkingCopy.workExperiences.length === 0 ? (
          <p className="text-soft text-xs">
            Inga arbetslivserfarenheter tillagda ännu.
          </p>
        ) : (
          profileWorkingCopy.workExperiences.map(
            (workExperience, workExperienceIndex) => (
              <div
                key={workExperienceIndex}
                className="panel grid gap-3 rounded-xl p-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h5 className="text-label font-semibold">
                    Arbetslivserfarenhet {workExperienceIndex + 1}
                  </h5>
                  <button
                    type="button"
                    className="btn btn-danger rounded px-3 py-1.5 text-sm font-medium"
                    onClick={() =>
                      updateProfileWorkingCopy((current) => ({
                        ...current,
                        workExperiences:
                          current.workExperiences.filter(
                            (_, index) =>
                              index !== workExperienceIndex,
                          ),
                      }))
                    }
                  >
                    Ta bort
                  </button>
                </div>
                <div className="grid gap-3 md:grid-cols-2">
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Yrkesbefattning
                    </span>
                    <input
                      className="input rounded px-3 py-2"
                      value={workExperience.jobTitle}
                      onChange={(event) =>
                        updateWorkExperience(
                          workExperienceIndex,
                          (current) => ({
                            ...current,
                            jobTitle: event.target.value,
                          }),
                        )
                      }
                    />
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Företagsnamn
                    </span>
                    <input
                      className="input rounded px-3 py-2"
                      value={workExperience.workExpCompany}
                      onChange={(event) =>
                        updateWorkExperience(
                          workExperienceIndex,
                          (current) => ({
                            ...current,
                            workExpCompany: event.target.value,
                          }),
                        )
                      }
                    />
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Organisationsnr
                    </span>
                    <input
                      className="input rounded px-3 py-2"
                      value={workExperience.workExpCompanyOrgNr}
                      onChange={(event) =>
                        updateWorkExperience(
                          workExperienceIndex,
                          (current) => ({
                            ...current,
                            workExpCompanyOrgNr: event.target.value,
                          }),
                        )
                      }
                    />
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Stad
                    </span>
                    <input
                      className="input rounded px-3 py-2"
                      value={workExperience.city}
                      onChange={(event) =>
                        updateWorkExperience(
                          workExperienceIndex,
                          (current) => ({
                            ...current,
                            city: event.target.value,
                          }),
                        )
                      }
                    />
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Land
                    </span>
                    <input
                      className="input rounded px-3 py-2"
                      value={workExperience.country}
                      onChange={(event) =>
                        updateWorkExperience(
                          workExperienceIndex,
                          (current) => ({
                            ...current,
                            country: event.target.value,
                          }),
                        )
                      }
                    />
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Startdatum
                    </span>
                    <input
                      type="text"
                      inputMode="numeric"
                      maxLength={10}
                      pattern="\d{4}-\d{2}-\d{2}"
                      placeholder="åååå-mm-dd"
                      className="input rounded px-3 py-2"
                      value={workExperience.startDate}
                      onChange={(event) =>
                        updateWorkExperience(
                          workExperienceIndex,
                          (current) => ({
                            ...current,
                            startDate: normalizeDateInput(
                              event.target.value,
                            ),
                          }),
                        )
                      }
                    />
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Slutdatum
                    </span>
                    <input
                      type="text"
                      inputMode="numeric"
                      maxLength={10}
                      pattern="\d{4}-\d{2}-\d{2}"
                      placeholder="åååå-mm-dd"
                      className="input rounded px-3 py-2"
                      disabled={workExperience.currentlyHere}
                      value={
                        workExperience.currentlyHere
                          ? ""
                          : workExperience.endDate
                      }
                      onChange={(event) => {
                        const endDate = normalizeDateInput(
                          event.target.value,
                        );
                        updateWorkExperience(
                          workExperienceIndex,
                          (current) => ({
                            ...current,
                            endDate,
                            currentlyHere: endDateHasPassed(endDate)
                              ? false
                              : current.currentlyHere,
                          }),
                        );
                      }}
                    />
                  </label>
                  {currentlyWorkingHereAvailable(
                    workExperience,
                  ) && (
                    <label className="flex items-center gap-2 self-end text-sm font-medium">
                      <input
                        type="checkbox"
                        checked={workExperience.currentlyHere}
                        onChange={(event) =>
                          updateWorkExperience(
                            workExperienceIndex,
                            (current) => ({
                              ...current,
                              currentlyHere:
                                event.target.checked &&
                                currentlyWorkingHereAvailable(
                                  current,
                                ),
                              endDate: event.target.checked
                                ? ""
                                : current.endDate,
                            }),
                          )
                        }
                      />
                      Arbetar här nu
                    </label>
                  )}
                </div>
              </div>
            ),
          )
        )}
      </section>
  );
}
