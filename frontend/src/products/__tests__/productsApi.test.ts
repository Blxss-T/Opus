import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import * as productsApi from "../productsApi";
import { server } from "../../auth/__tests__/mockServer";

const baseUrl = "http://localhost:8080/api/v1";

function ok(data: unknown) {
  return HttpResponse.json({ success: true, data, timestamp: "" });
}

const sampleProduct = {
  id: "prod-1",
  organizationId: "org-1",
  sku: "WIDGET-1",
  name: "Widget",
  description: "A widget",
  category: "Hardware",
  unitPrice: 19.99,
  costPrice: 9.99,
  stockQuantity: 50,
  reorderLevel: 10,
  active: true,
  version: 0,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
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

describe("productsApi", () => {
  it("creates a product", async () => {
    server.use(
      http.post(`${baseUrl}/products`, async ({ request }) => {
        const body = await request.json();
        expect(body).toMatchObject({ sku: "WIDGET-1", name: "Widget" });
        return ok(sampleProduct);
      }),
    );
    const result = await productsApi.createProduct({
      sku: "WIDGET-1",
      name: "Widget",
      category: "Hardware",
      unitPrice: 19.99,
      costPrice: 9.99,
      initialStock: 50,
      reorderLevel: 10,
    });
    expect(result.id).toBe("prod-1");
  });

  it("lists products with query params", async () => {
    server.use(
      http.get(`${baseUrl}/products`, ({ request }) => {
        const url = new URL(request.url);
        expect(url.searchParams.get("q")).toBe("widget");
        expect(url.searchParams.get("category")).toBe("Hardware");
        return ok({ content: [sampleProduct], page: { size: 20, number: 0, totalElements: 1, totalPages: 1 } });
      }),
    );
    const result = await productsApi.listProducts({ q: "widget", category: "Hardware", page: 0, size: 20 });
    expect(result.content).toHaveLength(1);
  });

  it("gets a product by id", async () => {
    server.use(http.get(`${baseUrl}/products/prod-1`, () => ok(sampleProduct)));
    const result = await productsApi.getProduct("prod-1");
    expect(result.sku).toBe("WIDGET-1");
  });

  it("updates a product", async () => {
    server.use(
      http.put(`${baseUrl}/products/prod-1`, async ({ request }) => {
        const body = await request.json();
        expect(body).not.toHaveProperty("sku");
        expect(body).not.toHaveProperty("initialStock");
        return ok({ ...sampleProduct, name: "Widget Pro" });
      }),
    );
    const result = await productsApi.updateProduct("prod-1", {
      name: "Widget Pro",
      category: "Hardware",
      unitPrice: 19.99,
      costPrice: 9.99,
      reorderLevel: 10,
    });
    expect(result.name).toBe("Widget Pro");
  });

  it("deletes (soft) a product", async () => {
    server.use(http.delete(`${baseUrl}/products/prod-1`, () => ok({ ...sampleProduct, active: false })));
    const result = await productsApi.deleteProduct("prod-1");
    expect(result.active).toBe(false);
  });

  it("adjusts stock with a signed delta, sent through verbatim", async () => {
    server.use(
      http.post(`${baseUrl}/products/prod-1/adjust-stock`, async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({ quantityDelta: -5, movementType: "OUTBOUND", reason: "Damaged in transit" });
        return ok({ ...sampleProduct, stockQuantity: 45 });
      }),
    );
    const result = await productsApi.adjustStock("prod-1", {
      quantityDelta: -5,
      movementType: "OUTBOUND",
      reason: "Damaged in transit",
    });
    expect(result.stockQuantity).toBe(45);
  });

  it("gets movement history filtered by type", async () => {
    server.use(
      http.get(`${baseUrl}/products/prod-1/movements`, ({ request }) => {
        const url = new URL(request.url);
        expect(url.searchParams.get("type")).toBe("INBOUND");
        return ok({ content: [sampleMovement], page: { size: 20, number: 0, totalElements: 1, totalPages: 1 } });
      }),
    );
    const result = await productsApi.getMovements("prod-1", { type: "INBOUND", page: 0, size: 20 });
    expect(result.content).toHaveLength(1);
  });
});
