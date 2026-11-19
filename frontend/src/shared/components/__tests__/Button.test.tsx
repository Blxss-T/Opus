import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button } from "../Button";

describe("Button", () => {
  it("renders the label and stays enabled", () => {
    render(<Button type="button">Save</Button>);
    expect(screen.getByRole("button", { name: "Save" })).toBeEnabled();
  });

  it("shows a loading label and disables the control", () => {
    render(
      <Button type="button" loading>
        Save
      </Button>,
    );
    const button = screen.getByRole("button", { name: "Please wait…" });
    expect(button).toBeDisabled();
  });
});
