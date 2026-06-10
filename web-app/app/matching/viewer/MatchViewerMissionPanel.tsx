import type {
  MatchViewerMissionSlotView,
  MatchViewerMissionView,
} from "~/matching/types";
import { sortedMatches } from "./matchViewerSorting";
import type { MatchSortMode, PresentationGenerationFeedback } from "./matchViewerViewTypes";
import { MatchViewerCandidateCard } from "./MatchViewerCandidateCard";
import { MatchViewerMatchActions } from "./MatchViewerMatchActions";
import { MatchViewerMissionSlotSummary } from "./MatchViewerMissionSlotSummary";

function MatchViewerMissionSlotPanel({
  mission,
  slot,
  matchSortMode,
  loadingBreakdownMatchId,
  loadingPresentationMatchId,
  presentationFeedback,
  onInspectBreakdown,
  onStartPresentationGeneration,
}: {
  mission: MatchViewerMissionView;
  slot: MatchViewerMissionSlotView;
  matchSortMode: MatchSortMode;
  loadingBreakdownMatchId: string | number | null;
  loadingPresentationMatchId: string | number | null;
  presentationFeedback: PresentationGenerationFeedback | null;
  onInspectBreakdown: (matchId: string | number) => void;
  onStartPresentationGeneration: (matchId: string | number) => void;
}) {
  const matches = sortedMatches(slot.matches ?? [], matchSortMode);
  const compactCandidates = matches.length > 1;

  return (
    <section className="grid gap-4">
      {matches.length === 0 ? (
        <div className="grid min-w-0 gap-4">
          <MatchViewerMissionSlotSummary mission={mission} slot={slot} />
          <div className="panel rounded p-4 text-sm">
            <p className="text-soft">No candidate matches for this slot.</p>
          </div>
        </div>
      ) : (
        <div className={compactCandidates ? "grid gap-8" : "grid gap-6"}>
          {matches.map((match) => (
            <article
              key={`${match.matchId}:${match.candidateCard.candidateProfileId}`}
              className="grid min-w-0 gap-4 rounded border border-cyan-400/20 bg-slate-950/20 p-3 sm:p-4"
            >
              <MatchViewerMatchActions
                match={match}
                loading={
                  String(loadingBreakdownMatchId) === String(match.matchId)
                }
                loadingPresentation={
                  String(loadingPresentationMatchId) === String(match.matchId)
                }
                presentationFeedback={
                  presentationFeedback?.matchId === String(match.matchId)
                    ? presentationFeedback
                    : null
                }
                matchCount={matches.length}
                onInspectBreakdown={onInspectBreakdown}
                onStartPresentationGeneration={onStartPresentationGeneration}
              />
              <div className="grid min-w-0 gap-4 2xl:grid-cols-2">
                <MatchViewerMissionSlotSummary mission={mission} slot={slot} />
                <MatchViewerCandidateCard candidate={match.candidateCard} />
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export function MatchViewerMissionPanel({
  mission,
  matchSortMode,
  loadingBreakdownMatchId,
  loadingPresentationMatchId,
  presentationFeedback,
  onInspectBreakdown,
  onStartPresentationGeneration,
}: {
  mission: MatchViewerMissionView;
  matchSortMode: MatchSortMode;
  loadingBreakdownMatchId: string | number | null;
  loadingPresentationMatchId: string | number | null;
  presentationFeedback: PresentationGenerationFeedback | null;
  onInspectBreakdown: (matchId: string | number) => void;
  onStartPresentationGeneration: (matchId: string | number) => void;
}) {
  const slots = mission.slots ?? [];

  return (
    <article className="panel rounded p-3 sm:p-4 lg:p-5">
      <div className="grid gap-5">
        {slots.length === 0 ? (
          <div className="panel-soft rounded p-4 text-sm">
            <p className="text-soft">No mission slots in response.</p>
          </div>
        ) : (
          slots.map((slot) => (
            <MatchViewerMissionSlotPanel
              key={`${mission.missionId}:${slot.missionSlotId ?? slot.missionSlotNumber}`}
              mission={mission}
              slot={slot}
              matchSortMode={matchSortMode}
              loadingBreakdownMatchId={loadingBreakdownMatchId}
              loadingPresentationMatchId={loadingPresentationMatchId}
              presentationFeedback={presentationFeedback}
              onInspectBreakdown={onInspectBreakdown}
              onStartPresentationGeneration={onStartPresentationGeneration}
            />
          ))
        )}
      </div>
    </article>
  );
}
