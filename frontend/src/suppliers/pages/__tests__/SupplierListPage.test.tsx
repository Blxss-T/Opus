import { screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "../../../shared/__tests__/testProviders";
import { server } from "../../../auth/__tests__/mockServer";
import { SupplierListPage } from "../SupplierListPage";

const baseUrl = "http://localhost:8080/api/v1";

function mockSupplierList() {
  server.use(
    http.get(`${baseUrl}/suppliers`, () =>
      HttpResponse.json({
        success: true,
        data: {
          content: [
            {
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
            },
          ],
          page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
        },
        timestamp: "",
      }),
    ),
  );
}

describe("SupplierListPage", () => {
  it("shows the New supplier link for MANAGER", async () => {
    mockSupplierList();
    renderWithProviders(<SupplierListPage />, { role: "MANAGER" });

    expect(await screen.findByText("Global Parts Ltd")).toBeInTheDocument();
    expect(screen.getByText("New supplier")).toBeInTheDocument();
  });

  it("hides the New supplier link for EMPLOYEE", async () => {
    mockSupplierList();
    renderWithProviders(<SupplierListPage />, { role: "EMPLOYEE" });

    expect(await screen.findByText("Global Parts Ltd")).toBeInTheDocument();
    expect(screen.queryByText("New supplier")).not.toBeInTheDocument();
  });

  it("shows an empty state when there are no suppliers", async () => {
    server.use(
      http.get(`${baseUrl}/suppliers`, () =>
        HttpResponse.json({
          success: true,
          data: { content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } },
          timestamp: "",
        }),
      ),
    );
    renderWithProviders(<SupplierListPage />, { role: "MANAGER" });

    expect(await screen.findByText("No suppliers found.")).toBeInTheDocument();
  });
});
