import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import type { ApiResponse } from "../../api/types";
import type { AuthResponse } from "../types";

const baseUrl = "http://localhost:8080/api/v1";

export const demoUser = {
  id: "user-1",
  organizationId: "org-1",
  email: "admin@opus-demo.test",
  firstName: "Demo",
  lastName: "Admin",
  role: "ORG_ADMIN" as const,
  active: true,
  emailVerified: true,
};

export const demoOrganization = {
  id: "org-1",
  name: "Opus Demo Trading",
  slug: "opus-demo-trading",
  active: true,
};

export function authResponse(overrides: Partial<AuthResponse> = {}): AuthResponse {
  return {
    accessToken: "access-token-1",
    refreshToken: "refresh-token-1",
    tokenType: "Bearer",
    user: demoUser,
    organization: demoOrganization,
    ...overrides,
  };
}

function ok<T>(data: T): ApiResponse<T> {
  return { success: true, data, timestamp: new Date().toISOString() };
}

export const server = setupServer(
  http.post(`${baseUrl}/auth/login`, () => HttpResponse.json(ok(authResponse()))),
  http.post(`${baseUrl}/auth/refresh`, () =>
    HttpResponse.json(ok(authResponse({ accessToken: "access-token-2", refreshToken: "refresh-token-2" }))),
  ),
  http.post(`${baseUrl}/auth/logout`, () => HttpResponse.json(ok(undefined))),
);
