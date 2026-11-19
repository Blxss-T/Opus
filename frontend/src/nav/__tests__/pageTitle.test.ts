import { describe, expect, it } from "vitest";
import { titleForPath } from "../pageTitle";

describe("titleForPath", () => {
  it("maps list, create, and detail routes", () => {
    expect(titleForPath("/dashboard")).toBe("Dashboard");
    expect(titleForPath("/products")).toBe("Products");
    expect(titleForPath("/products/new")).toBe("New product");
    expect(titleForPath("/products/abc")).toBe("Product");
    expect(titleForPath("/employees/me")).toBe("My profile");
    expect(titleForPath("/employees")).toBe("Employees");
    expect(titleForPath("/api-keys")).toBe("API keys");
  });
});
