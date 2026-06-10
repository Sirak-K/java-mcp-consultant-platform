import { apiClient } from "~/shared/api/client";
import type {
  MissionProposalEditInput,
  MissionProposalInput,
  MissionProposalPreviewInput,
  MissionProposalPreviewView,
  MissionProposalView,
  RegisteredMissionView,
} from "../types";

type ApiPathId = string | number;

const encodedId = (id: ApiPathId) => encodeURIComponent(String(id));

export const missionsApiPaths = {
  missionProposals: "/api/ops/mission-proposals",
  missionProposal: (id: ApiPathId) => `/api/ops/mission-proposals/${encodedId(id)}`,
  approveMissionProposal: (id: ApiPathId) =>
    `/api/ops/mission-proposals/${encodedId(id)}/approve`,
  rejectMissionProposal: (id: ApiPathId) =>
    `/api/ops/mission-proposals/${encodedId(id)}/reject`,
  publicMissionProposals: "/api/public/mission-proposals",
  publicMissionProposalPreview: "/api/public/mission-proposal-preview",
  registeredMissions: "/api/ops/registered-missions",
} as const;

export const missionsApi = {
  submitMissionProposal(
    input: MissionProposalInput,
  ): Promise<MissionProposalView> {
    return apiClient.post<MissionProposalView>(
      missionsApiPaths.publicMissionProposals,
      input,
    );
  },

  previewMissionProposal(
    input: MissionProposalPreviewInput,
  ): Promise<MissionProposalPreviewView> {
    return apiClient.post<MissionProposalPreviewView>(
      missionsApiPaths.publicMissionProposalPreview,
      input,
    );
  },

  listMissionProposals(): Promise<MissionProposalView[]> {
    return apiClient.get<MissionProposalView[]>(missionsApiPaths.missionProposals);
  },

  editMissionProposal(
    id: number,
    input: MissionProposalEditInput,
  ): Promise<MissionProposalView> {
    return apiClient.put<MissionProposalView>(
      missionsApiPaths.missionProposal(id),
      input,
    );
  },

  approveMissionProposal(id: number): Promise<MissionProposalView> {
    return apiClient.put<MissionProposalView>(
      missionsApiPaths.approveMissionProposal(id),
    );
  },

  rejectMissionProposal(id: number): Promise<MissionProposalView> {
    return apiClient.put<MissionProposalView>(
      missionsApiPaths.rejectMissionProposal(id),
    );
  },

  listRegisteredMissions(): Promise<RegisteredMissionView[]> {
    return apiClient.get<RegisteredMissionView[]>(missionsApiPaths.registeredMissions);
  },
};
