import { apiClient } from "~/shared/api/client";
import type { MarketplaceReferenceData } from "../types";

export const referenceDataApiPaths = {
  referenceData: "/api/public/reference-data",
} as const;

export const referenceDataApi = {
  referenceData(): Promise<MarketplaceReferenceData> {
    return apiClient.get<MarketplaceReferenceData>(referenceDataApiPaths.referenceData);
  },
};
