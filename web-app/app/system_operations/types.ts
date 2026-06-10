export interface HealthStatus {
  status: string;
  components?: Record<string, { status: string }>;
}

export interface OpsOverview {
  activeSessions?: number;
  lastSessionAt?: string;
  candidateCount?: number;
  totalMissions?: number;
}

export interface TriageEntry {
  id?: number;
  timestamp: string;
  type: string;
  context?: string;
  message: string;
}

export interface SessionEntry {
  id: string;
  tenantType: string;
  dbCompanyId: number;
  createdAt?: string;
}

export interface BindingEntry {
  dbCompanyId: number;
  tenantType: string;
  isPlatformSystem: boolean;
}
