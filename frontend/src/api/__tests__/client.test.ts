import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiGet, apiPost, setUnauthorizedHandler } from "../client";
import { ApiError } from "../types";
import { server } from "../../auth/__tests__/mockServer";
import { clearTokens, setTokens } from "../../auth/tokenStorage";

const baseUrl = "http://localhost:8080/api/v1";

describe("api client", () => {
  beforeEach(() => {
    clearTokens();
    setUnauthorizedHandler(() => {});
  });

  it("unwraps the ApiResponse envelope down to data", async () => {
    server.use(
      http.get(`${baseUrl}/widgets`, () =>
        HttpResponse.json({ success: true, data: { id: "1" }, timestamp: new Date().toISOString() }),
      ),
    );
    const result = await apiGet<{ id: string }>("/widgets");
    expect(result).toEqual({ id: "1" });
  });

  it("throws an ApiError carrying the field-level validation details", async () => {
    server.use(
      http.post(`${baseUrl}/widgets`, () =>
        HttpResponse.json(
          {
            success: false,
            error: {
              status: 400,
              error: "ERR_400",
              message: "Validation failed",
              path: "/widgets",
              details: [{ field: "name", message: "must not be blank" }],
              timestamp: new Date().toISOString(),
            },
            timestamp: new Date().toISOString(),
          },
          { status: 400 },
        ),
      ),
    );

    await expect(apiPost("/widgets", {})).rejects.toSatisfy((error: unknown) => {
      expect(error).toBeInstanceOf(ApiError);
      const apiError = error as ApiError;
      expect(apiError.code).toBe("ERR_400");
      expect(apiError.fieldErrors).toEqual([{ field: "name", message: "must not be blank" }]);
      return true;
    });
  });

  it("retries once after a silent refresh on 401, then succeeds", async () => {
    setTokens("expired-token", "refresh-token-1");
    let attempt = 0;
    server.use(
      http.get(`${baseUrl}/widgets`, ({ request }) => {
        attempt += 1;
        const auth = request.headers.get("authorization");
        if (attempt === 1) {
          expect(auth).toBe("Bearer expired-token");
          return HttpResponse.json(
            {
              success: false,
              error: { status: 401, error: "ERR_401", message: "expired", path: "/widgets", timestamp: "" },
              timestamp: "",
            },
            { status: 401 },
          );
        }
        expect(auth).toBe("Bearer access-token-2");
        return HttpResponse.json({ success: true, data: { ok: true }, timestamp: "" });
      }),
    );

    const result = await apiGet<{ ok: boolean }>("/widgets");
    expect(result).toEqual({ ok: true });
    expect(attempt).toBe(2);
  });

  it("calls the unauthorized handler and clears tokens when the refresh itself fails", async () => {
    setTokens("expired-token", "dead-refresh-token");
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    server.use(
      http.get(`${baseUrl}/widgets`, () =>
        HttpResponse.json(
          { success: false, error: { status: 401, error: "ERR_401", message: "expired", path: "", timestamp: "" }, timestamp: "" },
          { status: 401 },
        ),
      ),
      http.post(`${baseUrl}/auth/refresh`, () =>
        HttpResponse.json(
          { success: false, error: { status: 401, error: "ERR_401", message: "revoked", path: "", timestamp: "" }, timestamp: "" },
          { status: 401 },
        ),
      ),
    );

    await expect(apiGet("/widgets")).rejects.toBeInstanceOf(ApiError);
    expect(handler).toHaveBeenCalledTimes(1);
  });
});
