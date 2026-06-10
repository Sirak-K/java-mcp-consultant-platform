import type { CandidatePresentationArtifactView } from "../types";
import {
  artifactStatusClass,
  artifactStatusLabel,
  artifactSubtitle,
  displayArtifactTimestamp,
  presentationTitleForMatch,
} from "./candidatePresentationArtifactDisplay";

interface CandidatePresentationArtifactsListProps {
  artifacts: CandidatePresentationArtifactView[];
  selectedArtifactId?: number | null;
  onSelectArtifact: (artifact: CandidatePresentationArtifactView) => void;
}

export function CandidatePresentationArtifactsList({
  artifacts,
  selectedArtifactId,
  onSelectArtifact,
}: CandidatePresentationArtifactsListProps) {
  return (
    <aside className="panel grid content-start gap-3 rounded p-4">
      <div>
        <p className="text-label text-xs font-semibold uppercase tracking-[0.18em]">
          Artefakter
        </p>
        <p className="text-soft text-sm">{artifacts.length} totalt</p>
      </div>
      <div className="grid gap-2">
        {artifacts.map((artifact) => (
          <button
            key={artifact.id}
            type="button"
            className={`panel-soft rounded p-3 text-left transition ${selectedArtifactId === artifact.id ? "border-cyan-300/70" : ""}`}
            onClick={() => onSelectArtifact(artifact)}
          >
            <div className="flex items-start justify-between gap-3">
              <span className="text-title text-sm font-semibold">
                {presentationTitleForMatch(
                  artifact.sourceCandidateToSlotMatchId,
                )}
              </span>
              <span
                className={`rounded-full border px-2 py-1 text-[11px] font-semibold ${artifactStatusClass(artifact.artifactStatus)}`}
              >
                {artifactStatusLabel(artifact.artifactStatus)}
              </span>
            </div>
            <p className="text-muted mt-2 text-xs">
              {artifactSubtitle(artifact)}
            </p>
            <p className="text-soft mt-1 text-xs">
              Uppdaterad {displayArtifactTimestamp(artifact.updatedAt)}
            </p>
          </button>
        ))}
      </div>
    </aside>
  );
}
