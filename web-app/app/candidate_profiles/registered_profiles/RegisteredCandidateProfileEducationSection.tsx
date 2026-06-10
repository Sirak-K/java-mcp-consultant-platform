import {
  currentlyStudyingAvailable,
  defaultCandidateEducationWorkingCopy,
  endDateHasPassed,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import type {
  CandidateCvProfileWorkingCopyInput,
  CandidateEducationWorkingCopyInput,
} from "~/candidate_profiles/types";
import { normalizeDateInput } from "~/shared/formHelpers";

type ProfileWorkingCopyUpdater = (
  updater: (current: CandidateCvProfileWorkingCopyInput) => CandidateCvProfileWorkingCopyInput,
) => void;

type EducationUpdater = (
  index: number,
  updater: (current: CandidateEducationWorkingCopyInput) => CandidateEducationWorkingCopyInput,
) => void;

type RegisteredCandidateProfileEducationSectionProps = {
  profileWorkingCopy: CandidateCvProfileWorkingCopyInput;
  updateProfileWorkingCopy: ProfileWorkingCopyUpdater;
  updateEducation: EducationUpdater;
};

export function RegisteredCandidateProfileEducationSection({
  profileWorkingCopy,
  updateProfileWorkingCopy,
  updateEducation,
}: RegisteredCandidateProfileEducationSectionProps) {
  return (
      <section className="mt-5 grid gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h4 className="text-section font-semibold">Utbildning</h4>
          <button
            type="button"
            className="btn btn-soft rounded px-3 py-1.5 text-sm font-medium"
            onClick={() =>
              updateProfileWorkingCopy((current) => ({
                ...current,
                educations: [
                  ...current.educations,
                  defaultCandidateEducationWorkingCopy(),
                ],
              }))
            }
          >
            Lägg till utbildning
          </button>
        </div>

        {profileWorkingCopy.educations.length === 0 ? (
          <p className="text-soft text-xs">
            Inga utbildningar tillagda ännu.
          </p>
        ) : (
          profileWorkingCopy.educations.map(
            (education, educationIndex) => (
              <div
                key={educationIndex}
                className="panel grid gap-3 rounded-xl p-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h5 className="text-label font-semibold">
                    Utbildning {educationIndex + 1}
                  </h5>
                  <button
                    type="button"
                    className="btn btn-danger rounded px-3 py-1.5 text-sm font-medium"
                    onClick={() =>
                      updateProfileWorkingCopy((current) => ({
                        ...current,
                        educations: current.educations.filter(
                          (_, index) => index !== educationIndex,
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
                      Institution
                    </span>
                    <input
                      className="input rounded px-3 py-2"
                      value={education.institution}
                      onChange={(event) =>
                        updateEducation(
                          educationIndex,
                          (current) => ({
                            ...current,
                            institution: event.target.value,
                          }),
                        )
                      }
                    />
                  </label>
                  <label className="grid gap-2">
                    <span className="text-label font-medium">
                      Akademiskt område
                    </span>
                    <input
                      className="input rounded px-3 py-2"
                      value={education.fieldOfStudy}
                      onChange={(event) =>
                        updateEducation(
                          educationIndex,
                          (current) => ({
                            ...current,
                            fieldOfStudy: event.target.value,
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
                      value={education.startDate}
                      onChange={(event) =>
                        updateEducation(
                          educationIndex,
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
                      disabled={
                        education.currentlyStudying &&
                        currentlyStudyingAvailable(education)
                      }
                      value={
                        education.currentlyStudying &&
                        currentlyStudyingAvailable(education)
                          ? ""
                          : education.endDate
                      }
                      onChange={(event) =>
                        updateEducation(
                          educationIndex,
                          (current) => ({
                            ...current,
                            endDate: normalizeDateInput(
                              event.target.value,
                            ),
                            currentlyStudying: endDateHasPassed(
                              normalizeDateInput(
                                event.target.value,
                              ),
                            )
                              ? false
                              : current.currentlyStudying,
                          }),
                        )
                      }
                    />
                  </label>
                  {currentlyStudyingAvailable(education) && (
                    <label className="flex items-center gap-2 self-end text-sm font-medium">
                      <input
                        type="checkbox"
                        checked={education.currentlyStudying}
                        onChange={(event) =>
                          updateEducation(
                            educationIndex,
                            (current) => ({
                              ...current,
                              currentlyStudying:
                                event.target.checked &&
                                currentlyStudyingAvailable(current),
                              endDate: event.target.checked
                                ? ""
                                : current.endDate,
                            }),
                          )
                        }
                      />
                      Studerar här nu
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
