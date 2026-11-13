import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MotionConfig } from "framer-motion";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AuthProvider } from "../../auth/AuthContext";
import { mockAuthenticatedAs } from "../../shared/__tests__/testProviders";
import { ComingNextPage } from "../../shared/components/ComingNextPage";
import { ToastProvider } from "../../shared/components/Toast";
import type { Role } from "../../auth/types";
import { AppShell } from "../AppShell";

function renderShell(role: Role, initialEntry = "/dashboard") {
  mockAuthenticatedAs(role);
  return render(
    <MotionConfig reducedMotion="always">
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <MemoryRouter initialEntries={[initialEntry]}>
          <AuthProvider>
            <ToastProvider>
              <Routes>
                <Route element={<AppShell />}>
                  <Route path="/dashboard" element={<p>Dashboard body</p>} />
                  <Route path="/products" element={<p>Products body</p>} />
                  <Route path="/purchasing" element={<ComingNextPage />} />
                  <Route path="/users" element={<ComingNextPage />} />
                  <Route path="/employees" element={<ComingNextPage />} />
                  <Route path="/employees/me" element={<ComingNextPage />} />
                  <Route path="/api-keys" element={<ComingNextPage />} />
                  <Route path="/audit" element={<ComingNextPage />} />
                  <Route path="/organizations" element={<ComingNextPage />} />
                </Route>
              </Routes>
            </ToastProvider>
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    </MotionConfig>,
  );
}

describe("AppShell", () => {
  it("shows the Jobgio sidebar, page title, and user card for an admin", async () => {
    renderShell("ORG_ADMIN");

    expect(await screen.findByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Users" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "API keys" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Audit log" })).toBeInTheDocument();
    expect(screen.getByText("admin@opus-demo.test")).toBeInTheDocument();
    expect(screen.getByText("Opus Demo Trading")).toBeInTheDocument();
    expect(screen.getByText("Admin")).toBeInTheDocument();
  });

  it("lets a manager see Users but not API keys or Audit", async () => {
    renderShell("MANAGER");

    expect(await screen.findByRole("link", { name: "Users" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Employees" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "API keys" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Audit log" })).not.toBeInTheDocument();
  });

  it("gives employees My profile and hides admin-only people links", async () => {
    renderShell("EMPLOYEE");

    expect(await screen.findByRole("link", { name: "My profile" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Employees" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "API keys" })).not.toBeInTheDocument();
  });

  it("opens a coming-next empty state for unbuilt screens", async () => {
    const user = userEvent.setup();
    renderShell("ORG_ADMIN");

    await screen.findByRole("link", { name: "Purchase orders" });
    await user.click(screen.getByRole("link", { name: "Purchase orders" }));

    expect(await screen.findByRole("heading", { name: "Purchase orders" })).toBeInTheDocument();
    expect(screen.getByText("This screen is coming next.")).toBeInTheDocument();
  });

  it("opens change password from the account menu", async () => {
    const user = userEvent.setup();
    renderShell("ORG_ADMIN");

    await screen.findByRole("button", { name: "Demo Admin account menu" });
    await user.click(screen.getByRole("button", { name: "Demo Admin account menu" }));
    await user.click(screen.getByRole("menuitem", { name: "Change password" }));

    expect(await screen.findByRole("dialog", { name: "Change password" })).toBeInTheDocument();
  });
});
