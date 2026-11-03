// Access token lives only in memory (lost on reload) and the refresh token
// in sessionStorage (lost on tab close). Neither ever touches localStorage:
// the backend has no httpOnly cookie option (CORS credentials are disabled),
// so this is the smallest persistent footprint available for the refresh
// token while still surviving a page reload within the same tab.
const REFRESH_TOKEN_KEY = "opus.refreshToken";

let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setRefreshToken(token: string | null): void {
  try {
    if (token) {
      sessionStorage.setItem(REFRESH_TOKEN_KEY, token);
    } else {
      sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    }
  } catch {
    // sessionStorage unavailable (private mode, blocked site data, etc.) —
    // the session simply won't survive a reload.
  }
}

export function setTokens(access: string, refresh: string): void {
  setAccessToken(access);
  setRefreshToken(refresh);
}

export function clearTokens(): void {
  setAccessToken(null);
  setRefreshToken(null);
}
