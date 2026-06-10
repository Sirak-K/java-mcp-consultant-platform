export type MatchNotificationMatchId = string | number;

export interface MatchNotificationPreviewView {
  matchId: MatchNotificationMatchId;
  groupedMatchCount?: number | null;
  matchIds?: MatchNotificationMatchId[] | null;
  candidateProfileId?: number | null;
  missionId?: number | null;
  subject: string;
  evidenceBrief?: string | null;
  textBody?: string | null;
  htmlBody?: string | null;
  generatedAt?: string | null;
}

export interface MatchNotificationSendView {
  matchId: MatchNotificationMatchId;
  groupedMatchCount?: number | null;
  matchIds?: MatchNotificationMatchId[] | null;
  candidateProfileId?: number | null;
  missionId?: number | null;
  to: string;
  from: string;
  subject: string;
  transport: string;
  status: string;
  transportRef: string;
  deliveryId?: number | null;
  deliveryStatus?: string | null;
  sentAt?: string | null;
  messageRfc822: string;
}
