import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "../../../shared/__tests__/testProviders";
import { ProductCreatePage } from "../ProductCreatePage";

describe("ProductCreatePage", () => {
  it("requires sku, name, category, unit price, and cost price, and shows Initial stock", async () => {
    renderWithProviders(<ProductCreatePage />, { role: "ORG_ADMIN" });
    await screen.findByLabelText("SKU");

    for (const label of ["SKU", "Name", "Category", "Unit price", "Cost price"]) {
      expect(screen.getByLabelText(label)).toBeRequired();
    }
    expect(screen.getByLabelText("Initial stock")).toBeInTheDocument();
  });
});
