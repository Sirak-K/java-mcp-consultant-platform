import type { CandidatePresentationGenerationStartView } from "~/candidate_presentation/types";
import type {
  MatchScoreBreakdownView,
  MatchViewerCandidateMatchView,
} from "~/matching/types";

export function hasText(value: string | null | undefined): value is string {
  return Boolean(value?.trim());
}

function displayText(
  value: string | null | undefined,
  fallback = "N/A",
): string {
  return hasText(value) ? value.trim() : fallback;
}

export function displayCardText(
  value: string | null | undefined,
  fallback = "N/A",
): string {
  return displayText(value, fallback).replace(/_/g, " ");
}

export function displayMatchSkillLevel(value: string | null | undefined): string {
  return displayCardText(value).replace(/\s*\([^)]*\)\s*$/, "");
}

const SCORE_LABEL_TRANSLATIONS: Record<string, string> = {
  "Overqualified Match": "Överkvalificerad Matchning",
  "Great Match": "Utmärkt Matchning",
  "Good Match": "Bra Matchning",
  "OK Match": "OK Matchning",
  "Weak Match": "Svag Matchning",
  Otillräckligt: "Otillräcklig Matchning",
};

const MATCH_BREAKDOWN_FACTOR_TRANSLATIONS: Record<string, string> = {
  Role: "Roll",
  "Senior Skills": "Seniora kompetenser",
  "Junior Skills": "Juniora kompetenser",
  "Work Mode": "Arbetsform",
};

const MATCH_BREAKDOWN_NOTE_TRANSLATIONS: Record<string, string> = {
  "Cand profile role and role experience fulfilled the mission slot requirement.":
    "Kandidatprofilens roll och rollerfarenhet uppfyllde uppdragets rollkrav.",
  "Cand profile role or role experience did not fulfill the mission slot requirement.":
    "Kandidatprofilens roll eller rollerfarenhet uppfyllde inte uppdragets rollkrav.",
  "Required skill levels that use the canonical +5 skill score.":
    "Kravkompetenser på senior nivå som använder den kanoniska +5-poängen.",
  "Required junior-level skill matches that use the canonical +2 skill score.":
    "Kravkompetenser på junior nivå som använder den kanoniska +2-poängen.",
  "Cand profile work mode matched the cust mission work mode.":
    "Kandidatprofilens arbetsform matchade uppdragets arbetsform.",
  "Cand profile work mode did not match the cust mission work mode.":
    "Kandidatprofilens arbetsform matchade inte uppdragets arbetsform.",
};

const MISSING_OR_WEAK_FACTOR_TRANSLATIONS: Record<string, string> = {
  "Role match or required role experience was not fulfilled.":
    "Rollmatchning eller krävd rollerfarenhet uppfylldes inte.",
  "Work mode was not matched.": "Arbetsform matchades inte.",
  "Skill score attribution is partially limited by the persisted snapshot format.":
    "Kompetenspoängens attribution är delvis begränsad av det sparade snapshot-formatet.",
  "No missing score factors in the available match snapshot.":
    "Inga saknade poängfaktorer i tillgängligt matchningssnapshot.",
};

export function translateScoreLabel(value: string): string {
  return SCORE_LABEL_TRANSLATIONS[value] ?? value;
}

export function translateMatchBreakdownFactor(value: string): string {
  return MATCH_BREAKDOWN_FACTOR_TRANSLATIONS[value] ?? value;
}

export function translateMatchBreakdownNote(value: string): string {
  return MATCH_BREAKDOWN_NOTE_TRANSLATIONS[value] ?? value;
}

export function translateMatchBreakdownDecision(
  breakdown: MatchScoreBreakdownView,
): string {
  if (breakdown.passedDiscoveryThreshold) {
    return `Matchningen finns eftersom sparad kanonisk poäng ${breakdown.score} når tröskelvärdet ${breakdown.discoveryThreshold} (${translateScoreLabel(breakdown.scoreLabel)}).`;
  }

  return `Matchningen når inte tröskelvärdet ${breakdown.discoveryThreshold} och ska inte behandlas som upptäckbar.`;
}

export function translateMissingOrWeakFactor(value: string): string {
  const skillPrefix = "Required skill not matched: ";
  if (value.startsWith(skillPrefix)) {
    return `Kravkompetens ej matchad: ${value.slice(skillPrefix.length)}`;
  }
  return MISSING_OR_WEAK_FACTOR_TRANSLATIONS[value] ?? value;
}

export function displayDate(value: string | null | undefined): string {
  return hasText(value) ? value.replace("T", " ").replace("Z", "") : "N/A";
}

export function isGenerationStartView(
  value: unknown,
): value is CandidatePresentationGenerationStartView {
  return Boolean(
    value &&
    typeof value === "object" &&
    "generationStartStatus" in value &&
    "matchId" in value,
  );
}

export function formatScore(match: MatchViewerCandidateMatchView): string {
  if (hasText(match.scoreLabel)) {
    return translateScoreLabel(match.scoreLabel);
  }
  if (typeof match.score === "number") {
    return `${Math.round(match.score * 100)}%`;
  }
  return "N/A";
}

function scorePercent(match: MatchViewerCandidateMatchView): number | null {
  if (typeof match.score !== "number") {
    return null;
  }
  return match.score > 1 ? match.score : Math.round(match.score * 100);
}

export function matchLabelClass(match: MatchViewerCandidateMatchView): string {
  const label = hasText(match.scoreLabel)
    ? translateScoreLabel(match.scoreLabel).toLocaleLowerCase()
    : "";
  const score = scorePercent(match);
  if (
    label.includes("överkvalificerad") ||
    label.includes("overqualified") ||
    (!label && score != null && score > 100)
  ) {
    return "border-fuchsia-200 bg-gradient-to-r from-fuchsia-400/30 via-cyan-300/20 to-lime-300/25 text-fuchsia-50 shadow-lg shadow-fuchsia-500/20 animate-pulse";
  }
  if (
    label.includes("utmärkt") ||
    label.includes("great") ||
    (!label && score != null && score >= 90)
  ) {
    return "border-lime-200 bg-gradient-to-r from-lime-400/30 via-emerald-300/20 to-cyan-300/25 text-lime-50 shadow-lg shadow-lime-500/20 animate-pulse";
  }
  if (
    label.includes("bra") ||
    label.includes("good") ||
    (!label && score != null && score >= 80)
  ) {
    return "border-emerald-300/80 bg-emerald-400/15 text-emerald-50 shadow-md shadow-emerald-500/10";
  }
  if (label.includes("ok") || (!label && score != null && score >= 75)) {
    return "border-sky-300/80 bg-sky-400/15 text-sky-50";
  }
  if (label.includes("svag") || label.includes("weak")) {
    return "border-yellow-300/80 bg-yellow-400/15 text-yellow-50";
  }
  return "border-red-300/80 bg-red-500/15 text-red-50";
}
