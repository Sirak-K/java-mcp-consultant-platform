import { apiClient } from "~/shared/api/client";
import type {
  CandidateApplicationEditInput,
  CandidateApplicationInput,
  CandidateApplicationView,
  CandidateCvPreviewView,
  CandidateCvProfileWorkingCopyInput,
  CandidateProfileSummaryInput,
  RegisteredCandidateProfileCardView,
  RegisteredCandidateProfileView,
} from "../types";

type ApiPathId = string | number;

const encodedId = (id: ApiPathId) => encodeURIComponent(String(id));

type RegisteredCandidateProfileApiView = CandidateApplicationView & {
  candidateProfileId?: number | null;
};

function toRegisteredCandidateProfile(
  item: RegisteredCandidateProfileApiView,
): RegisteredCandidateProfileView {
  const { id, candidateProfileId, ...profile } = item;
  return {
    ...profile,
    candidateProfileId: candidateProfileId ?? id,
  };
}

export const candidateProfilesApiPaths = {
  publicCandidateApplications: "/api/public/candidate-applications",
  publicCandidateCvPreview: "/api/public/candidate-cv-preview",
  candidateApplications: "/api/ops/candidate-applications",
  registeredCandidateProfiles: "/api/ops/registered-candidate-profiles",
  registeredCandidateProfile: (id: ApiPathId) =>
    `/api/ops/registered-candidate-profiles/${encodedId(id)}`,
  registeredCandidateProfileCards: "/api/ops/registered-candidate-profile-cards",
} as const;

export const candidateProfilesApi = {
  submitCandidateApplication(
    input: CandidateApplicationInput,
  ): Promise<CandidateApplicationView> {
    return apiClient.post<CandidateApplicationView>(
      candidateProfilesApiPaths.publicCandidateApplications,
      input,
    );
  },

  submitCandidateApplicationFile(
    contactEmail: string,
    cvFile: File,
    profileWorkingCopy: CandidateCvProfileWorkingCopyInput,
    certificateFiles: File[],
    generatedSummary?: CandidateProfileSummaryInput,
  ): Promise<CandidateApplicationView> {
    const formData = new FormData();
    formData.append("contactEmail", contactEmail);
    formData.append("cvFile", cvFile);
    formData.append(
      "profileWorkingCopy",
      new Blob([JSON.stringify(profileWorkingCopy)], { type: "application/json" }),
    );
    if (generatedSummary) {
      formData.append(
        "generatedSummary",
        new Blob([JSON.stringify(generatedSummary)], { type: "application/json" }),
      );
    }
    certificateFiles.forEach((file) => {
      formData.append("certificateFiles", file);
    });
    return apiClient.postForm<CandidateApplicationView>(
      candidateProfilesApiPaths.publicCandidateApplications,
      formData,
    );
  },

  previewCandidateCv(cvFile: File): Promise<CandidateCvPreviewView> {
    const formData = new FormData();
    formData.append("cvFile", cvFile);
    return apiClient.postForm<CandidateCvPreviewView>(
      candidateProfilesApiPaths.publicCandidateCvPreview,
      formData,
    );
  },

  listCandidateApplications(): Promise<CandidateApplicationView[]> {
    return apiClient.get<CandidateApplicationView[]>(
      candidateProfilesApiPaths.candidateApplications,
    );
  },

  async listRegisteredCandidateProfiles(): Promise<RegisteredCandidateProfileView[]> {
    const profiles = await apiClient.get<RegisteredCandidateProfileApiView[]>(
      candidateProfilesApiPaths.registeredCandidateProfiles,
    );
    return profiles.map(toRegisteredCandidateProfile);
  },

  listRegisteredCandidateProfileCards(): Promise<RegisteredCandidateProfileCardView[]> {
    return apiClient.get<RegisteredCandidateProfileCardView[]>(
      candidateProfilesApiPaths.registeredCandidateProfileCards,
    );
  },

  editRegisteredCandidateProfile(
    id: number,
    input: CandidateApplicationEditInput,
  ): Promise<RegisteredCandidateProfileView> {
    return apiClient.put<RegisteredCandidateProfileApiView>(
      candidateProfilesApiPaths.registeredCandidateProfile(id),
      input,
    ).then(toRegisteredCandidateProfile);
  },

  deleteRegisteredCandidateProfile(id: number): Promise<void> {
    return apiClient.delete<void>(
      candidateProfilesApiPaths.registeredCandidateProfile(id),
    );
  },
};
