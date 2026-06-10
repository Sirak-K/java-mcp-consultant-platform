// Auth session hooks used by public login and protected ops surfaces.

import { useState } from "react";
import { useNavigate } from "react-router";
import { authApi } from "~/auth/api/authApi";
import { useAuthContext } from "./AuthContext";
import type { LoginRequest } from "~/auth/types";
import type { ApiError } from "~/shared/api/apiErrors";

export interface UseLoginResult {
  login: (req: LoginRequest) => Promise<void>;
  submitting: boolean;
  error: string | null;
}

export function useLogin(): UseLoginResult {
  const { setUser } = useAuthContext();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const login = async (req: LoginRequest) => {
    setSubmitting(true);
    setError(null);
    try {
      const user = await authApi.login(req);
      setUser(user);
      if (user.isPlatformSystem) navigate("/ops/overview");
      else navigate("/unauthorized");
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      if (apiErr.code === "UNAUTHORIZED") {
        setError("Felaktig e-postadress eller lösenord.");
      } else {
        setError(apiErr.message ?? "Inloggning misslyckades.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return { login, submitting, error };
}

export function useLogout(): () => Promise<void> {
  const { setUser } = useAuthContext();
  const navigate = useNavigate();

  return async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
      navigate("/login");
    }
  };
}
