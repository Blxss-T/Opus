import { screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "../../../shared/__tests__/testProviders";
import { server } from "../../../auth/__tests__/mockServer";
import { ProductListPage } from "../ProductListPage";

const baseUrl = "http://localhost:8080/api/v1";

const sampleProduct = {
  id: "prod-1",
  organizationId: "org-1",
  sku: "WIDGET-1",
  name: "Widget",
  description: null,
  category: "Hardware",
  unitPrice: 19.99,
  costPrice: 9.99,
  stockQuantity: 50,
  reorderLevel: 10,
  active: true,
  version: 0,
  createdAt: "",
  updatedAt: "",
};

function mockProductList() {
  server.use(
    http.get(`${baseUrl}/products`, () =>
      HttpResponse.json({
        success: true,
        data: { content: [sampleProduct], page: { size: 20, number: 0, totalElements: 1, totalPages: 1 } },
        timestamp: "",
      }),
    ),
  );
}

describe("ProductListPage", () => {
  it("shows the New product link for ORG_ADMIN", async () => {
    mockProductList();
    renderWithProviders(<ProductListPage />, { role: "ORG_ADMIN" });

    expect(await screen.findByText("Widget")).toBeInTheDocument();
    expect(screen.getByText("New product")).toBeInTheDocument();
  });

  it("hides the New product link for EMPLOYEE", async () => {
    mockProductList();
    renderWithProviders(<ProductListPage />, { role: "EMPLOYEE" });

    expect(await screen.findByText("Widget")).toBeInTheDocument();
    expect(screen.queryByText("New product")).not.toBeInTheDocument();
  });

  it("shows an empty state when there are no products", async () => {
    server.use(
      http.get(`${baseUrl}/products`, () =>
        HttpResponse.json({
          success: true,
          data: { content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } },
          timestamp: "",
        }),
      ),
    );
    renderWithProviders(<ProductListPage />, { role: "ORG_ADMIN" });

    expect(await screen.findByText("No products found.")).toBeInTheDocument();
  });
});
