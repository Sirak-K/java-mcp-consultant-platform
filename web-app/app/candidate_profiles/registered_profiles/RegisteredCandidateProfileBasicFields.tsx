import {
  CANDIDATE_PROFILE_REVIEW_WORK_MODE_LABELS,
  workModeOptions,
} from "~/reference_data/workMode";
import type {
  CandidateApplicationEditWorkingCopy,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import type { CandidateCvProfileWorkingCopyInput } from "~/candidate_profiles/types";

type EditWorkingCopyUpdater = (
  updater: (current: CandidateApplicationEditWorkingCopy) => CandidateApplicationEditWorkingCopy,
) => void;

type ProfileWorkingCopyUpdater = (
  updater: (current: CandidateCvProfileWorkingCopyInput) => CandidateCvProfileWorkingCopyInput,
) => void;

type RegisteredCandidateProfileBasicFieldsProps = {
  workingCopy: CandidateApplicationEditWorkingCopy;
  profileWorkingCopy: CandidateCvProfileWorkingCopyInput;
  updateWorkingCopy: EditWorkingCopyUpdater;
  updateProfileWorkingCopy: ProfileWorkingCopyUpdater;
};

export function RegisteredCandidateProfileBasicFields({
  workingCopy,
  profileWorkingCopy,
  updateWorkingCopy,
  updateProfileWorkingCopy,
}: RegisteredCandidateProfileBasicFieldsProps) {
  return (
      <div className="grid gap-4 md:grid-cols-2">
        <label className="grid gap-2">
          <span className="text-label font-medium">
            Kontakt-email
          </span>
          <input
            required
            type="email"
            className="input rounded px-3 py-2"
            value={workingCopy.contactEmail}
            onChange={(event) =>
              updateWorkingCopy((current) => ({
                ...current,
                contactEmail: event.target.value,
              }))
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">CV-filnamn</span>
          <input
            required
            className="input rounded px-3 py-2"
            value={workingCopy.cvFileName}
            onChange={(event) =>
              updateWorkingCopy((current) => ({
                ...current,
                cvFileName: event.target.value,
              }))
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Förnamn</span>
          <input
            required
            className="input rounded px-3 py-2"
            value={profileWorkingCopy.firstName}
            onChange={(event) =>
              updateProfileWorkingCopy((current) => ({
                ...current,
                firstName: event.target.value,
              }))
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Efternamn</span>
          <input
            required
            className="input rounded px-3 py-2"
            value={profileWorkingCopy.lastName}
            onChange={(event) =>
              updateProfileWorkingCopy((current) => ({
                ...current,
                lastName: event.target.value,
              }))
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Arbetsläge</span>
          <select
            required
            className="input rounded px-3 py-2"
            value={profileWorkingCopy.workMode || "REMOTE"}
            onChange={(event) =>
              updateProfileWorkingCopy((current) => ({
                ...current,
                workMode: event.target.value,
              }))
            }
          >
            {workModeOptions(
              CANDIDATE_PROFILE_REVIEW_WORK_MODE_LABELS,
            ).map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Stad</span>
          <input
            className="input rounded px-3 py-2"
            value={profileWorkingCopy.city}
            onChange={(event) =>
              updateProfileWorkingCopy((current) => ({
                ...current,
                city: event.target.value,
              }))
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Land</span>
          <input
            className="input rounded px-3 py-2"
            value={profileWorkingCopy.country}
            onChange={(event) =>
              updateProfileWorkingCopy((current) => ({
                ...current,
                country: event.target.value,
              }))
            }
          />
        </label>
        <label className="grid gap-2 md:col-span-2">
          <span className="text-label font-medium">
            Bio / sammanfattning
          </span>
          <textarea
            className="input min-h-24 rounded px-3 py-2"
            value={profileWorkingCopy.profileSummary}
            onChange={(event) =>
              updateProfileWorkingCopy((current) => ({
                ...current,
                profileSummary: event.target.value,
              }))
            }
          />
        </label>
      </div>
  );
}
