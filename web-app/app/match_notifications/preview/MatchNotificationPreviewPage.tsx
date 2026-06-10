import { useEffect, useMemo, useState } from "react";
import { matchNotificationsApi } from "~/match_notifications/api/matchNotificationsApi";
import type {
  MatchNotificationMatchId,
  MatchNotificationPreviewView,
  MatchNotificationSendView,
} from "~/match_notifications/types";
import type { ApiError } from "~/shared/api/apiErrors";
import {
  MatchNotificationPreviewBody,
  type MatchNotificationPreviewBodyMode,
} from "./MatchNotificationPreviewBody";
import { MatchNotificationPreviewList } from "./MatchNotificationPreviewList";
import { MatchNotificationSendResultPanel } from "./MatchNotificationSendResultPanel";

function previewErrorMessage(error: unknown): string {
  const apiError = error as ApiError;
  return apiError.message ?? "Could not load match notification previews";
}

export default function MatchNotificationPreviewPage() {
  const [previews, setPreviews] = useState<MatchNotificationPreviewView[]>([]);
  const [selectedMatchId, setSelectedMatchId] =
    useState<MatchNotificationMatchId | null>(null);
  const [bodyMode, setBodyMode] =
    useState<MatchNotificationPreviewBodyMode>("preview");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sendError, setSendError] = useState<string | null>(null);
  const [sendResult, setSendResult] =
    useState<MatchNotificationSendView | null>(null);

  const selectedPreview = useMemo(() => {
    if (selectedMatchId === null) {
      return previews[0] ?? null;
    }
    return (
      previews.find(
        (preview) => String(preview.matchId) === String(selectedMatchId),
      ) ??
      previews[0] ??
      null
    );
  }, [previews, selectedMatchId]);

  async function sendSelectedPreviewWith(transport: "mock" | "brevo") {
    if (!selectedPreview) {
      return;
    }
    setSending(true);
    setSendError(null);
    setSendResult(null);
    try {
      const result =
        transport === "brevo"
          ? await matchNotificationsApi.sendEmail(selectedPreview.matchId)
          : await matchNotificationsApi.sendMock(selectedPreview.matchId);
      setSendResult(result);
    } catch (err) {
      setSendError(previewErrorMessage(err));
    } finally {
      setSending(false);
    }
  }

  async function loadPreviews(
    preferredMatchId?: MatchNotificationMatchId | null,
  ) {
    setLoading(true);
    setError(null);
    try {
      const nextPreviews = await matchNotificationsApi.listPreviews();
      setPreviews(nextPreviews);
      const preferredStillExists =
        preferredMatchId !== null &&
        preferredMatchId !== undefined &&
        nextPreviews.some(
          (preview) => String(preview.matchId) === String(preferredMatchId),
        );
      const nextSelected = preferredStillExists
        ? preferredMatchId
        : (nextPreviews[0]?.matchId ?? null);
      setSelectedMatchId(nextSelected);
      setBodyMode("preview");
    } catch (err) {
      setPreviews([]);
      setSelectedMatchId(null);
      setError(previewErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadPreviews(null);
  }, []);

  return (
    <div className="p-6">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-label text-xs font-semibold uppercase">
            Automatiserat utskick
          </p>
          <h1 className="text-title mt-1 text-2xl font-semibold">
            Matchningsbrev
          </h1>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            className="btn btn-main rounded px-4 py-2 text-sm font-medium"
            disabled={loading || sending || !selectedPreview}
            onClick={() => void sendSelectedPreviewWith("brevo")}
          >
            {sending ? "Sending..." : "Send Brevo Mail"}
          </button>
          <button
            type="button"
            className="btn btn-soft rounded px-4 py-2 text-sm font-medium"
            disabled={loading || sending || !selectedPreview}
            onClick={() => void sendSelectedPreviewWith("mock")}
          >
            Mock .eml
          </button>
          <button
            type="button"
            className="btn btn-soft rounded px-4 py-2 text-sm font-medium"
            disabled={loading}
            onClick={() => void loadPreviews(selectedPreview?.matchId ?? null)}
          >
            {loading ? "Loading..." : "Refresh Inbox"}
          </button>
        </div>
      </div>

      {error && (
        <div className="panel mb-6 rounded p-4 text-sm">
          <span className="text-danger">{error}</span>
        </div>
      )}

      {sendError && (
        <div className="panel mb-6 rounded p-4 text-sm">
          <span className="text-danger">{sendError}</span>
        </div>
      )}

      {sendResult && <MatchNotificationSendResultPanel result={sendResult} />}

      {!loading && previews.length === 0 && !error && (
        <section className="panel rounded p-6 text-sm">
          <h2 className="text-section mb-2 text-base font-medium">Inkorg</h2>
          <p className="text-soft">No match notification emails exist yet.</p>
        </section>
      )}

      {selectedPreview && (
        <div className="grid gap-6 lg:grid-cols-[360px_minmax(0,1fr)]">
          <MatchNotificationPreviewList
            previews={previews}
            selectedPreview={selectedPreview}
            onSelect={(matchId) => {
              setSelectedMatchId(matchId);
              setBodyMode("preview");
            }}
          />
          <MatchNotificationPreviewBody
            preview={selectedPreview}
            bodyMode={bodyMode}
            onBodyModeChange={setBodyMode}
          />
        </div>
      )}
    </div>
  );
}
