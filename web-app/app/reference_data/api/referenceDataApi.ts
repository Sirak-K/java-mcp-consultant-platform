import { apiClient } from "~/shared/api/client";
import type { ReferenceData } from "../types";

export const referenceDataApiPaths = {
  referenceData: "/api/public/reference-data",
} as const;

export const referenceDataApi = {
  referenceData(): Promise<ReferenceData> {
    return apiClient.get<ReferenceData>(referenceDataApiPaths.referenceData);
  },
};
