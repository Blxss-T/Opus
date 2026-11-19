import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../../shared/__tests__/testProviders";
import { server } from "../../../auth/__tests__/mockServer";
import { SupplierDetailPage } from "../SupplierDetailPage";

const baseUrl = "http://localhost:8080/api/v1";

const sampleSupplier = {
  id: "sup-1",
  organizationId: "org-1",
  name: "Global Parts Ltd",
  contactPerson: "Jo Lee",
  email: "jo@globalparts.test",
  phone: "555-0200",
  address: null,
  taxNumber: null,
  active: true,
  createdAt: "",
  updatedAt: "",
};

function mockGetSupplier() {
  server.use(http.get(`${baseUrl}/suppliers/sup-1`, () => HttpResponse.json({ success: true, data: sampleSupplier, timestamp: "" })));
}

function renderDetail(role: "ORG_ADMIN" | "EMPLOYEE") {
  return renderWithProviders(<SupplierDetailPage />, {
    role,
    route: { path: "/suppliers/:id", initialEntry: "/suppliers/sup-1" },
  });
}

describe("SupplierDetailPage", () => {
  it("renders an editable form with a delete button for ORG_ADMIN", async () => {
    mockGetSupplier();
    renderDetail("ORG_ADMIN");

    const nameInput = await screen.findByLabelText("Name");
    expect(nameInput).toHaveValue("Global Parts Ltd");
    expect(nameInput).not.toBeDisabled();
    expect(screen.getByText("Delete supplier")).toBeInTheDocument();
  });

  it("renders a read-only form with no delete button for EMPLOYEE", async () => {
    mockGetSupplier();
    renderDetail("EMPLOYEE");

    const nameInput = await screen.findByLabelText("Name");
    expect(nameInput).toBeDisabled();
    expect(screen.queryByText("Delete supplier")).not.toBeInTheDocument();
  });

  it("deletes the supplier after confirmation and navigates away", async () => {
    mockGetSupplier();
    let deleteCalled = false;
    server.use(
      http.delete(`${baseUrl}/suppliers/sup-1`, () => {
        deleteCalled = true;
        return HttpResponse.json({ success: true, data: { ...sampleSupplier, active: false }, timestamp: "" });
      }),
    );
    vi.spyOn(window, "confirm").mockReturnValue(true);

    const user = userEvent.setup();
    renderDetail("ORG_ADMIN");
    await screen.findByLabelText("Name");
    await user.click(screen.getByText("Delete supplier"));

    await vi.waitFor(() => expect(deleteCalled).toBe(true));
  });
});
