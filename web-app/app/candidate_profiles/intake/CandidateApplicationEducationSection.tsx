import {
  currentlyStudyingAvailable,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import type { CandidateEducationFormFields } from "~/candidate_profiles/intake/candidateApplicationIntakeState";
import { normalizeDateInput } from "~/shared/formHelpers";

type CandidateApplicationEducationSectionProps = {
  educations: CandidateEducationFormFields[];
  addEducation: () => void;
  removeEducation: (index: number) => void;
  updateEducationField: <K extends keyof CandidateEducationFormFields>(
    index: number,
    name: K,
    value: CandidateEducationFormFields[K],
  ) => void;
  updateCurrentEducation: (index: number, currentlyStudying: boolean) => void;
};

export function CandidateApplicationEducationSection({
  educations,
  addEducation,
  removeEducation,
  updateEducationField,
  updateCurrentEducation,
}: CandidateApplicationEducationSectionProps) {
  return (
    <section className="panel-soft grid gap-4 rounded-xl p-5 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-section text-lg font-semibold">5. Utbildning</h2>
        <button
          type="button"
          onClick={addEducation}
          className="btn btn-soft rounded px-4 py-2 text-sm font-semibold"
        >
          Lägg till utbildning
        </button>
      </div>
      <div className="grid gap-4">
        {educations.map((education, index) => (
          <div key={index} className="panel grid gap-4 rounded-xl p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-label text-base font-semibold">
                Education {index + 1}
              </h3>
              {educations.length > 1 && (
                <button
                  type="button"
                  onClick={() => removeEducation(index)}
                  className="btn btn-danger rounded px-3 py-2 text-xs font-semibold"
                >
                  Ta bort
                </button>
              )}
            </div>
            <div className="grid gap-4 md:grid-cols-2">
              <label className="grid gap-2">
                <span className="text-label font-medium">Institution</span>
                <input
                  className="input rounded px-3 py-2"
                  value={education.institution}
                  onChange={(event) =>
                    updateEducationField(
                      index,
                      "institution",
                      event.target.value,
                    )
                  }
                />
              </label>
              <label className="grid gap-2">
                <span className="text-label font-medium">Akademiskt Område</span>
                <input
                  className="input rounded px-3 py-2"
                  value={education.fieldOfStudy}
                  onChange={(event) =>
                    updateEducationField(
                      index,
                      "fieldOfStudy",
                      event.target.value,
                    )
                  }
                />
              </label>
              <label className="grid gap-2">
                <span className="text-label font-medium">Startdatum</span>
                <input
                  type="date"
                  className="input rounded px-3 py-2"
                  value={education.startDate}
                  onChange={(event) =>
                    updateEducationField(
                      index,
                      "startDate",
                      normalizeDateInput(event.target.value),
                    )
                  }
                />
              </label>
              <label className="grid gap-2">
                <span className="text-label font-medium">Slutdatum</span>
                <input
                  type="date"
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
                    updateEducationField(
                      index,
                      "endDate",
                      normalizeDateInput(event.target.value),
                    )
                  }
                />
              </label>
            </div>
            {currentlyStudyingAvailable(education) && (
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={education.currentlyStudying}
                  onChange={(event) =>
                    updateCurrentEducation(index, event.target.checked)
                  }
                />
                <span className="text-label font-medium">Studerar här nu</span>
              </label>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
