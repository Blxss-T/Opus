import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../../AuthContext";
import { LoginPage } from "../LoginPage";

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
  });
});
