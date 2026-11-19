import { useState, type FormEvent } from "react";
import { FormField } from "../../shared/components/FormField";
import { Button } from "../../shared/components/Button";
import type { CreateCustomerRequest } from "../types";

interface CustomerFormProps {
  initial: CreateCustomerRequest;
  submitLabel: string;
  submitting: boolean;
  fieldErrors: Record<string, string>;
  formError: string | null;
  readOnly?: boolean;
  onSubmit: (values: CreateCustomerRequest) => void;
}

export function CustomerForm({
  initial,
  submitLabel,
  submitting,
  fieldErrors,
  formError,
  readOnly = false,
  onSubmit,
}: CustomerFormProps) {
  const [form, setForm] = useState<CreateCustomerRequest>(initial);

  function update<K extends keyof CreateCustomerRequest>(key: K, value: CreateCustomerRequest[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    onSubmit(form);
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField
        label="Name"
        name="name"
        required
        disabled={readOnly}
        value={form.name}
        error={fieldErrors.name}
        onChange={(e) => update("name", e.target.value)}
      />
      <FormField
        label="Email"
        name="email"
        type="email"
        required
        disabled={readOnly}
        value={form.email}
        error={fieldErrors.email}
        onChange={(e) => update("email", e.target.value)}
      />
      <FormField
        label="Phone"
        name="phone"
        disabled={readOnly}
        value={form.phone ?? ""}
        error={fieldErrors.phone}
        onChange={(e) => update("phone", e.target.value)}
      />
      <FormField
        label="Address"
        name="address"
        disabled={readOnly}
        value={form.address ?? ""}
        error={fieldErrors.address}
        onChange={(e) => update("address", e.target.value)}
      />
      <FormField
        label="Notes"
        name="notes"
        disabled={readOnly}
        value={form.notes ?? ""}
        error={fieldErrors.notes}
        onChange={(e) => update("notes", e.target.value)}
      />
      {formError && <p className="form-field__error">{formError}</p>}
      {!readOnly && (
        <Button type="submit" loading={submitting}>
          {submitLabel}
        </Button>
      )}
    </form>
  );
}
