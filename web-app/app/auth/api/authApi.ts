import { apiClient } from "~/shared/api/client";
import type { AuthUser, LoginRequest } from "../types";

export const authApiPaths = {
  login: "/api/auth/login",
  me: "/api/auth/me",
  logout: "/api/auth/logout",
} as const;

export const authApi = {
  async login(req: LoginRequest): Promise<AuthUser> {
    await apiClient.post<void>(authApiPaths.login, req);
    return apiClient.get<AuthUser>(authApiPaths.me);
  },

  me(): Promise<AuthUser> {
    return apiClient.get<AuthUser>(authApiPaths.me);
  },

  logout(): Promise<void> {
    return apiClient.post<void>(authApiPaths.logout);
  },
};
