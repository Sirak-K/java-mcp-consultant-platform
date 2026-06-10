import { useEffect, useState } from "react";
import { matchNotificationsApi } from "~/match_notifications/api/matchNotificationsApi";
import type { MatchNotificationPreviewView } from "~/match_notifications/types";

const MATCH_NOTIFICATION_UNREAD_TTL_MS = 60 * 60 * 1000;
const MATCH_NOTIFICATION_UNREAD_NOTIFICATIONS_KEY =
  "matchNotifications.unread.notifications.v1";
const MATCH_NOTIFICATION_UNREAD_SEEN_KEY =
  "matchNotifications.unread.seen.v1";

interface UseUnreadMatchNotificationCountInput {
  enabled: boolean;
  matchNotificationOpen: boolean;
}

function readMatchNotificationUnreadStore(key: string): Record<string, number> {
  if (typeof window === "undefined") return {};
  try {
    const parsed = JSON.parse(window.localStorage.getItem(key) ?? "{}");
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }
    return Object.fromEntries(
      Object.entries(parsed).filter(
        (entry): entry is [string, number] =>
          typeof entry[1] === "number" && Number.isFinite(entry[1]),
      ),
    );
  } catch {
    return {};
  }
}

function writeMatchNotificationUnreadStore(
  key: string,
  value: Record<string, number>,
) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    return;
  }
}

function matchNotificationUnreadPreviewKey(
  preview: MatchNotificationPreviewView,
): string {
  const matchIds = preview.matchIds
    ?.filter((matchId) => matchId !== null && matchId !== undefined)
    .map((matchId) => String(matchId))
    .sort();
  if (matchIds && matchIds.length > 0) return `matches:${matchIds.join("|")}`;
  if (preview.matchId !== null && preview.matchId !== undefined) {
    return `match:${String(preview.matchId)}`;
  }
  return `candidate:${preview.candidateProfileId ?? "unknown"}:mission:${preview.missionId ?? "unknown"}`;
}

function updateMatchNotificationUnreadCount(
  previews: MatchNotificationPreviewView[],
  matchNotificationOpen: boolean,
): number {
  const now = Date.now();
  const currentKeys = new Set(previews.map(matchNotificationUnreadPreviewKey));
  const notifications = readMatchNotificationUnreadStore(
    MATCH_NOTIFICATION_UNREAD_NOTIFICATIONS_KEY,
  );
  const seen = readMatchNotificationUnreadStore(
    MATCH_NOTIFICATION_UNREAD_SEEN_KEY,
  );

  for (const [key, createdAt] of Object.entries(notifications)) {
    if (!currentKeys.has(key)) {
      delete notifications[key];
      continue;
    }
    if (now - createdAt > MATCH_NOTIFICATION_UNREAD_TTL_MS) {
      delete notifications[key];
      seen[key] = Math.max(seen[key] ?? 0, createdAt);
    }
  }

  for (const key of currentKeys) {
    if (!notifications[key] && !seen[key]) notifications[key] = now;
    if (matchNotificationOpen) {
      seen[key] = now;
      delete notifications[key];
    }
  }

  writeMatchNotificationUnreadStore(
    MATCH_NOTIFICATION_UNREAD_NOTIFICATIONS_KEY,
    notifications,
  );
  writeMatchNotificationUnreadStore(MATCH_NOTIFICATION_UNREAD_SEEN_KEY, seen);

  if (matchNotificationOpen) return 0;
  return Array.from(currentKeys).filter((key) => {
    const createdAt = notifications[key];
    return (
      createdAt !== undefined &&
      now - createdAt <= MATCH_NOTIFICATION_UNREAD_TTL_MS &&
      (!seen[key] || seen[key] < createdAt)
    );
  }).length;
}

export function useUnreadMatchNotificationCount({
  enabled,
  matchNotificationOpen,
}: UseUnreadMatchNotificationCountInput): number {
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!enabled) {
      setUnreadCount(0);
      return;
    }

    let cancelled = false;
    let intervalId: number | undefined;

    async function refreshUnreadCount() {
      try {
        const previews = await matchNotificationsApi.listPreviews();
        if (!cancelled) {
          setUnreadCount(
            updateMatchNotificationUnreadCount(
              previews,
              matchNotificationOpen,
            ),
          );
        }
      } catch {
        if (!cancelled) setUnreadCount(0);
      }
    }

    void refreshUnreadCount();
    intervalId = window.setInterval(() => {
      void refreshUnreadCount();
    }, 60_000);

    return () => {
      cancelled = true;
      if (intervalId !== undefined) window.clearInterval(intervalId);
    };
  }, [enabled, matchNotificationOpen]);

  return unreadCount;
}
