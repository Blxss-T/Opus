import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "../../../shared/__tests__/testProviders";
import { server } from "../../../auth/__tests__/mockServer";
import { SupplierCreatePage } from "../SupplierCreatePage";

const baseUrl = "http://localhost:8080/api/v1";

describe("SupplierCreatePage", () => {
  it("requires only name (email is optional for suppliers)", async () => {
    renderWithProviders(<SupplierCreatePage />, { role: "ORG_ADMIN" });
    await screen.findByLabelText("Name");
    expect(screen.getByLabelText("Name")).toBeRequired();
    expect(screen.getByLabelText("Email")).not.toBeRequired();
  });

  it("shows server-side field errors on a 400 response", async () => {
    server.use(
      http.post(`${baseUrl}/suppliers`, () =>
        HttpResponse.json(
          {
            success: false,
            error: {
              status: 400,
              error: "ERR_400",
              message: "Validation failed",
              path: "/suppliers",
              details: [{ field: "taxNumber", message: "Tax number cannot exceed 100 characters" }],
              timestamp: "",
            },
            timestamp: "",
          },
          { status: 400 },
        ),
      ),
    );

    const user = userEvent.setup();
    renderWithProviders(<SupplierCreatePage />, { role: "ORG_ADMIN" });
    await user.type(await screen.findByLabelText("Name"), "Global Parts Ltd");
    await user.click(screen.getByText("Create supplier"));

    expect(await screen.findByText("Tax number cannot exceed 100 characters")).toBeInTheDocument();
  });
});
