import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import * as authApi from "../authApi";
import { server } from "./mockServer";

const baseUrl = "http://localhost:8080/api/v1";

describe("authApi", () => {
  it("sends the forgot-password request and returns the generic message", async () => {
    server.use(
      http.post(`${baseUrl}/auth/password/forgot`, async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({ email: "someone@example.com" });
        return HttpResponse.json({
          success: true,
          data: { message: "If an account exists, a reset code has been sent." },
          timestamp: "",
        });
      }),
    );

    const result = await authApi.forgotPassword({ email: "someone@example.com" });
    expect(result.message).toContain("reset code");
  });

  it("sends the OTP type through to /auth/otp/send", async () => {
    server.use(
      http.post(`${baseUrl}/auth/otp/send`, async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({ email: "someone@example.com", type: "EMAIL_VERIFICATION" });
        return HttpResponse.json({
          success: true,
          data: { success: true, message: "sent", expiresAt: "2026-01-01T00:00:00Z" },
          timestamp: "",
        });
      }),
    );

    const result = await authApi.sendOtp({ email: "someone@example.com", type: "EMAIL_VERIFICATION" });
    expect(result.success).toBe(true);
  });

  it("sends the Google ID token to /auth/google", async () => {
    server.use(
      http.post(`${baseUrl}/auth/google`, async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({ idToken: "google-id-token" });
        return HttpResponse.json({
          success: true,
          data: {
            accessToken: "access-token-1",
            refreshToken: "refresh-token-1",
            tokenType: "Bearer",
            user: {
              id: "user-1",
              organizationId: "org-1",
              email: "ada@opus.test",
              firstName: "Ada",
              lastName: "Lovelace",
              role: "ORG_ADMIN",
              active: true,
              emailVerified: true,
            },
            organization: { id: "org-1", name: "Ada's Organization", slug: "adas-organization", active: true },
          },
          timestamp: "",
        });
      }),
    );

    const result = await authApi.loginWithGoogle({ idToken: "google-id-token" });
    expect(result.user.email).toBe("ada@opus.test");
  });
});
