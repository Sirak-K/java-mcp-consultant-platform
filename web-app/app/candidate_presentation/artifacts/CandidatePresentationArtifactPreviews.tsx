import type { CandidatePresentationArtifactView } from "../types";
import {
  customerReadyArtifactMarkdown,
  formatArtifactJson,
  opsReviewArtifactJson,
} from "./candidatePresentationArtifactContent";
import type { CandidatePresentationArtifactVersionMode } from "./useCandidatePresentationArtifacts";

interface CandidatePresentationArtifactPreviewsProps {
  artifact: CandidatePresentationArtifactView;
  artifactVersionMode: CandidatePresentationArtifactVersionMode;
  onArtifactVersionModeChange: (
    mode: CandidatePresentationArtifactVersionMode,
  ) => void;
}

function JsonPanel({ title, value }: { title: string; value: string }) {
  return (
    <section className="grid gap-3 border-t border-cyan-400/20 pt-5">
      <h2 className="text-section text-lg font-semibold">{title}</h2>
      <pre className="panel-soft max-h-80 overflow-auto rounded p-4 text-xs leading-5 text-muted">
        {formatArtifactJson(value)}
      </pre>
    </section>
  );
}

function MarkdownPreview({ markdown }: { markdown: string }) {
  const lines = markdown.split("\n");
  return (
    <section className="panel-soft grid min-h-96 gap-2 rounded p-5 text-sm leading-6 text-soft">
      {lines.map((line, index) => {
        const key = `${index}:${line}`;
        if (!line.trim()) return <div key={key} className="h-2" />;
        if (line.startsWith("# ")) {
          return (
            <h1 key={key} className="text-title text-2xl font-semibold">
              {line.slice(2)}
            </h1>
          );
        }
        if (line.startsWith("## ")) {
          return (
            <h2 key={key} className="text-section mt-4 text-lg font-semibold">
              {line.slice(3)}
            </h2>
          );
        }
        if (line.startsWith("### ")) {
          return (
            <h3
              key={key}
              className="text-label mt-3 text-sm font-semibold uppercase"
            >
              {line.slice(4)}
            </h3>
          );
        }
        if (line.startsWith("- ")) {
          return (
            <p key={key} className="pl-4">
              - {line.slice(2)}
            </p>
          );
        }
        return (
          <p key={key} className="whitespace-pre-wrap break-words">
            {line}
          </p>
        );
      })}
    </section>
  );
}

export function CandidatePresentationArtifactPreviews({
  artifact,
  artifactVersionMode,
  onArtifactVersionModeChange,
}: CandidatePresentationArtifactPreviewsProps) {
  return (
    <div className="mt-7 grid gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-cyan-400/20 pt-5">
        <p className="text-label text-xs font-semibold uppercase tracking-[0.18em]">
          Artefaktinnehåll
        </p>
        <div className="flex gap-2">
          <button
            type="button"
            className={`btn rounded px-3 py-1.5 text-xs font-medium ${
              artifactVersionMode === "customer" ? "btn-main" : "btn-soft"
            }`}
            onClick={() => onArtifactVersionModeChange("customer")}
          >
            Kundversion
          </button>
          <button
            type="button"
            className={`btn rounded px-3 py-1.5 text-xs font-medium ${
              artifactVersionMode === "ops" ? "btn-main" : "btn-soft"
            }`}
            onClick={() => onArtifactVersionModeChange("ops")}
          >
            OPS-granskning
          </button>
        </div>
      </div>
      {artifactVersionMode === "customer" ? (
        <MarkdownPreview markdown={customerReadyArtifactMarkdown(artifact)} />
      ) : (
        <JsonPanel title="OPS-granskning" value={opsReviewArtifactJson(artifact)} />
      )}
    </div>
  );
}
