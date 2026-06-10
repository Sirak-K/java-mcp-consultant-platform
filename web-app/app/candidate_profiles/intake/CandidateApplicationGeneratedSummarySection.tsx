import type { CandidateSummaryFormFields } from "~/candidate_profiles/intake/candidateApplicationIntakeState";

type CandidateApplicationGeneratedSummarySectionProps = {
  generatedSummary: CandidateSummaryFormFields;
  previewingCv: boolean;
  hasSummarySourceData: () => boolean;
  generateStructuredSummary: () => void;
  stripCompetencyYearThresholds: (value: string) => string;
};

export function CandidateApplicationGeneratedSummarySection({
  generatedSummary,
  previewingCv,
  hasSummarySourceData,
  generateStructuredSummary,
  stripCompetencyYearThresholds,
}: CandidateApplicationGeneratedSummarySectionProps) {
  return (
    <section className="panel-soft grid gap-3 rounded-xl p-5 text-sm">
      <div>
        <h2 className="text-section text-lg font-semibold">
          Genererad sammanfattning
        </h2>
        <p className="text-muted mt-2 leading-6">
          Sammanfattningen är baserad på värdena som har angetts.
        </p>
      </div>
      <div className="grid gap-2">
        <label className="grid gap-2">
          <span className="text-label font-medium">
            Kompetenser - Översikt
          </span>
          <textarea
            readOnly
            className="input min-h-20 rounded px-3 py-2"
            value={stripCompetencyYearThresholds(
              generatedSummary.coreCompetenceOverview,
            )}
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Geografisk placering</span>
          <textarea
            readOnly
            className="input min-h-16 rounded px-3 py-2"
            value={generatedSummary.location}
          />
        </label>
        <label className="grid gap-2">
          <span className="text-label font-medium">Övriga detaljer</span>
          <textarea
            readOnly
            className="input min-h-20 rounded px-3 py-2"
            value={generatedSummary.otherDetails}
          />
        </label>
      </div>
      <button
        type="button"
        onClick={generateStructuredSummary}
        disabled={previewingCv || !hasSummarySourceData()}
        className="btn btn-soft justify-self-start rounded px-4 py-2 text-sm font-semibold disabled:opacity-50"
      >
        {previewingCv ? "Building..." : "Generera"}
      </button>
    </section>
  );
}
