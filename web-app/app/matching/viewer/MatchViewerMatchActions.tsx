import type { MatchViewerCandidateMatchView } from "~/matching/types";
import type { PresentationGenerationFeedback } from "./matchViewerViewTypes";
import { formatScore, matchLabelClass } from "./matchViewerFormatting";

export function MatchViewerMatchActions({
  match,
  loading,
  matchCount,
  onInspectBreakdown,
  onStartPresentationGeneration,
  loadingPresentation,
  presentationFeedback,
}: {
  match: MatchViewerCandidateMatchView;
  loading: boolean;
  matchCount: number;
  onInspectBreakdown: (matchId: string | number) => void;
  onStartPresentationGeneration: (matchId: string | number) => void;
  loadingPresentation: boolean;
  presentationFeedback: PresentationGenerationFeedback | null;
}) {
  const feedbackClass =
    presentationFeedback?.tone === "error"
      ? "border-red-400/60 bg-red-950/40 text-red-100"
      : presentationFeedback?.tone === "success"
        ? "border-lime-300/50 bg-lime-400/10 text-lime-100"
        : "border-cyan-300/50 bg-cyan-400/10 text-cyan-100";

  return (
    <aside className="grid min-w-0 gap-3 rounded border border-cyan-400/20 bg-slate-950/20 p-3 lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] lg:items-center">
      <div className="flex min-w-0 flex-wrap items-center justify-center gap-2 lg:col-start-2">
        <span className="rounded border border-teal-300/80 bg-teal-400/10 px-3 py-1.5 text-center text-xs font-semibold text-teal-100">
          {matchCount} matchning{matchCount === 1 ? "" : "ar"}
        </span>
        <span
          className={`rounded border px-3 py-1.5 text-center text-xs font-semibold ${matchLabelClass(match)}`}
        >
          {formatScore(match)}
        </span>
        <button
          type="button"
          className="w-full rounded border border-cyan-400/40 px-3 py-2 text-xs font-semibold text-cyan-100 transition hover:border-cyan-300 hover:bg-cyan-400/10 disabled:cursor-wait disabled:opacity-60 sm:w-auto"
          onClick={() => onInspectBreakdown(match.matchId)}
          disabled={loading}
        >
          {loading ? "Laddar matchdetaljer..." : "Poänganalys"}
        </button>
      </div>
      <button
        type="button"
        className="w-full rounded border border-lime-300/45 px-3 py-2 text-xs font-semibold text-lime-100 transition hover:border-lime-200 hover:bg-lime-300/10 disabled:cursor-wait disabled:opacity-60 sm:w-auto lg:col-start-3 lg:justify-self-end"
        onClick={() => onStartPresentationGeneration(match.matchId)}
        disabled={loading || loadingPresentation}
      >
        {loadingPresentation
          ? "Startar generering..."
          : "Generera kandidatpresentation"}
      </button>
      {presentationFeedback && (
        <div
          className={`rounded border px-3 py-2 text-xs leading-5 lg:col-span-3 ${feedbackClass}`}
        >
          <p className="font-semibold">{presentationFeedback.message}</p>
          {presentationFeedback.detail && (
            <p className="mt-1 opacity-90">{presentationFeedback.detail}</p>
          )}
        </div>
      )}
    </aside>
  );
}
