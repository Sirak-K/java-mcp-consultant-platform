import { apiClient } from "~/shared/api/client";
import type {
  MatchNotificationMatchId,
  MatchNotificationPreviewView,
  MatchNotificationSendView,
} from "../types";

type ApiPathId = string | number;

const encodedId = (id: ApiPathId) => encodeURIComponent(String(id));

export const matchNotificationsApiPaths = {
  previews: "/api/ops/match-notifications/previews",
  preview: (matchId: ApiPathId) =>
    `/api/ops/match-notifications/matches/${encodedId(matchId)}/preview`,
  mockSend: (matchId: ApiPathId) =>
    `/api/ops/match-notifications/matches/${encodedId(matchId)}/mock-send`,
  emailSend: (matchId: ApiPathId) =>
    `/api/ops/match-notifications/matches/${encodedId(matchId)}/email-send`,
} as const;

export const matchNotificationsApi = {
  listPreviews(): Promise<MatchNotificationPreviewView[]> {
    return apiClient.get<MatchNotificationPreviewView[]>(
      matchNotificationsApiPaths.previews,
    );
  },

  preview(
    matchId: MatchNotificationMatchId,
  ): Promise<MatchNotificationPreviewView> {
    return apiClient.get<MatchNotificationPreviewView>(
      matchNotificationsApiPaths.preview(matchId),
    );
  },

  sendMock(matchId: MatchNotificationMatchId): Promise<MatchNotificationSendView> {
    return apiClient.post<MatchNotificationSendView>(
      matchNotificationsApiPaths.mockSend(matchId),
    );
  },

  sendEmail(matchId: MatchNotificationMatchId): Promise<MatchNotificationSendView> {
    return apiClient.post<MatchNotificationSendView>(
      matchNotificationsApiPaths.emailSend(matchId),
    );
  },
};
