import type {
  CandidatePresentationArtifactStatus,
  CandidatePresentationArtifactView,
} from "../types";

export function displayArtifactTimestamp(value?: string | null): string {
  return value?.trim() || "Ingen tidsstämpel";
}

export function displayArtifactValue(
  value?: string | number | null,
): string {
  if (value === null || value === undefined || value === "") return "-";
  return String(value);
}

export function presentationTitleForMatch(matchId: number): string {
  return `Presentation för Matchnings-ID: ${matchId}`;
}

export function artifactSubtitle(
  artifact: CandidatePresentationArtifactView,
): string {
  return `Matchning ${artifact.sourceCandidateToSlotMatchId} | Profil ${artifact.candProfileId} | Uppdrag ${artifact.missionId}`;
}

export function artifactStatusLabel(
  status: CandidatePresentationArtifactStatus,
): string {
  if (status === "PENDING_GENERATION") return "Väntar på generering";
  if (status === "GENERATED") return "Genererad";
  if (status === "OPS_REVIEW") return "OPS-granskning";
  return "Generering misslyckades";
}

export function artifactStatusDescription(
  status: CandidatePresentationArtifactStatus,
): string {
  if (status === "PENDING_GENERATION") {
    return "Väntar på generering innan OPS kan redigera, spara eller skicka.";
  }
  if (status === "GENERATED") {
    return "Genererat innehåll är redo för OPS-redigering och sparning.";
  }
  if (status === "OPS_REVIEW") {
    return "OPS-granskning är sparad och artefakten kan skickas via kundmejl-preview.";
  }
  return "Generering misslyckades. Kör om genereringen innan OPS-granskning kan fortsätta.";
}

export function canEditArtifactStatus(
  status: CandidatePresentationArtifactStatus,
): boolean {
  return status === "GENERATED" || status === "OPS_REVIEW";
}

export function shouldLogDisplayedArtifactStatus(
  status: CandidatePresentationArtifactStatus,
): boolean {
  return (
    status === "PENDING_GENERATION" ||
    status === "GENERATED" ||
    status === "GENERATION_FAILED"
  );
}

export function artifactEditActionTitle(
  status: CandidatePresentationArtifactStatus,
): string {
  if (canEditArtifactStatus(status)) return "Redigera kandidatpresentation";
  return artifactStatusDescription(status);
}

export function artifactStatusClass(
  status: CandidatePresentationArtifactStatus,
): string {
  if (status === "OPS_REVIEW") {
    return "border-cyan-300/70 bg-cyan-400/10 text-cyan-100";
  }
  if (status === "GENERATED") {
    return "border-violet-300/70 bg-violet-400/10 text-violet-100";
  }
  if (status === "GENERATION_FAILED") {
    return "border-red-300/70 bg-red-400/10 text-red-100";
  }
  return "border-yellow-300/70 bg-yellow-400/10 text-yellow-100";
}
