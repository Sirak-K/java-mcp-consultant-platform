import type {
  MatchNotificationMatchId,
  MatchNotificationPreviewView,
} from "../types";
import {
  displayTimestamp,
  previewMatchSummary,
  previewSnippet,
} from "./matchNotificationPreviewFormatting";

export function MatchNotificationPreviewList({
  previews,
  selectedPreview,
  onSelect,
}: {
  previews: MatchNotificationPreviewView[];
  selectedPreview: MatchNotificationPreviewView;
  onSelect: (matchId: MatchNotificationMatchId) => void;
}) {
  return (
    <aside className="panel h-fit rounded p-4 text-sm">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-section text-base font-medium">Inkorg</h2>
        <span className="text-muted text-xs">{previews.length} total</span>
      </div>
      <div className="grid gap-2">
        {previews.map((preview) => {
          const selected =
            String(preview.matchId) === String(selectedPreview.matchId);
          return (
            <button
              key={preview.matchId}
              type="button"
              className={`rounded border p-3 text-left transition ${
                selected
                  ? "border-cyan-400 bg-cyan-950/30"
                  : "border-slate-700 bg-slate-950/30 hover:border-slate-500"
              }`}
              onClick={() => onSelect(preview.matchId)}
            >
              <p className="text-title break-words text-sm font-semibold">
                {preview.subject || "(No subject)"}
              </p>
              <p className="text-muted mt-1 text-xs">
                {previewMatchSummary(preview)} |{" "}
                {displayTimestamp(preview.generatedAt)}
              </p>
              <p className="text-soft mt-2 line-clamp-3 text-xs leading-5">
                {previewSnippet(preview)}
              </p>
            </button>
          );
        })}
      </div>
    </aside>
  );
}
