import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "../../../shared/__tests__/testProviders";
import { server } from "../../../auth/__tests__/mockServer";
import { CustomerCreatePage } from "../CustomerCreatePage";

const baseUrl = "http://localhost:8080/api/v1";

describe("CustomerCreatePage", () => {
  it("requires name and email", async () => {
    renderWithProviders(<CustomerCreatePage />, { role: "ORG_ADMIN" });
    await screen.findByLabelText("Name");
    expect(screen.getByLabelText("Name")).toBeRequired();
    expect(screen.getByLabelText("Email")).toBeRequired();
  });

  it("shows server-side field errors on a 400 response", async () => {
    server.use(
      http.post(`${baseUrl}/customers`, () =>
        HttpResponse.json(
          {
            success: false,
            error: {
              status: 400,
              error: "ERR_400",
              message: "Validation failed",
              path: "/customers",
              details: [{ field: "email", message: "Invalid email format" }],
              timestamp: "",
            },
            timestamp: "",
          },
          { status: 400 },
        ),
      ),
    );

    const user = userEvent.setup();
    renderWithProviders(<CustomerCreatePage />, { role: "ORG_ADMIN" });
    await user.type(await screen.findByLabelText("Name"), "Acme Co");
    // A browser-valid email format, so native constraint validation doesn't
    // block submission before the (mocked) server-side 400 can round-trip.
    await user.type(screen.getByLabelText("Email"), "someone@example.com");
    await user.click(screen.getByText("Create customer"));

    expect(await screen.findByText("Invalid email format")).toBeInTheDocument();
  });
});
