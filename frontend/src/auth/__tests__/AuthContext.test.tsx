import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { AuthProvider, useAuthContext } from "../AuthContext";
import { clearTokens, getAccessToken, getRefreshToken, setRefreshToken } from "../tokenStorage";
import { server } from "./mockServer";

const baseUrl = "http://localhost:8080/api/v1";

function Probe() {
  const { status, user, login, logout } = useAuthContext();
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="user">{user?.email ?? ""}</span>
      <button onClick={() => login({ email: "admin@opus-demo.test", password: "DemoPass123!" })}>login</button>
      <button onClick={() => logout()}>logout</button>
    </div>
  );
}

describe("AuthContext", () => {
  beforeEach(() => {
    clearTokens();
  });

  it("starts unauthenticated with no stored refresh token", async () => {
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("unauthenticated"));
  });

  it("logs in, storing tokens and the user", async () => {
    const user = userEvent.setup();
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("unauthenticated"));

    await user.click(screen.getByText("login"));

    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("authenticated"));
    expect(screen.getByTestId("user")).toHaveTextContent("admin@opus-demo.test");
    expect(getAccessToken()).toBe("access-token-1");
    expect(getRefreshToken()).toBe("refresh-token-1");
  });

  it("logs out and clears tokens even though the test server honours the logout call", async () => {
    const user = userEvent.setup();
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("unauthenticated"));
    await user.click(screen.getByText("login"));
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("authenticated"));

    await user.click(screen.getByText("logout"));

    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("unauthenticated"));
    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });

  it("resumes a session on mount via silent refresh when a refresh token is already stored", async () => {
    act(() => setRefreshToken("refresh-token-1"));

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("authenticated"));
    expect(screen.getByTestId("user")).toHaveTextContent("admin@opus-demo.test");
  });

  it("force-logs-out when the stored refresh token is rejected on mount", async () => {
    act(() => setRefreshToken("a-revoked-refresh-token"));
    server.use(
      http.post(`${baseUrl}/auth/refresh`, () =>
        HttpResponse.json(
          { success: false, error: { status: 401, error: "ERR_401", message: "revoked", path: "", timestamp: "" }, timestamp: "" },
          { status: 401 },
        ),
      ),
    );

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("unauthenticated"));
    expect(getRefreshToken()).toBeNull();
  });
});
