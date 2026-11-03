import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { ResetPasswordPage } from "../ResetPasswordPage";

describe("ResetPasswordPage", () => {
  it("flags mismatched passwords instead of submitting", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/password/reset?email=someone@example.com"]}>
        <ResetPasswordPage />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText("Verification code"), "123456");
    await user.type(screen.getByLabelText("New password"), "NewPassword123!");
    await user.type(screen.getByLabelText("Confirm new password"), "DoesNotMatch123!");
    await user.click(screen.getByRole("button", { name: "Reset password" }));

    expect(await screen.findByText("Passwords do not match.")).toBeInTheDocument();
  });
});
