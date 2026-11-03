import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import type { ApiResponse, ErrorResponse } from "./types";
import { ApiError } from "./types";
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from "../auth/tokenStorage";
import type { AuthResponse } from "../auth/types";

const baseURL = import.meta.env.VITE_API_BASE_URL;

// Paths that must never trigger the refresh-and-retry flow on 401: retrying
// a failed login/refresh with a refresh call would loop forever.
const REFRESH_EXEMPT_PATHS = ["/auth/login", "/auth/refresh", "/auth/register"];

let unauthorizedHandler: (() => void) | null = null;

export function setUnauthorizedHandler(handler: () => void): void {
  unauthorizedHandler = handler;
}

export const httpClient: AxiosInstance = axios.create({ baseURL });

httpClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

function isExemptPath(url: string | undefined): boolean {
  if (!url) return false;
  return REFRESH_EXEMPT_PATHS.some((path) => url.includes(path));
}

function toApiError(error: unknown): ApiError {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    const retryAfterHeader = error.response?.headers?.["retry-after"];
    const retryAfterSeconds = retryAfterHeader ? Number(retryAfterHeader) : undefined;
    const envelopeError = error.response?.data?.error;
    if (envelopeError) {
      return new ApiError(envelopeError, retryAfterSeconds);
    }
    const fallback: ErrorResponse = {
      status: error.response?.status ?? 0,
      error: "ERR_NETWORK",
      message: error.message || "Network error",
      path: error.config?.url ?? "",
      timestamp: new Date().toISOString(),
    };
    return new ApiError(fallback, retryAfterSeconds);
  }
  const unknown: ErrorResponse = {
    status: 0,
    error: "ERR_UNKNOWN",
    message: error instanceof Error ? error.message : "Unknown error",
    path: "",
    timestamp: new Date().toISOString(),
  };
  return new ApiError(unknown);
}

// Concurrent 401s during a refresh share one in-flight promise so a burst of
// requests doesn't fire multiple refresh calls (each of which would rotate
// and invalidate the previous refresh token).
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = getRefreshToken();
      if (!refreshToken) {
        throw new Error("No refresh token available");
      }
      const response = await axios.post<ApiResponse<AuthResponse>>(
        `${baseURL}/auth/refresh`,
        { refreshToken },
      );
      const auth = response.data.data;
      if (!auth) {
        throw new Error("Refresh response missing data");
      }
      setTokens(auth.accessToken, auth.refreshToken);
      return auth.accessToken;
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

httpClient.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiResponse<unknown>;
    if (envelope && typeof envelope === "object" && "success" in envelope) {
      if (!envelope.success) {
        throw new ApiError(
          envelope.error ?? {
            status: response.status,
            error: "ERR_UNKNOWN",
            message: "Request failed",
            path: response.config.url ?? "",
            timestamp: new Date().toISOString(),
          },
        );
      }
      return envelope.data;
    }
    return response.data;
  },
  async (error) => {
    const original = error?.config as InternalAxiosRequestConfig & { _retried?: boolean };
    const status = error?.response?.status;

    if (status === 401 && original && !original._retried && !isExemptPath(original.url)) {
      original._retried = true;
      try {
        const newAccessToken = await refreshAccessToken();
        original.headers.set("Authorization", `Bearer ${newAccessToken}`);
        return httpClient(original);
      } catch {
        clearTokens();
        unauthorizedHandler?.();
        return Promise.reject(toApiError(error));
      }
    }

    return Promise.reject(toApiError(error));
  },
);

// The response interceptor above unwraps ApiResponse<T> down to T, which
// axios's own generics don't reflect (they still expect AxiosResponse<T>).
// These wrappers give call sites the real resulting type without a cast at
// every call site.
export function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return httpClient.get(url, { params }) as unknown as Promise<T>;
}

export function apiPost<T>(url: string, body?: unknown): Promise<T> {
  return httpClient.post(url, body) as unknown as Promise<T>;
}

export function apiPut<T>(url: string, body?: unknown): Promise<T> {
  return httpClient.put(url, body) as unknown as Promise<T>;
}

export function apiPatch<T>(url: string, body?: unknown): Promise<T> {
  return httpClient.patch(url, body) as unknown as Promise<T>;
}

export function apiDelete<T>(url: string): Promise<T> {
  return httpClient.delete(url) as unknown as Promise<T>;
}
