import { apiClient } from "~/shared/api/client";
import type {
  CandidatePresentationArtifactEditInput,
  CandidatePresentationArtifactView,
  CandidatePresentationGenerationStartView,
} from "../types";

type ApiPathId = string | number;

const encodedId = (id: ApiPathId) => encodeURIComponent(String(id));

export const candidatePresentationApiPaths = {
  artifacts: "/api/ops/candidate-presentation-artifacts",
  artifact: (id: ApiPathId) => `/api/ops/candidate-presentation-artifacts/${encodedId(id)}`,
  generation: (matchId: ApiPathId) =>
    `/api/ops/matches/${encodedId(matchId)}/candidate-presentation-generation`,
} as const;

export const candidatePresentationApi = {
  listArtifacts(): Promise<CandidatePresentationArtifactView[]> {
    return apiClient.get<CandidatePresentationArtifactView[]>(
      candidatePresentationApiPaths.artifacts,
    );
  },

  artifact(id: number): Promise<CandidatePresentationArtifactView> {
    return apiClient.get<CandidatePresentationArtifactView>(
      candidatePresentationApiPaths.artifact(id),
    );
  },

  editArtifact(
    id: number,
    input: CandidatePresentationArtifactEditInput,
  ): Promise<CandidatePresentationArtifactView> {
    return apiClient.put<CandidatePresentationArtifactView>(
      candidatePresentationApiPaths.artifact(id),
      input,
    );
  },

  startGeneration(matchId: ApiPathId): Promise<CandidatePresentationGenerationStartView> {
    return apiClient.post<CandidatePresentationGenerationStartView>(
      candidatePresentationApiPaths.generation(matchId),
    );
  },
};
