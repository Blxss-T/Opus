import { screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "../../../shared/__tests__/testProviders";
import { server } from "../../../auth/__tests__/mockServer";
import { CustomerListPage } from "../CustomerListPage";

const baseUrl = "http://localhost:8080/api/v1";

function mockCustomerList() {
  server.use(
    http.get(`${baseUrl}/customers`, () =>
      HttpResponse.json({
        success: true,
        data: {
          content: [
            {
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
            },
          ],
          page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
        },
        timestamp: "",
      }),
    ),
  );
}

describe("CustomerListPage", () => {
  it("shows the New customer link for ORG_ADMIN", async () => {
    mockCustomerList();
    renderWithProviders(<CustomerListPage />, { role: "ORG_ADMIN" });

    expect(await screen.findByText("Acme Co")).toBeInTheDocument();
    expect(screen.getByText("New customer")).toBeInTheDocument();
  });

  it("hides the New customer link for EMPLOYEE", async () => {
    mockCustomerList();
    renderWithProviders(<CustomerListPage />, { role: "EMPLOYEE" });

    expect(await screen.findByText("Acme Co")).toBeInTheDocument();
    expect(screen.queryByText("New customer")).not.toBeInTheDocument();
  });

  it("shows an empty state when there are no customers", async () => {
    server.use(
      http.get(`${baseUrl}/customers`, () =>
        HttpResponse.json({
          success: true,
          data: { content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } },
          timestamp: "",
        }),
      ),
    );
    renderWithProviders(<CustomerListPage />, { role: "ORG_ADMIN" });

    expect(await screen.findByText("No customers found.")).toBeInTheDocument();
  });
});
