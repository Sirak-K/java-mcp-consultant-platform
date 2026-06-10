export type MatchSortMode = "MATCH_ID" | "SCORE_DESC" | "SCORE_ASC";
export type PresentationGenerationFeedbackTone = "error" | "info" | "success";

export interface PresentationGenerationFeedback {
  matchId: string;
  tone: PresentationGenerationFeedbackTone;
  message: string;
  detail?: string;
}

export const MATCH_SORT_OPTIONS: { value: MatchSortMode; label: string }[] = [
  { value: "MATCH_ID", label: "Sortering - Matchnings-ID först" },
  { value: "SCORE_DESC", label: "Sortering - Bästa matchningar först" },
  { value: "SCORE_ASC", label: "Sortering - Sämsta matchningar först" },
];
