import { render, screen } from "@testing-library/react";
import { MotionConfig } from "framer-motion";
import { describe, expect, it } from "vitest";
import { ToastProvider } from "../../shared/components/Toast";
import { StyleGuidePage } from "../StyleGuidePage";

describe("StyleGuidePage", () => {
  it("renders the component gallery", () => {
    render(
      <MotionConfig reducedMotion="always">
        <ToastProvider>
          <StyleGuidePage />
        </ToastProvider>
      </MotionConfig>,
    );
    expect(screen.getByRole("heading", { name: "Style guide" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Primary" })).toBeInTheDocument();
    expect(screen.getByText("FULFILLED")).toBeInTheDocument();
  });
});
