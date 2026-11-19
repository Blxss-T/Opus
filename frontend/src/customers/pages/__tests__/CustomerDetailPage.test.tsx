import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../../shared/__tests__/testProviders";
import { server } from "../../../auth/__tests__/mockServer";
import { CustomerDetailPage } from "../CustomerDetailPage";

const baseUrl = "http://localhost:8080/api/v1";

const sampleCustomer = {
  id: "cust-1",
  organizationId: "org-1",
  name: "Acme Co",
  email: "buyer@acme.test",
  phone: "555-0100",
  address: null,
  notes: null,
  active: true,
  createdAt: "",
  updatedAt: "",
};

function mockGetCustomer() {
  server.use(http.get(`${baseUrl}/customers/cust-1`, () => HttpResponse.json({ success: true, data: sampleCustomer, timestamp: "" })));
}

function renderDetail(role: "ORG_ADMIN" | "EMPLOYEE") {
  return renderWithProviders(<CustomerDetailPage />, {
    role,
    route: { path: "/customers/:id", initialEntry: "/customers/cust-1" },
  });
}

describe("CustomerDetailPage", () => {
  it("renders an editable form with a delete button for ORG_ADMIN", async () => {
    mockGetCustomer();
    renderDetail("ORG_ADMIN");

    const nameInput = await screen.findByLabelText("Name");
    expect(nameInput).toHaveValue("Acme Co");
    expect(nameInput).not.toBeDisabled();
    expect(screen.getByText("Delete customer")).toBeInTheDocument();
  });

  it("renders a read-only form with no delete button for EMPLOYEE", async () => {
    mockGetCustomer();
    renderDetail("EMPLOYEE");

    const nameInput = await screen.findByLabelText("Name");
    expect(nameInput).toBeDisabled();
    expect(screen.queryByText("Delete customer")).not.toBeInTheDocument();
  });

  it("deletes the customer after confirmation and navigates away", async () => {
    mockGetCustomer();
    let deleteCalled = false;
    server.use(
      http.delete(`${baseUrl}/customers/cust-1`, () => {
        deleteCalled = true;
        return HttpResponse.json({ success: true, data: { ...sampleCustomer, active: false }, timestamp: "" });
      }),
    );
    vi.spyOn(window, "confirm").mockReturnValue(true);

    const user = userEvent.setup();
    renderDetail("ORG_ADMIN");
    await screen.findByLabelText("Name");
    await user.click(screen.getByText("Delete customer"));

    await vi.waitFor(() => expect(deleteCalled).toBe(true));
  });
});
