import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { ForgotPasswordPage } from "../ForgotPasswordPage";

describe("ForgotPasswordPage", () => {
  it("requires an email before submitting", () => {
    render(
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>,
    );
    expect(screen.getByLabelText("Email")).toBeRequired();
  });
});
