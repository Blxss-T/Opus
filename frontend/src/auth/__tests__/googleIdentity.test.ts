import { describe, expect, it } from "vitest";
import { requestGoogleIdToken } from "../googleIdentity";

describe("googleIdentity", () => {
  it("rejects when the Google client id is not configured", async () => {
    await expect(requestGoogleIdToken()).rejects.toThrow("Google sign-in is not configured.");
  });
});
