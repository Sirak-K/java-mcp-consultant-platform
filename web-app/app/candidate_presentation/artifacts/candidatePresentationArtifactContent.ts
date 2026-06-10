import type {
  CandidatePresentationArtifactEditInput,
  CandidatePresentationArtifactView,
} from "../types";
import { presentationTitleForMatch } from "./candidatePresentationArtifactDisplay";

export function formatArtifactJson(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw || "{}"), null, 2);
  } catch {
    return raw;
  }
}

export function parseArtifactJson(raw: string): unknown {
  try {
    return JSON.parse(raw || "{}");
  } catch {
    return raw;
  }
}

export function isValidArtifactJson(raw: string): boolean {
  try {
    JSON.parse(raw || "{}");
    return true;
  } catch {
    return false;
  }
}

export function buildArtifactEditState(
  artifact: CandidatePresentationArtifactView,
): CandidatePresentationArtifactEditInput {
  return {
    customerFacingContentJson: formatArtifactJson(
      artifact.customerFacingContentJson,
    ),
    opsReviewContentJson: formatArtifactJson(artifact.opsReviewContentJson),
    evidenceTraceJson: formatArtifactJson(artifact.evidenceTraceJson),
  };
}

const sectionTitleLabels: Record<string, string> = {
  "Candidate Presentation Overview": "Kandidatpresentation - översikt",
  "Mission Overview": "Uppdragsöversikt",
  "Mission Slot Match Details": "Matchningsdetaljer för uppdragsposition",
  "Matched Role": "Matchad roll",
  "Matched Primary Skills": "Matchade primära kompetenser",
  "Matched Secondary Skills": "Matchade sekundära kompetenser",
  "About The Matching Candidate": "Om kandidaten",
  "Role Fit": "Rollmatchning",
  "Work Mode Fit": "Matchning för arbetsläge",
  "Experience And Availability": "Erfarenhet och tillgänglighet",
  Certifications: "Certifikat",
  "Relevant Work Experience": "Relevant arbetslivserfarenhet",
  "OPS Review Notes": "OPS-granskningsnoteringar",
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function titleFromKey(key: string): string {
  const title = key
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/^./, (letter) => letter.toUpperCase());
  return sectionTitleLabels[title] ?? title;
}

function scalarPreviewText(value: unknown): string {
  if (value === null || value === undefined || value === "") {
    return "";
  }
  if (typeof value === "string") {
    return value.trim();
  }
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  return JSON.stringify(value, null, 2);
}

function markdownHeading(level: number, title: string): string {
  return `${"#".repeat(Math.min(Math.max(level, 1), 6))} ${title}`;
}

function jsonValueToMarkdown(value: unknown, headingLevel: number): string {
  if (Array.isArray(value)) {
    if (value.length === 0) return "";
    return value
      .map((entry) => {
        if (isRecord(entry) || Array.isArray(entry)) {
          return jsonValueToMarkdown(entry, headingLevel);
        }
        const text = scalarPreviewText(entry);
        return text ? `- ${text}` : "";
      })
      .filter(Boolean)
      .join("\n");
  }

  if (isRecord(value)) {
    const sections = Object.entries(value)
      .map(([key, entry]) => {
        const body = jsonValueToMarkdown(entry, headingLevel + 1);
        const scalar = body || scalarPreviewText(entry);
        if (!scalar) return "";
        return `${markdownHeading(headingLevel, titleFromKey(key))}\n\n${scalar}`;
      })
      .filter(Boolean);
    return sections.join("\n\n");
  }

  return scalarPreviewText(value);
}

export function customerReadyArtifactMarkdown(
  artifact: CandidatePresentationArtifactView,
): string {
  const customerFacing = parseArtifactJson(artifact.customerFacingContentJson);
  return (
    jsonValueToMarkdown(customerFacing, 1) ||
    `# ${presentationTitleForMatch(artifact.sourceCandidateToSlotMatchId)}`
  );
}

export function opsReviewArtifactJson(
  artifact: CandidatePresentationArtifactView,
): string {
  return JSON.stringify(
    {
      metadata: {
        artifactId: artifact.id,
        sourceCandidateToSlotMatchId: artifact.sourceCandidateToSlotMatchId,
        candProfileId: artifact.candProfileId,
        missionId: artifact.missionId,
        missionSlotId: artifact.missionSlotId,
        artifactStatus: artifact.artifactStatus,
        presentationTitle: presentationTitleForMatch(
          artifact.sourceCandidateToSlotMatchId,
        ),
        createdAt: artifact.createdAt,
        updatedAt: artifact.updatedAt,
      },
      customerReadyMarkdown: customerReadyArtifactMarkdown(artifact),
      opsReviewContent: parseArtifactJson(artifact.opsReviewContentJson),
      evidenceTrace: parseArtifactJson(artifact.evidenceTraceJson),
    },
    null,
    2,
  );
}
