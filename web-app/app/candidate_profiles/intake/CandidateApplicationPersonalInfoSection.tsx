import { CandidateApplicationLanguageField } from "~/candidate_profiles/intake/CandidateApplicationLanguageField";
import type { CandidateCvProfileFormFields } from "~/candidate_profiles/intake/candidateApplicationIntakeState";

type CandidateApplicationPersonalInfoSectionProps = {
  cvProfile: CandidateCvProfileFormFields;
  languageWorkingCopy: string;
  setLanguageWorkingCopy: (value: string) => void;
  updateProfileField: <K extends keyof CandidateCvProfileFormFields>(
    name: K,
    value: CandidateCvProfileFormFields[K],
  ) => void;
  commitLabelWorkingCopy: (
    name: "languages",
    workingCopyValue: string,
    resetWorkingCopy: () => void,
  ) => void;
  removeLabelValue: (name: "languages", valueToRemove: string) => void;
};

export function CandidateApplicationPersonalInfoSection({
  cvProfile,
  languageWorkingCopy,
  setLanguageWorkingCopy,
  updateProfileField,
  commitLabelWorkingCopy,
  removeLabelValue,
}: CandidateApplicationPersonalInfoSectionProps) {
  return (
    <section className="panel-soft grid gap-4 rounded-xl p-5 text-sm">
      <h2 className="text-section text-lg font-semibold">
        1. Personlig Information
      </h2>
      <div className="grid gap-4 md:grid-cols-2">
        <label className="grid gap-2">
          <span className="text-label font-medium">Förnamn</span>
          <input
            className="input rounded px-3 py-2"
            value={cvProfile.firstName}
            onChange={(event) =>
              updateProfileField("firstName", event.target.value)
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Efternamn</span>
          <input
            className="input rounded px-3 py-2"
            value={cvProfile.lastName}
            onChange={(event) =>
              updateProfileField("lastName", event.target.value)
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Mobilnummer</span>
          <input
            className="input rounded px-3 py-2"
            value={cvProfile.phoneNumber}
            onChange={(event) =>
              updateProfileField("phoneNumber", event.target.value)
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Land</span>
          <input
            className="input rounded px-3 py-2"
            value={cvProfile.country}
            onChange={(event) =>
              updateProfileField("country", event.target.value)
            }
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Stad</span>
          <input
            className="input rounded px-3 py-2"
            value={cvProfile.city}
            onChange={(event) =>
              updateProfileField("city", event.target.value)
            }
          />
        </label>
        <CandidateApplicationLanguageField
          value={cvProfile.languages}
          workingCopyValue={languageWorkingCopy}
          onWorkingCopyChange={setLanguageWorkingCopy}
          onCommit={() =>
            commitLabelWorkingCopy("languages", languageWorkingCopy, () =>
              setLanguageWorkingCopy(""),
            )
          }
          onRemove={(value) => removeLabelValue("languages", value)}
        />
      </div>
    </section>
  );
}
