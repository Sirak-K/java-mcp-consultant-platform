import { apiClient } from "~/shared/api/client";
import type {
  BindingEntry,
  HealthStatus,
  OpsOverview,
  SessionEntry,
  TriageEntry,
} from "../types";

type ApiPathId = string | number;

const encodedId = (id: ApiPathId) => encodeURIComponent(String(id));

export const systemOperationsApiPaths = {
  health: "/actuator/health",
  overview: "/api/ops/overview",
  triage: "/api/ops/triage",
  diagnosticSessions: "/api/ops/diagnostics/sessions",
  diagnosticBindings: "/api/ops/diagnostics/bindings",
  encodedId,
} as const;

export const systemOperationsApi = {
  health(): Promise<HealthStatus> {
    return apiClient.get<HealthStatus>(systemOperationsApiPaths.health);
  },

  overview(): Promise<OpsOverview> {
    return apiClient.get<OpsOverview>(systemOperationsApiPaths.overview);
  },

  triage(): Promise<TriageEntry[]> {
    return apiClient.get<TriageEntry[]>(systemOperationsApiPaths.triage);
  },

  diagnosticSessions(): Promise<SessionEntry[]> {
    return apiClient.get<SessionEntry[]>(systemOperationsApiPaths.diagnosticSessions);
  },

  diagnosticBindings(): Promise<BindingEntry[]> {
    return apiClient.get<BindingEntry[]>(systemOperationsApiPaths.diagnosticBindings);
  },
};
