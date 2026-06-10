export type CandidatePresentationArtifactStatus =
  | "PENDING_GENERATION"
  | "GENERATED"
  | "OPS_REVIEW"
  | "GENERATION_FAILED";

export interface CandidatePresentationArtifactView {
  id: number;
  sourceCandidateToSlotMatchId: number;
  candProfileId: number;
  missionId: number;
  missionSlotId: number;
  artifactStatus: CandidatePresentationArtifactStatus;
  presentationTitle: string;
  customerFacingContentJson: string;
  opsReviewContentJson: string;
  evidenceTraceJson: string;
  createdAt: string;
  updatedAt: string;
}

export interface CandidatePresentationArtifactEditInput {
  customerFacingContentJson: string;
  opsReviewContentJson: string;
  evidenceTraceJson: string;
}

export interface CandidatePresentationGenerationStartView {
  generationStartStatus: "accepted" | "blocked" | "failed_to_start" | string;
  matchId: number;
  artifactId?: number | null;
  artifactStatus?: CandidatePresentationArtifactStatus | null;
  runId: string;
  message: string;
  artifact?: CandidatePresentationArtifactView | null;
}
