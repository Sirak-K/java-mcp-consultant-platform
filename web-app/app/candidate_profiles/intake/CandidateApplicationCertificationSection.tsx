import type { CandidateCertificationFormFields } from "~/candidate_profiles/intake/candidateApplicationIntakeState";

type CandidateApplicationCertificationSectionProps = {
  certifications: CandidateCertificationFormFields[];
  certificationFeedback: string | null;
  addCertification: () => void;
  removeCertification: (index: number) => void;
  updateCertificationField: <K extends keyof CandidateCertificationFormFields>(
    index: number,
    name: K,
    value: CandidateCertificationFormFields[K],
  ) => void;
  updateCertificationDocument: (index: number, file: File | null) => void;
};

export function CandidateApplicationCertificationSection({
  certifications,
  certificationFeedback,
  addCertification,
  removeCertification,
  updateCertificationField,
  updateCertificationDocument,
}: CandidateApplicationCertificationSectionProps) {
  return (
    <section className="panel-soft grid gap-4 rounded-xl p-5 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-section text-lg font-semibold">6. Certifikat</h2>
        <button
          type="button"
          onClick={addCertification}
          className="btn btn-soft rounded px-4 py-2 text-sm font-semibold"
        >
          Lägg till Certifikat
        </button>
      </div>
      {certificationFeedback && (
        <p className="text-warning text-sm font-semibold">
          {certificationFeedback}
        </p>
      )}
      <div className="grid gap-4">
        {certifications.map((certification, index) => (
          <div key={index} className="panel grid gap-4 rounded-xl p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-label text-base font-semibold">
                Certifikat {index + 1}
              </h3>
              <button
                type="button"
                onClick={() => removeCertification(index)}
                className="btn btn-danger rounded px-3 py-2 text-xs font-semibold"
              >
                Ta bort
              </button>
            </div>

            <label className="grid gap-2">
              <span className="text-label font-medium">Certifikat Titel</span>
              <input
                className="input rounded px-3 py-2"
                value={certification.name}
                onChange={(event) =>
                  updateCertificationField(index, "name", event.target.value)
                }
              />
            </label>

            <label className="grid gap-2">
              <span className="text-label font-medium">
                Ladda upp certifikat
              </span>
              <input
                type="file"
                accept=".pdf,.doc,.docx,.txt,.md"
                className="input rounded px-3 py-2"
                onChange={(event) =>
                  updateCertificationDocument(
                    index,
                    event.target.files?.[0] ?? null,
                  )
                }
              />
            </label>

            {certification.documentAttached && (
              <p className="text-success text-xs">
                Metadata: {certification.documentFileName},{" "}
                {certification.documentContentType ||
                  "application/octet-stream"}
                , {certification.documentSizeBytes ?? 0} bytes
              </p>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
