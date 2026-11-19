import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../../AuthContext";
import { requestGoogleIdToken } from "../../googleIdentity";
import { getAccessToken } from "../../tokenStorage";
import { LoginPage } from "../LoginPage";

vi.mock("../../googleIdentity", () => ({
  requestGoogleIdToken: vi.fn(),
  preloadGoogleIdentity: vi.fn(),
  getGoogleClientId: vi.fn(() => "test-google-client"),
}));

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe("LoginPage", () => {
  it("requires email and password before the browser lets the form submit", async () => {
    renderLoginPage();
    const emailInput = screen.getByLabelText("Email");
    const passwordInput = screen.getByLabelText("Password");
    expect(emailInput).toBeRequired();
    expect(passwordInput).toBeRequired();
  });

  it("renders a link to register and to forgot-password", async () => {
    renderLoginPage();
    expect(screen.getByText("Register your organization")).toBeInTheDocument();
    expect(screen.getByText("Forgot your password?")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continue with Google" })).toBeInTheDocument();
  });

  it("signs in with a Google ID token", async () => {
    vi.mocked(requestGoogleIdToken).mockResolvedValue("google-id-token");
    const user = userEvent.setup();
    renderLoginPage();

    await user.click(screen.getByRole("button", { name: "Continue with Google" }));

    await waitFor(() => expect(getAccessToken()).toBe("access-token-1"));
  });
});
