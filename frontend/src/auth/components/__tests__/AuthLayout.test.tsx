import { render, screen } from "@testing-library/react";
import { MotionConfig } from "framer-motion";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AuthLayout } from "../AuthLayout";

function renderLayout(path: string, title = "Sign in") {
  return render(
    <MotionConfig reducedMotion="always">
      <MemoryRouter initialEntries={[path]}>
        <AuthLayout title={title}>
          <p>Form body</p>
        </AuthLayout>
      </MemoryRouter>
    </MotionConfig>,
  );
}

describe("AuthLayout", () => {
  it("shows the welcome panel on sign-in and keeps the form title", () => {
    renderLayout("/login");
    expect(screen.getByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Welcome back" })).toBeInTheDocument();
    expect(screen.getByText("Sign in to your account")).toBeInTheDocument();
    expect(screen.getByText("Form body")).toBeInTheDocument();
    expect(screen.getAllByAltText("Opus").length).toBeGreaterThan(0);
  });

  it("shows the get-started steps on register", () => {
    renderLayout("/register", "Register your organization");
    expect(screen.getByRole("heading", { name: "Get started with us" })).toBeInTheDocument();
    expect(screen.getByText("Sign up your account")).toBeInTheDocument();
    expect(screen.getByText("Set up your workspace")).toBeInTheDocument();
  });
});
