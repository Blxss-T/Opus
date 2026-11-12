import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatusPill, statusToneFor } from "../StatusPill";

describe("StatusPill", () => {
  it("maps order statuses to the muted tones from the design system", () => {
    expect(statusToneFor("DRAFT")).toBe("draft");
    expect(statusToneFor("CONFIRMED")).toBe("ordered");
    expect(statusToneFor("FULFILLED")).toBe("success");
    expect(statusToneFor("CANCELLED")).toBe("danger");
  });

  it("renders the status text", () => {
    render(<StatusPill tone="success">FULFILLED</StatusPill>);
    expect(screen.getByText("FULFILLED")).toBeInTheDocument();
  });
});
