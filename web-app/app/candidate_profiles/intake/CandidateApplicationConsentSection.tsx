import type { CandidateCvProfileFormFields } from "~/candidate_profiles/intake/candidateApplicationIntakeState";

type CandidateApplicationConsentSectionProps = {
  cvProfile: CandidateCvProfileFormFields;
  updateProfileField: <K extends keyof CandidateCvProfileFormFields>(
    name: K,
    value: CandidateCvProfileFormFields[K],
  ) => void;
};

export function CandidateApplicationConsentSection({
  cvProfile,
  updateProfileField,
}: CandidateApplicationConsentSectionProps) {
  return (
    <section className="panel-soft grid gap-4 rounded-xl p-5 text-sm">
      <h2 className="text-section text-lg font-semibold">7. Samtycke (GDPR)</h2>
      <label className="flex items-center gap-2">
        <input
          type="checkbox"
          checked={cvProfile.gdprConsent}
          onChange={(event) =>
            updateProfileField("gdprConsent", event.target.checked)
          }
        />
        <span className="text-label font-medium">
          Jag samtycker till att mina CV-uppgifter behandlas för
          rekryteringsändamål.
        </span>
      </label>
    </section>
  );
}
