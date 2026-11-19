import { act, render, type RenderOptions } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MotionConfig } from "framer-motion";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { http, HttpResponse } from "msw";
import type { ReactElement } from "react";
import { AuthProvider } from "../../auth/AuthContext";
import { setRefreshToken } from "../../auth/tokenStorage";
import { authResponse, demoUser, server } from "../../auth/__tests__/mockServer";
import type { AuthResponse, Role } from "../../auth/types";
import type { ApiResponse } from "../../api/types";
import { ToastProvider } from "../components/Toast";

const baseUrl = "http://localhost:8080/api/v1";

function ok<T>(data: T): ApiResponse<T> {
  return { success: true, data, timestamp: new Date().toISOString() };
}

// Simulates an already-logged-in user by priming the same silent-refresh
// flow AuthContext.test.tsx already exercises on mount, rather than
// bypassing AuthContext with a test-only auth stub.
export function mockAuthenticatedAs(role: Role): void {
  act(() => setRefreshToken("refresh-token-1"));
  server.use(
    http.post(`${baseUrl}/auth/refresh`, () =>
      HttpResponse.json(ok<AuthResponse>(authResponse({ user: { ...demoUser, role } }))),
    ),
  );
}

function newTestQueryClient(): QueryClient {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

interface RenderWithProvidersOptions extends RenderOptions {
  role?: Role;
  // For pages that read useParams() — renders `ui` at `route.path` with the
  // router starting at `route.initialEntry` instead of a route-less MemoryRouter.
  route?: { path: string; initialEntry: string };
}

export function renderWithProviders(ui: ReactElement, options: RenderWithProvidersOptions = {}) {
  const { role, route, ...renderOptions } = options;
  if (role) {
    mockAuthenticatedAs(role);
  }

  const routed = route ? (
    <MemoryRouter initialEntries={[route.initialEntry]}>
      <Routes>
        <Route path={route.path} element={ui} />
      </Routes>
    </MemoryRouter>
  ) : (
    <MemoryRouter>{ui}</MemoryRouter>
  );

  return render(
    <MotionConfig reducedMotion="always">
      <QueryClientProvider client={newTestQueryClient()}>
        <AuthProvider>
          <ToastProvider>{routed}</ToastProvider>
        </AuthProvider>
      </QueryClientProvider>
    </MotionConfig>,
    renderOptions,
  );
}
