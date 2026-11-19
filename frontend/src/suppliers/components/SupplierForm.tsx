import { useState, type FormEvent } from "react";
import { FormField } from "../../shared/components/FormField";
import { Button } from "../../shared/components/Button";
import type { CreateSupplierRequest } from "../types";

interface SupplierFormProps {
  initial: CreateSupplierRequest;
  submitLabel: string;
  submitting: boolean;
  fieldErrors: Record<string, string>;
  formError: string | null;
  readOnly?: boolean;
  onSubmit: (values: CreateSupplierRequest) => void;
}

export function SupplierForm({
  initial,
  submitLabel,
  submitting,
  fieldErrors,
  formError,
  readOnly = false,
  onSubmit,
}: SupplierFormProps) {
  const [form, setForm] = useState<CreateSupplierRequest>(initial);

  function update<K extends keyof CreateSupplierRequest>(key: K, value: CreateSupplierRequest[K]) {
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
        label="Contact person"
        name="contactPerson"
        disabled={readOnly}
        value={form.contactPerson ?? ""}
        error={fieldErrors.contactPerson}
        onChange={(e) => update("contactPerson", e.target.value)}
      />
      <FormField
        label="Email"
        name="email"
        type="email"
        disabled={readOnly}
        value={form.email ?? ""}
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
        label="Tax number"
        name="taxNumber"
        disabled={readOnly}
        value={form.taxNumber ?? ""}
        error={fieldErrors.taxNumber}
        onChange={(e) => update("taxNumber", e.target.value)}
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
