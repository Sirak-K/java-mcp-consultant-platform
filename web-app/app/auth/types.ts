export type TenantType = "CUSTOMER" | "PLATFORM_SYSTEM";

export interface AuthUser {
  dbCompanyId: number;
  tenantExternalId: string | null;
  tenantType: TenantType;
  isPlatformSystem: boolean;
}

export interface AuthState {
  user: AuthUser | null;
  loading: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}
