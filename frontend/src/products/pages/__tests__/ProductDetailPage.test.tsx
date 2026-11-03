import { fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../../shared/__tests__/testProviders";
import { server } from "../../../auth/__tests__/mockServer";
import { ProductDetailPage } from "../ProductDetailPage";

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

const sampleMovement = {
  id: "mv-1",
  productId: "prod-1",
  quantityDelta: 10,
  previousQuantity: 40,
  newQuantity: 50,
  movementType: "INBOUND",
  reason: "Restock",
  performedBy: "user-1",
  performedByEmail: "admin@opus-demo.test",
  createdAt: "2026-01-01T00:00:00Z",
};

function mockProductAndMovements() {
  server.use(
    http.get(`${baseUrl}/products/prod-1`, () => HttpResponse.json({ success: true, data: sampleProduct, timestamp: "" })),
    http.get(`${baseUrl}/products/prod-1/movements`, () =>
      HttpResponse.json({
        success: true,
        data: { content: [sampleMovement], page: { size: 20, number: 0, totalElements: 1, totalPages: 1 } },
        timestamp: "",
      }),
    ),
  );
}

function renderDetail(role: "ORG_ADMIN" | "EMPLOYEE") {
  return renderWithProviders(<ProductDetailPage />, {
    role,
    route: { path: "/products/:id", initialEntry: "/products/prod-1" },
  });
}

describe("ProductDetailPage", () => {
  it("shows an editable, disabled-SKU form, adjust-stock, and delete for ORG_ADMIN", async () => {
    mockProductAndMovements();
    renderDetail("ORG_ADMIN");

    const skuInput = await screen.findByLabelText("SKU");
    expect(skuInput).toHaveValue("WIDGET-1");
    expect(skuInput).toBeDisabled();
    expect(screen.getByLabelText("Name")).not.toBeDisabled();
    expect(screen.getByText("Delete product")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Adjust stock" })).toBeInTheDocument();
    expect(await screen.findByText("Restock")).toBeInTheDocument();
  });

  it("hides adjust-stock and delete for EMPLOYEE but still shows movements", async () => {
    mockProductAndMovements();
    renderDetail("EMPLOYEE");

    expect(await screen.findByLabelText("Name")).toBeDisabled();
    expect(screen.queryByText("Delete product")).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Adjust stock" })).not.toBeInTheDocument();
    expect(await screen.findByText("Restock")).toBeInTheDocument();
  });

  it("submits an adjust-stock request with the signed delta and refetches movements", async () => {
    mockProductAndMovements();
    let adjustBody: unknown;
    server.use(
      http.post(`${baseUrl}/products/prod-1/adjust-stock`, async ({ request }) => {
        adjustBody = await request.json();
        return HttpResponse.json({ success: true, data: { ...sampleProduct, stockQuantity: 45 }, timestamp: "" });
      }),
    );

    const user = userEvent.setup();
    renderDetail("ORG_ADMIN");
    await screen.findByRole("heading", { name: "Adjust stock" });

    const quantityInput = screen.getByLabelText("Quantity delta (negative for outbound/damage)");
    // userEvent.type() drops the leading "-" on type="number" inputs in
    // jsdom; set the value directly instead of simulating keystrokes.
    fireEvent.change(quantityInput, { target: { value: "-5" } });
    await user.type(screen.getByLabelText("Reason"), "Damaged in transit");
    await user.click(screen.getByText("Adjust stock", { selector: "button" }));

    await vi.waitFor(() =>
      expect(adjustBody).toEqual({ quantityDelta: -5, movementType: "ADJUSTMENT", reason: "Damaged in transit" }),
    );
  });

  it("re-queries movements when the type filter changes", async () => {
    mockProductAndMovements();
    let lastTypeParam: string | null = null;
    server.use(
      http.get(`${baseUrl}/products/prod-1/movements`, ({ request }) => {
        lastTypeParam = new URL(request.url).searchParams.get("type");
        return HttpResponse.json({
          success: true,
          data: { content: [sampleMovement], page: { size: 20, number: 0, totalElements: 1, totalPages: 1 } },
          timestamp: "",
        });
      }),
    );

    const user = userEvent.setup();
    renderDetail("ORG_ADMIN");
    await screen.findByText("Restock");

    const filterSelect = screen.getByLabelText("Filter by type");
    await user.selectOptions(filterSelect, "OUTBOUND");

    await vi.waitFor(() => expect(lastTypeParam).toBe("OUTBOUND"));
  });

  it("deletes the product after confirmation and navigates away", async () => {
    mockProductAndMovements();
    let deleteCalled = false;
    server.use(
      http.delete(`${baseUrl}/products/prod-1`, () => {
        deleteCalled = true;
        return HttpResponse.json({ success: true, data: { ...sampleProduct, active: false }, timestamp: "" });
      }),
    );
    vi.spyOn(window, "confirm").mockReturnValue(true);

    const user = userEvent.setup();
    renderDetail("ORG_ADMIN");
    await screen.findByText("Delete product");
    await user.click(screen.getByText("Delete product"));

    await vi.waitFor(() => expect(deleteCalled).toBe(true));
  });
});
