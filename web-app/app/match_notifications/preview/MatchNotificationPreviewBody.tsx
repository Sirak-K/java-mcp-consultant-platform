import type { MatchNotificationPreviewView } from "../types";
import { matchNotificationMailFrameDocument } from "./matchNotificationMailFrame";
import {
  displayTimestamp,
  previewMatchSummary,
} from "./matchNotificationPreviewFormatting";

export type MatchNotificationPreviewBodyMode = "preview" | "json";

export function MatchNotificationPreviewBody({
  preview,
  bodyMode,
  onBodyModeChange,
}: {
  preview: MatchNotificationPreviewView;
  bodyMode: MatchNotificationPreviewBodyMode;
  onBodyModeChange: (bodyMode: MatchNotificationPreviewBodyMode) => void;
}) {
  const hasHtmlBody = Boolean(preview.htmlBody?.trim());
  const jsonBody = JSON.stringify(preview, null, 2);

  return (
    <article className="panel overflow-hidden rounded text-sm">
      <header className="border-b border-slate-700 p-5">
        <p className="text-label mb-2 text-xs font-semibold uppercase">Ämne</p>
        <h2 className="text-title break-words text-xl font-semibold">
          {preview.subject || "(No subject)"}
        </h2>
      </header>

      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-700 px-5 py-3">
        <div className="text-muted text-xs">
          {previewMatchSummary(preview)}
          {preview.generatedAt
            ? ` | Received ${displayTimestamp(preview.generatedAt)}`
            : ""}
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            className={`btn rounded px-3 py-1.5 text-xs font-medium ${
              bodyMode === "preview" ? "btn-main" : "btn-soft"
            }`}
            onClick={() => onBodyModeChange("preview")}
          >
            Preview
          </button>
          <button
            type="button"
            className={`btn rounded px-3 py-1.5 text-xs font-medium ${
              bodyMode === "json" ? "btn-main" : "btn-soft"
            }`}
            onClick={() => onBodyModeChange("json")}
          >
            JSON
          </button>
        </div>
      </div>

      <section className="bg-slate-950/30 p-5">
        {bodyMode === "json" ? (
          <pre className="text-title min-h-96 whitespace-pre-wrap break-words rounded bg-slate-950/50 p-5 font-mono text-xs leading-5">
            {jsonBody}
          </pre>
        ) : hasHtmlBody && preview.htmlBody ? (
          <iframe
            title="Match notification email HTML preview"
            className="h-[640px] w-full rounded border border-slate-700 bg-slate-950"
            sandbox=""
            srcDoc={matchNotificationMailFrameDocument(preview.htmlBody)}
          />
        ) : (
          <pre className="text-title min-h-96 whitespace-pre-wrap break-words rounded bg-slate-950/50 p-5 font-sans text-sm leading-6">
            {preview.textBody?.trim() || "No preview body available."}
          </pre>
        )}
      </section>
    </article>
  );
}
