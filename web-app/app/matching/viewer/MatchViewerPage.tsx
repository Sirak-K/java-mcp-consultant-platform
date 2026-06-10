import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { ErrorPlaceholder, LoadingPlaceholder } from "~/shared/components";
import { candidatePresentationApi } from "~/candidate_presentation/api/candidatePresentationApi";
import { matchingApi, matchingApiPaths } from "~/matching/api/matchingApi";
import { opsUiEventLogger } from "~/shared/observability/opsUiEventLogger";
import { MatchScoreBreakdownModal } from "./MatchScoreBreakdownModal";
import { MatchViewerMissionPanel } from "./MatchViewerMissionPanel";
import { displayDate, isGenerationStartView } from "./matchViewerFormatting";
import {
  compareMissionsByFirstMatch,
  compareSlotsByFirstMatch,
  sortedMatches,
} from "./matchViewerSorting";
import {
  MATCH_SORT_OPTIONS,
  type MatchSortMode,
  type PresentationGenerationFeedback,
} from "./matchViewerViewTypes";
import type {
  MatchScoreBreakdownView,
  MatchViewerView,
} from "~/matching/types";
import type { ApiError } from "~/shared/api/apiErrors";

const contract = `GET ${matchingApiPaths.matchViewer} -> MatchViewerView`;

