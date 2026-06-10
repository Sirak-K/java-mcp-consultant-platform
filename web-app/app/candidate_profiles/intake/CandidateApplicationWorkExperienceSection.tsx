import {
  currentlyWorkingHereAvailable,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import type { CandidateWorkExperienceFormFields } from "~/candidate_profiles/intake/candidateApplicationIntakeState";
import { normalizeDateInput } from "~/shared/formHelpers";

type CandidateApplicationWorkExperienceSectionProps = {
  workExperiences: CandidateWorkExperienceFormFields[];
  addWorkExperience: () => void;
  removeWorkExperience: (index: number) => void;
  updateWorkExperienceField: <K extends keyof CandidateWorkExperienceFormFields>(
    index: number,
    name: K,
    value: CandidateWorkExperienceFormFields[K],
  ) => void;
  updateCurrentWorkExperience: (
    index: number,
    currentlyHere: boolean,
  ) => void;
  selectCompanyIdentity: (index: number, organisationNumber: string) => void;
};

export function CandidateApplicationWorkExperienceSection({
  workExperiences,
  addWorkExperience,
  removeWorkExperience,
  updateWorkExperienceField,
  updateCurrentWorkExperience,
  selectCompanyIdentity,
}: CandidateApplicationWorkExperienceSectionProps) {
  return (
    <section className="panel-soft grid gap-4 rounded-xl p-5 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-section text-lg font-semibold">
          3. Arbetslivserfarenheter
        </h2>
        <button
          type="button"
          onClick={addWorkExperience}
          className="btn btn-soft rounded px-4 py-2 text-sm font-semibold"
        >
          Add Work Experience
        </button>
      </div>
      <div className="grid gap-4">
        {workExperiences.map((workExperience, index) => (
          <div key={index} className="panel grid gap-4 rounded-xl p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-label text-base font-semibold">
                Work Experience {index + 1}
              </h3>
              {workExperiences.length > 1 && (
                <button
                  type="button"
                  onClick={() => removeWorkExperience(index)}
                  className="btn btn-danger rounded px-3 py-2 text-xs font-semibold"
                >
                  Ta bort
                </button>
              )}
            </div>
            <div className="grid gap-4 md:grid-cols-2">
              <label className="grid gap-2">
                <span className="text-label font-medium">Yrkesbefattning</span>
                <input
                  className="input rounded px-3 py-2"
                  value={workExperience.jobTitle}
                  onChange={(event) =>
                    updateWorkExperienceField(
                      index,
                      "jobTitle",
                      event.target.value,
                    )
                  }
                />
              </label>
              <label className="grid gap-2">
                <span className="text-label font-medium">Företagets Namn</span>
                <input
                  className="input rounded px-3 py-2"
                  readOnly={Boolean(workExperience.workExpCompanyOrgNr)}
                  value={workExperience.workExpCompany}
                  onChange={(event) =>
                    updateWorkExperienceField(
                      index,
                      "workExpCompany",
                      event.target.value,
                    )
                  }
                />
              </label>
              <label className="grid gap-2">
                <span className="text-label font-medium">
                  Organisationens Nr
                </span>
                <input
                  className="input rounded px-3 py-2"
                  readOnly={Boolean(workExperience.workExpCompanyOrgNr)}
                  value={workExperience.workExpCompanyOrgNr}
                  onChange={(event) =>
                    updateWorkExperienceField(
                      index,
                      "workExpCompanyOrgNr",
                      event.target.value,
                    )
                  }
                />
              </label>
              {workExperience.companyIdentityOptions.length > 0 &&
                !workExperience.workExpCompanyOrgNr && (
                  <label className="grid gap-2 md:col-span-2">
                    <span className="text-label font-medium">
                      Välj korrekt företag
                    </span>
                    <select
                      className="input rounded px-3 py-2"
                      value=""
                      onChange={(event) =>
                        selectCompanyIdentity(index, event.target.value)
                      }
                    >
                      <option value="">Välj företag...</option>
                      {workExperience.companyIdentityOptions.map((option) => (
                        <option
                          key={option.organisationNumber}
                          value={option.organisationNumber}
                        >
                          {[
                            option.organisationName,
                            option.organisationNumber,
                            option.organisationCity,
                          ]
                            .filter(Boolean)
                            .join(" | ")}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
              <label className="grid gap-2">
                <span className="text-label font-medium">Stad</span>
                <input
                  className="input rounded px-3 py-2"
                  value={workExperience.city}
                  onChange={(event) =>
                    updateWorkExperienceField(index, "city", event.target.value)
                  }
                />
              </label>
              <label className="grid gap-2">
                <span className="text-label font-medium">Land</span>
                <input
                  className="input rounded px-3 py-2"
                  value={workExperience.country}
                  onChange={(event) =>
                    updateWorkExperienceField(
                      index,
                      "country",
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
                  value={workExperience.startDate}
                  onChange={(event) =>
                    updateWorkExperienceField(
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
                  disabled={workExperience.currentlyHere}
                  value={workExperience.endDate}
                  onChange={(event) =>
                    updateWorkExperienceField(
                      index,
                      "endDate",
                      normalizeDateInput(event.target.value),
                    )
                  }
                />
              </label>
            </div>
            {currentlyWorkingHereAvailable(workExperience) && (
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={workExperience.currentlyHere}
                  onChange={(event) =>
                    updateCurrentWorkExperience(index, event.target.checked)
                  }
                />
                <span className="text-label font-medium">
                  Currently working here
                </span>
              </label>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
