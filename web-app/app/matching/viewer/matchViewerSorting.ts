import type {
  MatchViewerCandidateMatchView,
  MatchViewerMissionSlotView,
  MatchViewerMissionView,
} from "~/matching/types";
import type { MatchSortMode } from "./matchViewerViewTypes";

function numericId(value: string | number): number | null {
  const parsed = typeof value === "number" ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function compareMatchIds(
  left: string | number,
  right: string | number,
): number {
  const leftNumber = numericId(left);
  const rightNumber = numericId(right);
  if (leftNumber !== null && rightNumber !== null) {
    return leftNumber - rightNumber;
  }
  return String(left).localeCompare(String(right), undefined, {
    numeric: true,
    sensitivity: "base",
  });
}

function scoreForSort(
  match: MatchViewerCandidateMatchView,
  fallback: number,
): number {
  return typeof match.score === "number" ? match.score : fallback;
}

function compareMatches(
  left: MatchViewerCandidateMatchView,
  right: MatchViewerCandidateMatchView,
  sortMode: MatchSortMode,
): number {
  if (sortMode === "SCORE_DESC") {
    return (
      scoreForSort(right, Number.NEGATIVE_INFINITY) -
        scoreForSort(left, Number.NEGATIVE_INFINITY) ||
      compareMatchIds(left.matchId, right.matchId)
    );
  }
  if (sortMode === "SCORE_ASC") {
    return (
      scoreForSort(left, Number.POSITIVE_INFINITY) -
        scoreForSort(right, Number.POSITIVE_INFINITY) ||
      compareMatchIds(left.matchId, right.matchId)
    );
  }
  return compareMatchIds(left.matchId, right.matchId);
}

export function sortedMatches(
  matches: MatchViewerCandidateMatchView[],
  sortMode: MatchSortMode,
): MatchViewerCandidateMatchView[] {
  return [...matches].sort((left, right) =>
    compareMatches(left, right, sortMode),
  );
}

export function compareSlotsByFirstMatch(
  left: MatchViewerMissionSlotView,
  right: MatchViewerMissionSlotView,
  sortMode: MatchSortMode,
): number {
  const leftMatch = sortedMatches(left.matches ?? [], sortMode)[0] ?? null;
  const rightMatch = sortedMatches(right.matches ?? [], sortMode)[0] ?? null;
  if (leftMatch && rightMatch) {
    return (
      compareMatches(leftMatch, rightMatch, sortMode) ||
      left.missionSlotNumber - right.missionSlotNumber
    );
  }
  if (leftMatch) return -1;
  if (rightMatch) return 1;
  return left.missionSlotNumber - right.missionSlotNumber;
}

export function compareMissionsByFirstMatch(
  left: MatchViewerMissionView,
  right: MatchViewerMissionView,
  sortMode: MatchSortMode,
): number {
  const leftMatch =
    sortedMatches(
      (left.slots ?? []).flatMap((slot) => slot.matches ?? []),
      sortMode,
    )[0] ?? null;
  const rightMatch =
    sortedMatches(
      (right.slots ?? []).flatMap((slot) => slot.matches ?? []),
      sortMode,
    )[0] ?? null;
  if (leftMatch && rightMatch) {
    return (
      compareMatches(leftMatch, rightMatch, sortMode) ||
      left.missionId - right.missionId
    );
  }
  if (leftMatch) return -1;
  if (rightMatch) return 1;
  return left.missionId - right.missionId;
}
