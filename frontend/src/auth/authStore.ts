import { create } from "zustand";
import { setUnauthorizedHandler } from "../api/client";
import * as authApi from "./authApi";
import { clearTokens, getRefreshToken, setTokens } from "./tokenStorage";
import type { AuthResponse, LoginRequest, OrganizationResponse, RegisterRequest, Role, UserResponse } from "./types";

export type AuthStatus = "initializing" | "authenticated" | "unauthenticated";

export interface AuthState {
  status: AuthStatus;
  user: UserResponse | null;
  organization: OrganizationResponse | null;
  login: (body: LoginRequest) => Promise<void>;
  register: (body: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (roles: Role[]) => boolean;
  hydrate: () => void;
  forceLogout: () => void;
}

function applyAuthResponse(auth: AuthResponse): Pick<AuthState, "user" | "organization" | "status"> {
  setTokens(auth.accessToken, auth.refreshToken);
  return {
    user: auth.user,
    organization: auth.organization,
    status: "authenticated",
  };
}

let hasAttemptedResume = false;
let unauthorizedHandlerBound = false;

export const useAuthStore = create<AuthState>((set, get) => ({
  status: "initializing",
  user: null,
  organization: null,

  forceLogout: () => {
    clearTokens();
    set({ user: null, organization: null, status: "unauthenticated" });
  },

  hydrate: () => {
    if (!unauthorizedHandlerBound) {
      unauthorizedHandlerBound = true;
      setUnauthorizedHandler(() => get().forceLogout());
    }

    // React 18 StrictMode invokes mount effects twice in development.
    // The refresh token is single-use; a second call would trip reuse detection.
    if (hasAttemptedResume) {
      return;
    }
    hasAttemptedResume = true;

    const existingRefreshToken = getRefreshToken();
    if (!existingRefreshToken) {
      set({ status: "unauthenticated" });
      return;
    }
    authApi
      .refresh({ refreshToken: existingRefreshToken })
      .then((auth) => set(applyAuthResponse(auth)))
      .catch(() => {
        clearTokens();
        set({ user: null, organization: null, status: "unauthenticated" });
      });
  },

  login: async (body) => {
    const auth = await authApi.login(body);
    set(applyAuthResponse(auth));
  },

  register: async (body) => {
    const auth = await authApi.register(body);
    set(applyAuthResponse(auth));
  },

  logout: async () => {
    try {
      await authApi.logout();
    } finally {
      get().forceLogout();
    }
  },

  hasRole: (roles) => {
    const { user } = get();
    return !!user && roles.includes(user.role);
  },
}));

export function resetAuthStore(): void {
  hasAttemptedResume = false;
  unauthorizedHandlerBound = false;
  useAuthStore.setState({
    status: "initializing",
    user: null,
    organization: null,
  });
}
