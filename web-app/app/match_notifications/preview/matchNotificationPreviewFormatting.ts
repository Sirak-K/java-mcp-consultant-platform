import type {
  MatchNotificationMatchId,
  MatchNotificationPreviewView,
  MatchNotificationSendView,
} from "../types";

export function displayTimestamp(value?: string | null): string {
  return value?.replace("T", " ").replace("Z", "") ?? "No timestamp";
}

function displayValue(value: unknown): string {
  if (value === null || value === undefined || value === "") {
    return "";
  }
  if (typeof value === "object") {
    return JSON.stringify(value);
  }
  return String(value);
}

export function previewSnippet(
  preview: MatchNotificationPreviewView,
): string {
  return (
    preview.evidenceBrief?.trim() ||
    preview.textBody
      ?.trim()
      .split("\n")
      .find((line) => line.trim()) ||
    "No preview text"
  );
}

function matchIdLabel(matchId: MatchNotificationMatchId): string {
  return `Match ID ${matchId}`;
}

export function candidateProfileLabel(
  candidateProfileId: number | null | undefined,
): string {
  return candidateProfileId ? `Kandidat #${candidateProfileId}` : "";
}

export function previewMatchSummary(
  preview: MatchNotificationPreviewView,
): string {
  const ids =
    preview.matchIds?.filter(
      (matchId) => matchId !== null && matchId !== undefined,
    ) ?? [];
  const count = preview.groupedMatchCount ?? ids.length;
  const profileText = preview.candidateProfileId
    ? ` | ${candidateProfileLabel(preview.candidateProfileId)}`
    : "";
  if (count > 1 || ids.length > 1) {
    return `${count} matchningar${ids.length > 0 ? ` | Match ID ${ids.join(", ")}` : ""}${profileText}`;
  }
  return `${matchIdLabel(preview.matchId)}${profileText}`;
}

export function sendResultRows(
  result: MatchNotificationSendView,
): Array<[string, string]> {
  return [
    ["Status", result.status],
    ["Delivery", result.deliveryStatus],
    ["Match", previewMatchSummary(result)],
    ["Kandidat", candidateProfileLabel(result.candidateProfileId)],
    ["Transport", result.transport],
    ["To", result.to],
    ["Ref", result.transportRef],
    ["Delivery ID", result.deliveryId],
    ["Sent", result.sentAt],
  ]
    .map(([label, value]) => [label, displayValue(value)] as [string, string])
    .filter(([, value]) => value.length > 0);
}
