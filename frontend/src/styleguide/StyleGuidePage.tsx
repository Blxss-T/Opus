import { useState } from "react";
import { motion } from "framer-motion";
import { Plus, Search } from "lucide-react";
import { Button } from "../shared/components/Button";
import { Card } from "../shared/components/Card";
import { EmptyState } from "../shared/components/EmptyState";
import { FormField } from "../shared/components/FormField";
import { IconButton } from "../shared/components/IconButton";
import { Modal } from "../shared/components/Modal";
import { Select } from "../shared/components/Select";
import { Spinner } from "../shared/components/Spinner";
import { StatusPill, type StatusTone } from "../shared/components/StatusPill";
import { useToast } from "../shared/components/Toast";
import { fadeIn, motionEase } from "../shared/motion";

const tones: { tone: StatusTone; label: string }[] = [
  { tone: "draft", label: "DRAFT" },
  { tone: "ordered", label: "CONFIRMED" },
  { tone: "success", label: "FULFILLED" },
  { tone: "warning", label: "Low stock" },
  { tone: "danger", label: "CANCELLED" },
];

export function StyleGuidePage() {
  const { show } = useToast();
  const [modalOpen, setModalOpen] = useState(false);
  const [selectValue, setSelectValue] = useState("DRAFT");

  return (
    <div className="min-h-screen bg-bg p-8">
      <div className="mx-auto flex max-w-4xl flex-col gap-8">
        <header>
          <p className="text-small font-semibold tracking-wide text-primary-600">Opus</p>
          <h1 className="text-page text-heading">Style guide</h1>
          <p className="text-body text-muted">Internal preview of design-system components and states. DEV only.</p>
        </header>

        <Card>
          <h2 className="text-card">Buttons</h2>
          <div className="flex flex-wrap items-center gap-3">
            <Button type="button">Primary</Button>
            <Button type="button" variant="secondary">
              Secondary
            </Button>
            <Button type="button" variant="danger">
              Danger
            </Button>
            <Button type="button" loading>
              Saving
            </Button>
            <Button type="button" disabled>
              Disabled
            </Button>
            <IconButton label="Search">
              <Search size={18} strokeWidth={1.75} aria-hidden />
            </IconButton>
            <IconButton label="Add item">
              <Plus size={18} strokeWidth={1.75} aria-hidden />
            </IconButton>
          </div>
        </Card>

        <Card>
          <h2 className="text-card">Status pills</h2>
          <div className="flex flex-wrap gap-2">
            {tones.map((item) => (
              <StatusPill key={item.tone} tone={item.tone}>
                {item.label}
              </StatusPill>
            ))}
          </div>
        </Card>

        <Card>
          <h2 className="text-card">Inputs</h2>
          <div className="grid max-w-md gap-2">
            <FormField label="Email" name="styleguide-email" placeholder="name@company.com" />
            <FormField label="With error" name="styleguide-error" error="This field is required." />
            <Select
              label="Status"
              name="styleguide-status"
              value={selectValue}
              onValueChange={setSelectValue}
              options={[
                { value: "DRAFT", label: "Draft" },
                { value: "CONFIRMED", label: "Confirmed" },
                { value: "FULFILLED", label: "Fulfilled" },
              ]}
            />
          </div>
        </Card>

        <Card>
          <h2 className="text-card">Modal and toast</h2>
          <div className="flex flex-wrap gap-3">
            <Button type="button" onClick={() => setModalOpen(true)}>
              Open modal
            </Button>
            <Button type="button" variant="secondary" onClick={() => show("Saved successfully.", "success")}>
              Success toast
            </Button>
            <Button type="button" variant="secondary" onClick={() => show("Something went wrong.", "error")}>
              Error toast
            </Button>
          </div>
          <Modal
            open={modalOpen}
            onOpenChange={setModalOpen}
            title="Example modal"
            description="Focus is trapped while this dialog is open."
          >
            <p className="mb-4 text-body text-muted">Radix Dialog plus a short motion enter/exit.</p>
            <Button type="button" onClick={() => setModalOpen(false)}>
              Close
            </Button>
          </Modal>
        </Card>

        <div className="grid gap-4 md:grid-cols-2">
          <Card>
            <h2 className="text-card">Empty</h2>
            <EmptyState message="No records match this filter." />
          </Card>
          <Card>
            <h2 className="text-card">Loading</h2>
            <Spinner label="Loading preview…" />
          </Card>
        </div>

        <Card>
          <h2 className="text-card">Motion</h2>
          <motion.div
            className="h-16 rounded-md bg-primary-600"
            variants={fadeIn}
            initial="initial"
            animate="animate"
            transition={motionEase}
          />
        </Card>
      </div>
    </div>
  );
}