export default function MatchViewerPage() {
  const navigate = useNavigate();
  const [viewer, setViewer] = useState<MatchViewerView | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [breakdown, setBreakdown] = useState<MatchScoreBreakdownView | null>(
    null,
  );
  const [breakdownError, setBreakdownError] = useState<string | null>(null);
  const [loadingBreakdownMatchId, setLoadingBreakdownMatchId] = useState<
    string | number | null
  >(null);
  const [loadingPresentationMatchId, setLoadingPresentationMatchId] = useState<
    string | number | null
  >(null);
  const [presentationFeedback, setPresentationFeedback] =
    useState<PresentationGenerationFeedback | null>(null);
  const [matchSortMode, setMatchSortMode] =
    useState<MatchSortMode>("SCORE_DESC");

  useEffect(() => {
    let mounted = true;
    matchingApi
      .matchViewer()
      .then((result) => {
        if (mounted) {
          setViewer(result);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (mounted) {
          setError(err.message ?? "Could not load Match Viewer data.");
        }
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const inspectBreakdown = (matchId: string | number) => {
    setLoadingBreakdownMatchId(matchId);
    setBreakdown(null);
    setBreakdownError(null);
    matchingApi
      .inspectMatchScoreBreakdown(matchId)
      .then((result) => {
        setBreakdown(result);
      })
      .catch((err: ApiError) => {
        setBreakdownError(err.message ?? "Kunde inte läsa in matchdetaljer.");
      })
      .finally(() => {
        setLoadingBreakdownMatchId(null);
      });
  };

  const closeBreakdown = () => {
    setBreakdown(null);
    setBreakdownError(null);
    setLoadingBreakdownMatchId(null);
  };

  const startPresentationGeneration = (matchId: string | number) => {
    const matchKey = String(matchId);
    opsUiEventLogger.log({
      eventName: "match_viewer_candidate_presentation_generation_clicked",
      route: "/ops/matches-viewer",
      details: { matchId: matchKey },
    });
    setPresentationFeedback({
      matchId: matchKey,
      tone: "info",
      message: "Startar generering av kandidatpresentation...",
    });
    setLoadingPresentationMatchId(matchId);
    candidatePresentationApi
      .startGeneration(matchId)
      .then((result) => {
        opsUiEventLogger.log({
          eventName: "candidate_presentation_generation_start_api_accepted",
          route: "/ops/matches-viewer",
          details: {
            matchId: matchKey,
            artifactId: result.artifactId ?? null,
            artifactStatus: result.artifactStatus ?? null,
            runId: result.runId,
          },
        });
        setPresentationFeedback({
          matchId: matchKey,
          tone: "success",
          message:
            "Generering accepterad. Öppnar kandidatpresentationsartefakt...",
          detail: result.runId ? `Run ID: ${result.runId}` : undefined,
        });
        if (result.artifactId != null) {
          navigate(
            `/ops/candidate-presentation-artifacts?artifactId=${encodeURIComponent(String(result.artifactId))}`,
          );
        }
      })
      .catch((err: ApiError) => {
        const result = isGenerationStartView(err.body) ? err.body : null;
        opsUiEventLogger.log({
          eventName:
            err.status === 409
              ? "candidate_presentation_generation_start_api_blocked"
              : "candidate_presentation_generation_start_api_failed",
          route: "/ops/matches-viewer",
          details: {
            matchId: matchKey,
            errorCode: err.code,
            status: err.status ?? null,
            generationStartStatus: result?.generationStartStatus ?? null,
          },
        });
        setPresentationFeedback({
          matchId: matchKey,
          tone: "error",
          message:
            result?.message ??
            err.message ??
            "Generering av kandidatpresentation kunde inte starta.",
        });
      })
      .finally(() => {
        setLoadingPresentationMatchId(null);
      });
  };

  const missions = viewer?.missions ?? [];
  const sortedMissions = useMemo(
    () =>
      missions
        .map((mission) => ({
          ...mission,
          slots: [...(mission.slots ?? [])]
            .map((slot) => ({
              ...slot,
              matches: sortedMatches(slot.matches ?? [], matchSortMode),
            }))
            .sort((left, right) =>
              compareSlotsByFirstMatch(left, right, matchSortMode),
            ),
        }))
        .sort((left, right) =>
          compareMissionsByFirstMatch(left, right, matchSortMode),
        ),
    [missions, matchSortMode],
  );
  const matchCount = useMemo(
    () =>
      missions.reduce(
        (sum, mission) =>
          sum +
          (mission.slots ?? []).reduce(
            (slotSum, slot) => slotSum + (slot.matches?.length ?? 0),
            0,
          ),
        0,
      ),
    [missions],
  );

  if (loading) {
    return <LoadingPlaceholder />;
  }

  return (
    <div className="grid gap-5 p-3 sm:p-4 lg:gap-6 lg:p-6">
      <header className="max-w-4xl">
        <div className="max-w-4xl">
          <h1 className="text-title mt-2 text-2xl font-semibold">
            Alla Matchningar
          </h1>
          <p className="text-muted mt-2 text-sm leading-6">
            Matchningar graderas utifrån kompetens och geografisk lämplighet.
          </p>
        </div>
      </header>

      {error && (
        <div>
          <ErrorPlaceholder message={error} />
          <div className="panel mt-4 rounded p-4 text-sm">
            <h2 className="text-section mb-2 font-medium">
              Expected API contract
            </h2>
            <p className="text-muted">{contract}</p>
          </div>
        </div>
      )}

      {!error && missions.length === 0 && (
        <div className="panel rounded p-6 text-sm">
          <p className="text-soft">No grouped match data exists yet.</p>
        </div>
      )}

      {!error && missions.length > 0 && (
        <section className="panel-soft rounded p-3 sm:p-4">
          <div className="grid gap-4 xl:grid-cols-[minmax(12rem,16rem)_minmax(0,1fr)_auto] xl:items-center">
            <div className="grid gap-1">
              <p className="text-label text-xs font-semibold uppercase tracking-[0.18em]">
                Filtrering & sortering
              </p>
              <p className="text-muted text-xs">
                Välj hur matchobjekten prioriteras.
              </p>
            </div>
            <div
              className="flex flex-wrap gap-2"
              role="group"
              aria-label="Sortering av matchningar"
            >
              {MATCH_SORT_OPTIONS.map((option) => {
                const active = option.value === matchSortMode;
                return (
                  <button
                    key={option.value}
                    type="button"
                    className={`min-w-[13rem] flex-1 rounded border px-2.5 py-2 text-xs font-semibold leading-4 transition sm:flex-none sm:px-3 ${
                      active
                        ? "border-lime-300 bg-lime-300/15 text-lime-100"
                        : "border-cyan-400/35 text-cyan-100 hover:border-cyan-300 hover:bg-cyan-400/10"
                    }`}
                    aria-pressed={active}
                    onClick={() => setMatchSortMode(option.value)}
                  >
                    {option.label}
                  </button>
                );
              })}
            </div>
            <dl className="grid gap-2 rounded border border-cyan-400/20 bg-slate-950/30 p-3 text-xs sm:grid-cols-3 xl:min-w-[24rem]">
              <div>
                <dt className="text-label font-semibold uppercase">
                  Antal Uppdrag
                </dt>
                <dd className="text-soft mt-1">{missions.length}</dd>
              </div>
              <div>
                <dt className="text-label font-semibold uppercase">
                  Matchningar
                </dt>
                <dd className="text-soft mt-1">{matchCount}</dd>
              </div>
              <div>
                <dt className="text-label font-semibold uppercase">
                  Senaste uppdaterad
                </dt>
                <dd className="text-muted mt-1">
                  {displayDate(viewer?.generatedAt)}
                </dd>
              </div>
            </dl>
          </div>
        </section>
      )}

      {!error && missions.length > 0 && (
        <div className="grid gap-6">
          {sortedMissions.map((mission) => (
            <MatchViewerMissionPanel
              key={mission.missionId}
              mission={mission}
              matchSortMode={matchSortMode}
              loadingBreakdownMatchId={loadingBreakdownMatchId}
              loadingPresentationMatchId={loadingPresentationMatchId}
              presentationFeedback={presentationFeedback}
              onInspectBreakdown={inspectBreakdown}
              onStartPresentationGeneration={startPresentationGeneration}
            />
          ))}
        </div>
      )}

      <MatchScoreBreakdownModal
        breakdown={breakdown}
        error={breakdownError}
        loading={loadingBreakdownMatchId != null}
        onClose={closeBreakdown}
      />
    </div>
  );
}
