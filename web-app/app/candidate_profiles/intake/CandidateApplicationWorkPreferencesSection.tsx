import {
  LOCATION_FLEXIBILITY_OPTIONS,
  normalizeLocationFlexibility,
  type CandidateCvProfileFormFields,
} from "~/candidate_profiles/intake/candidateApplicationIntakeState";
import {
  CANDIDATE_APPLICATION_WORK_MODE_LABELS,
  workModeOptions,
} from "~/reference_data/workMode";

type CandidateApplicationWorkPreferencesSectionProps = {
  cvProfile: CandidateCvProfileFormFields;
  updateProfileField: <K extends keyof CandidateCvProfileFormFields>(
    name: K,
    value: CandidateCvProfileFormFields[K],
  ) => void;
};

export function CandidateApplicationWorkPreferencesSection({
  cvProfile,
  updateProfileField,
}: CandidateApplicationWorkPreferencesSectionProps) {
  return (
    <section className="panel-soft grid gap-4 rounded-xl p-5 text-sm">
      <h2 className="text-section text-lg font-semibold">
        4. Arbetspreferenser
      </h2>
      <div className="grid gap-4 md:grid-cols-2">
        <label className="grid gap-2">
          <span className="text-label font-medium">Arbetsläge</span>
          <select
            className="input rounded px-3 py-2"
            value={cvProfile.workMode}
            onChange={(event) =>
              updateProfileField("workMode", event.target.value)
            }
          >
            {workModeOptions(CANDIDATE_APPLICATION_WORK_MODE_LABELS).map(
              (item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ),
            )}
          </select>
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Flexibilitet</span>
          <select
            className="input rounded px-3 py-2"
            value={cvProfile.locationFlexibility}
            onChange={(event) =>
              updateProfileField(
                "locationFlexibility",
                normalizeLocationFlexibility(event.target.value),
              )
            }
          >
            <option value="">Select flexibility</option>
            {LOCATION_FLEXIBILITY_OPTIONS.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </label>
      </div>
      <label className="flex items-center gap-2">
        <input
          type="checkbox"
          checked={cvProfile.willingToRelocate}
          onChange={(event) =>
            updateProfileField("willingToRelocate", event.target.checked)
          }
        />
        <span className="text-label font-medium">Kan flytta</span>
      </label>
    </section>
  );
}
