// AuthContext + AuthProvider
// AuthProvider placeras i App() i root.tsx
// useEffect checks the current auth session on mount; user = null when unauthenticated.

import { createContext, useContext, useEffect, useState } from "react";
import { authApi } from "~/auth/api/authApi";
import type { AuthState, AuthUser } from "~/auth/types";

interface AuthContextValue extends AuthState {
  setUser: (user: AuthUser | null) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Real session check against the auth profile endpoint.
    authApi.me()
      .then(setUser)
      .catch(() => { /* not authenticated; user stays null */ })
      .finally(() => setLoading(false));
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuthContext(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (ctx === null) {
    throw new Error("useAuthContext must be used within AuthProvider");
  }
  return ctx;
}
