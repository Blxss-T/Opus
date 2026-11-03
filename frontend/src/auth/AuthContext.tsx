import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { setUnauthorizedHandler } from "../api/client";
import * as authApi from "./authApi";
import { clearTokens, getRefreshToken, setTokens } from "./tokenStorage";
import type { AuthResponse, LoginRequest, OrganizationResponse, RegisterRequest, Role, UserResponse } from "./types";

type AuthStatus = "initializing" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  status: AuthStatus;
  user: UserResponse | null;
  organization: OrganizationResponse | null;
  login: (body: LoginRequest) => Promise<void>;
  register: (body: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function applyAuthResponse(
  auth: AuthResponse,
  setUser: (u: UserResponse) => void,
  setOrganization: (o: OrganizationResponse) => void,
  setStatus: (s: AuthStatus) => void,
): void {
  setTokens(auth.accessToken, auth.refreshToken);
  setUser(auth.user);
  setOrganization(auth.organization);
  setStatus("authenticated");
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("initializing");
  const [user, setUser] = useState<UserResponse | null>(null);
  const [organization, setOrganization] = useState<OrganizationResponse | null>(null);

  const forceLogout = useCallback(() => {
    clearTokens();
    setUser(null);
    setOrganization(null);
    setStatus("unauthenticated");
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(forceLogout);
  }, [forceLogout]);

  // On mount, try to resume a session from the refresh token left in
  // sessionStorage (survives a reload, not a closed tab). Guarded with a ref
  // because React 18 StrictMode invokes mount effects twice in development:
  // without the guard, both invocations would send the same single-use
  // refresh token, and the backend's reuse detection would revoke the
  // session it was trying to resume.
  const hasAttemptedResume = useRef(false);
  useEffect(() => {
    if (hasAttemptedResume.current) {
      return;
    }
    hasAttemptedResume.current = true;

    const existingRefreshToken = getRefreshToken();
    if (!existingRefreshToken) {
      setStatus("unauthenticated");
      return;
    }
    authApi
      .refresh({ refreshToken: existingRefreshToken })
      .then((auth) => applyAuthResponse(auth, setUser, setOrganization, setStatus))
      .catch(() => {
        clearTokens();
        setStatus("unauthenticated");
      });
  }, []);

  const login = useCallback(async (body: LoginRequest) => {
    const auth = await authApi.login(body);
    applyAuthResponse(auth, setUser, setOrganization, setStatus);
  }, []);

  const register = useCallback(async (body: RegisterRequest) => {
    const auth = await authApi.register(body);
    applyAuthResponse(auth, setUser, setOrganization, setStatus);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      // Always clear local state, even if the network call failed —
      // staying "logged in" client-side after a failed logout call would
      // strand the user on a session they believe they ended.
      forceLogout();
    }
  }, [forceLogout]);

  const hasRole = useCallback((roles: Role[]) => !!user && roles.includes(user.role), [user]);

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, organization, login, register, logout, hasRole }),
    [status, user, organization, login, register, logout, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuthContext(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuthContext must be used within an AuthProvider");
  }
  return context;
}
