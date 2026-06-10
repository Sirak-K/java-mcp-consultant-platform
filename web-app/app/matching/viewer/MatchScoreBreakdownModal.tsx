import type { MatchScoreBreakdownView } from "~/matching/types";
import {
  translateMatchBreakdownDecision,
  translateMatchBreakdownFactor,
  translateMatchBreakdownNote,
  translateMissingOrWeakFactor,
  translateScoreLabel,
} from "./matchViewerFormatting";
import { MatchViewerSkillChips } from "./MatchViewerSkillChips";

export function MatchScoreBreakdownModal({
  breakdown,
  error,
  loading,
  onClose,
}: {
  breakdown: MatchScoreBreakdownView | null;
  error: string | null;
  loading: boolean;
  onClose: () => void;
}) {
  if (!breakdown && !error && !loading) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-slate-950/80 px-4 py-8 backdrop-blur-sm"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
      role="presentation"
    >
      <section className="panel max-h-[86vh] w-full max-w-3xl overflow-y-auto rounded border border-cyan-400/30 p-5 shadow-2xl shadow-cyan-950/50">
        <header className="flex items-start justify-between gap-4 border-b border-cyan-400/20 pb-4">
          <div>
            <p className="text-label text-xs font-semibold uppercase tracking-[0.18em]">
              Matchning
            </p>
            <h2 className="text-title mt-1 text-xl font-semibold">
              Poänganalys
            </h2>
          </div>
          <button
            type="button"
            className="rounded border border-cyan-400/40 px-3 py-1.5 text-sm text-cyan-100 transition hover:border-cyan-300 hover:bg-cyan-400/10"
            onClick={onClose}
            aria-label="Stäng fönster"
            title="Stäng fönster"
          >
            X
          </button>
        </header>

        {loading && (
          <div className="py-8 text-sm text-soft">Laddar matchdetaljer...</div>
        )}

        {!loading && error && (
          <div className="mt-4 rounded border border-red-400/40 bg-red-950/30 p-4 text-sm text-red-100">
            {error}
          </div>
        )}

        {!loading && breakdown && (
          <div className="mt-5 grid gap-5">
            <div className="grid gap-3 sm:grid-cols-4">
              <div className="panel-soft rounded p-3">
                <p className="text-label text-xs uppercase">Poäng</p>
                <p className="text-title mt-1 text-2xl font-semibold">
                  {breakdown.score}
                </p>
              </div>
              <div className="panel-soft rounded p-3">
                <p className="text-label text-xs uppercase">Bedömning</p>
                <p className="text-soft mt-1 font-semibold">
                  {translateScoreLabel(breakdown.scoreLabel)}
                </p>
              </div>
              <div className="panel-soft rounded p-3">
                <p className="text-label text-xs uppercase">Tröskelvärde</p>
                <p className="text-soft mt-1 font-semibold">
                  {breakdown.discoveryThreshold}p
                </p>
              </div>
              <div className="panel-soft rounded p-3">
                <p className="text-label text-xs uppercase">Beslut</p>
                <p className="text-soft mt-1 font-semibold">
                  {breakdown.passedDiscoveryThreshold
                    ? "Upptäckbar"
                    : "Blockerad"}
                </p>
              </div>
            </div>

            <p className="text-muted text-sm leading-6">
              {translateMatchBreakdownDecision(breakdown)}
            </p>

            <section className="grid gap-3">
              {breakdown.factors.map((factor) => (
                <article key={factor.factor} className="panel-soft rounded p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <h3 className="text-title font-semibold">
                        {translateMatchBreakdownFactor(factor.factor)}
                      </h3>
                      <p className="text-muted mt-1 text-sm">
                        {translateMatchBreakdownNote(factor.note)}
                      </p>
                    </div>
                    <span className="chip rounded-full px-3 py-1 text-xs font-medium">
                      +{factor.points}
                    </span>
                  </div>
                  <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-3">
                    <div>
                      <dt className="text-label text-xs uppercase">Matchade</dt>
                      <dd className="text-soft">
                        {factor.matchedCount}/{factor.requiredCount}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-label text-xs uppercase">
                        Poäng per träff
                      </dt>
                      <dd className="text-soft">+{factor.scorePerInstance}</dd>
                    </div>
                    <div>
                      <dt className="text-label text-xs uppercase">Status</dt>
                      <dd className="text-soft">
                        {factor.matched ? "Matchad" : "Ej matchad"}
                      </dd>
                    </div>
                  </dl>
                  <div className="mt-3">
                    <MatchViewerSkillChips
                      skills={factor.evidence ?? []}
                      emptyText="Inget underlag"
                    />
                  </div>
                </article>
              ))}
            </section>

            <section className="grid gap-2">
              <h3 className="text-section font-semibold">
                Saknade eller svaga faktorer
              </h3>
              <ul className="grid gap-2 text-sm text-soft">
                {breakdown.missingOrWeakFactors.map((item) => (
                  <li key={item} className="panel-soft rounded px-3 py-2">
                    {translateMissingOrWeakFactor(item)}
                  </li>
                ))}
              </ul>
            </section>

            <section>
              <h3 className="text-section mb-2 font-semibold">
                Matchade kompetenser
              </h3>
              <MatchViewerSkillChips
                skills={breakdown.matchedSkills ?? []}
                emptyText="Inga matchade kompetenser"
              />
            </section>
          </div>
        )}
      </section>
    </div>
  );
}
