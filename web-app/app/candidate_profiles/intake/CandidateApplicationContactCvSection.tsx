import type { ChangeEvent } from "react";
import type { CandidateApplicationFormFields } from "~/candidate_profiles/intake/candidateApplicationIntakeState";

type CandidateApplicationContactCvSectionProps = {
  form: CandidateApplicationFormFields;
  previewingCv: boolean;
  cvPreviewStatus: string | null;
  cvPreviewError: string | null;
  updateField: <K extends keyof CandidateApplicationFormFields>(
    name: K,
    value: CandidateApplicationFormFields[K],
  ) => void;
  updateCvMetadata: (event: ChangeEvent<HTMLInputElement>) => void;
};

export function CandidateApplicationContactCvSection({
  form,
  previewingCv,
  cvPreviewStatus,
  cvPreviewError,
  updateField,
  updateCvMetadata,
}: CandidateApplicationContactCvSectionProps) {
  return (
    <>
      <label className="grid gap-2 text-sm">
        <span className="text-label font-medium">Email</span>
        <input
          required
          type="email"
          className="input rounded px-3 py-2"
          value={form.contactEmail}
          onChange={(event) => updateField("contactEmail", event.target.value)}
        />
      </label>

      <label className="panel panel-dash grid cursor-pointer gap-3 rounded-xl p-5 text-sm">
        <span className="text-label font-medium">CV-fil</span>
        <span className="text-muted leading-6">PDF Rekommenderas.</span>
        <input
          required
          type="file"
          accept=".pdf,.doc,.docx,.txt,.md"
          className="input rounded px-3 py-2"
          onChange={updateCvMetadata}
        />
        {form.cvFileName && (
          <span className="text-success text-xs">
            Metadata: {form.cvFileName}, {form.cvContentType},{" "}
            {form.cvSizeBytes ?? 0} bytes
          </span>
        )}
        {previewingCv && (
          <span className="text-soft text-xs">
            Extraherar CV direkt efter upload...
          </span>
        )}
        {!previewingCv && cvPreviewStatus && (
          <span className="text-success text-xs">
            Direkt-extraktion: {cvPreviewStatus}
          </span>
        )}
        {cvPreviewError && (
          <span className="text-danger text-xs">{cvPreviewError}</span>
        )}
      </label>
    </>
  );
}
