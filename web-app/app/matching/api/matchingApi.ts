import { apiClient } from "~/shared/api/client";
import type { MatchScoreBreakdownView, MatchViewerView } from "../types";

type ApiPathId = string | number;

const encodedId = (id: ApiPathId) => encodeURIComponent(String(id));

export const matchingApiPaths = {
  matchViewer: "/api/ops/match-viewer",
  scoreBreakdown: (matchId: ApiPathId) =>
    `/api/ops/matches/${encodedId(matchId)}/score-breakdown`,
} as const;

export const matchingApi = {
  matchViewer(): Promise<MatchViewerView> {
    return apiClient.get<MatchViewerView>(matchingApiPaths.matchViewer);
  },

  inspectMatchScoreBreakdown(matchId: ApiPathId): Promise<MatchScoreBreakdownView> {
    return apiClient.get<MatchScoreBreakdownView>(matchingApiPaths.scoreBreakdown(matchId));
  },
};
