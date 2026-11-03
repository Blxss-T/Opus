import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../../AuthContext";
import { RegisterPage } from "../RegisterPage";

describe("RegisterPage", () => {
  it("requires every field used to create an organization", () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <RegisterPage />
        </AuthProvider>
      </MemoryRouter>,
    );

    for (const label of ["Organization name", "First name", "Last name", "Email"]) {
      expect(screen.getByLabelText(label)).toBeRequired();
    }
  });
});
