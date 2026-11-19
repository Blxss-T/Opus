import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import * as suppliersApi from "../suppliersApi";
import { server } from "../../auth/__tests__/mockServer";

const baseUrl = "http://localhost:8080/api/v1";

function ok(data: unknown) {
  return HttpResponse.json({ success: true, data, timestamp: "" });
}

const sampleSupplier = {
  id: "sup-1",
  organizationId: "org-1",
  name: "Global Parts Ltd",
  contactPerson: "Jo Lee",
  email: "jo@globalparts.test",
  phone: "555-0200",
  address: "9 Industrial Way",
  taxNumber: "TX-123",
  active: true,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

describe("suppliersApi", () => {
  it("creates a supplier", async () => {
    server.use(
      http.post(`${baseUrl}/suppliers`, async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({ name: "Global Parts Ltd" });
        return ok(sampleSupplier);
      }),
    );
    const result = await suppliersApi.createSupplier({ name: "Global Parts Ltd" });
    expect(result.id).toBe("sup-1");
  });

  it("lists suppliers with query params", async () => {
    server.use(
      http.get(`${baseUrl}/suppliers`, ({ request }) => {
        const url = new URL(request.url);
        expect(url.searchParams.get("q")).toBe("global");
        return ok({ content: [sampleSupplier], page: { size: 20, number: 0, totalElements: 1, totalPages: 1 } });
      }),
    );
    const result = await suppliersApi.listSuppliers({ q: "global", page: 0, size: 20 });
    expect(result.content).toHaveLength(1);
  });

  it("gets a supplier by id", async () => {
    server.use(http.get(`${baseUrl}/suppliers/sup-1`, () => ok(sampleSupplier)));
    const result = await suppliersApi.getSupplier("sup-1");
    expect(result.name).toBe("Global Parts Ltd");
  });

  it("updates a supplier", async () => {
    server.use(
      http.put(`${baseUrl}/suppliers/sup-1`, async ({ request }) => {
        const body = await request.json();
        expect(body).toMatchObject({ name: "Global Parts Renamed" });
        return ok({ ...sampleSupplier, name: "Global Parts Renamed" });
      }),
    );
    const result = await suppliersApi.updateSupplier("sup-1", { name: "Global Parts Renamed" });
    expect(result.name).toBe("Global Parts Renamed");
  });

  it("deletes (soft) a supplier", async () => {
    server.use(http.delete(`${baseUrl}/suppliers/sup-1`, () => ok({ ...sampleSupplier, active: false })));
    const result = await suppliersApi.deleteSupplier("sup-1");
    expect(result.active).toBe(false);
  });
});
