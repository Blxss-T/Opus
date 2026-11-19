import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import * as customersApi from "../customersApi";
import { server } from "../../auth/__tests__/mockServer";

const baseUrl = "http://localhost:8080/api/v1";

function ok(data: unknown) {
  return HttpResponse.json({ success: true, data, timestamp: "" });
}

const sampleCustomer = {
  id: "cust-1",
  organizationId: "org-1",
  name: "Acme Co",
  email: "buyer@acme.test",
  phone: "555-0100",
  address: "1 Market St",
  notes: "VIP",
  active: true,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

describe("customersApi", () => {
  it("creates a customer", async () => {
    server.use(
      http.post(`${baseUrl}/customers`, async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({ name: "Acme Co", email: "buyer@acme.test" });
        return ok(sampleCustomer);
      }),
    );
    const result = await customersApi.createCustomer({ name: "Acme Co", email: "buyer@acme.test" });
    expect(result.id).toBe("cust-1");
  });

  it("lists customers with query params", async () => {
    server.use(
      http.get(`${baseUrl}/customers`, ({ request }) => {
        const url = new URL(request.url);
        expect(url.searchParams.get("q")).toBe("acme");
        expect(url.searchParams.get("page")).toBe("0");
        return ok({ content: [sampleCustomer], page: { size: 20, number: 0, totalElements: 1, totalPages: 1 } });
      }),
    );
    const result = await customersApi.listCustomers({ q: "acme", page: 0, size: 20 });
    expect(result.content).toHaveLength(1);
  });

  it("gets a customer by id", async () => {
    server.use(http.get(`${baseUrl}/customers/cust-1`, () => ok(sampleCustomer)));
    const result = await customersApi.getCustomer("cust-1");
    expect(result.name).toBe("Acme Co");
  });

  it("updates a customer", async () => {
    server.use(
      http.put(`${baseUrl}/customers/cust-1`, async ({ request }) => {
        const body = await request.json();
        expect(body).toMatchObject({ name: "Acme Co Renamed" });
        return ok({ ...sampleCustomer, name: "Acme Co Renamed" });
      }),
    );
    const result = await customersApi.updateCustomer("cust-1", { name: "Acme Co Renamed", email: "buyer@acme.test" });
    expect(result.name).toBe("Acme Co Renamed");
  });

  it("deletes (soft) a customer", async () => {
    server.use(http.delete(`${baseUrl}/customers/cust-1`, () => ok({ ...sampleCustomer, active: false })));
    const result = await customersApi.deleteCustomer("cust-1");
    expect(result.active).toBe(false);
  });
});
