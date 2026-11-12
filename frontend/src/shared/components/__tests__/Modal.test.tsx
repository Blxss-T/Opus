import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { MotionConfig } from "framer-motion";
import { Button } from "../Button";
import { Modal } from "../Modal";

function Harness() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button type="button" onClick={() => setOpen(true)}>
        Open
      </Button>
      <Modal open={open} onOpenChange={setOpen} title="Rename organization">
        <p>Confirm with your password.</p>
      </Modal>
    </>
  );
}

describe("Modal", () => {
  it("opens, exposes the title, and closes from the labelled close button", async () => {
    const user = userEvent.setup();
    render(
      <MotionConfig reducedMotion="always">
        <Harness />
      </MotionConfig>,
    );

    expect(screen.queryByText("Confirm with your password.")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Open" }));
    expect(screen.getByRole("dialog", { name: "Rename organization" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Close" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });
});
