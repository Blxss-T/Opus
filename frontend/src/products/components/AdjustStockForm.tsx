import { useState, type FormEvent } from "react";
import { FormField } from "../../shared/components/FormField";
import { Button } from "../../shared/components/Button";
import type { AdjustStockRequest, MovementType } from "../types";

const movementTypes: MovementType[] = ["INBOUND", "OUTBOUND", "ADJUSTMENT", "RETURN", "DAMAGE"];

interface AdjustStockFormProps {
  submitting: boolean;
  formError: string | null;
  onSubmit: (values: AdjustStockRequest) => void;
}

const empty: AdjustStockRequest = { quantityDelta: 0, movementType: "ADJUSTMENT", reason: "" };

export function AdjustStockForm({ submitting, formError, onSubmit }: AdjustStockFormProps) {
  const [form, setForm] = useState<AdjustStockRequest>(empty);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    onSubmit(form);
    setForm(empty);
  }

  return (
    <form onSubmit={handleSubmit} className="adjust-stock-form">
      <FormField
        label="Quantity delta (negative for outbound/damage)"
        name="quantityDelta"
        type="number"
        step="1"
        required
        value={form.quantityDelta}
        onChange={(e) => setForm((current) => ({ ...current, quantityDelta: e.target.valueAsNumber || 0 }))}
      />
      <div className="form-field">
        <label htmlFor="movementType">Movement type</label>
        <select
          id="movementType"
          className="form-field__input"
          value={form.movementType}
          onChange={(e) => setForm((current) => ({ ...current, movementType: e.target.value as MovementType }))}
        >
          {movementTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
      </div>
      <FormField
        label="Reason"
        name="reason"
        required
        value={form.reason}
        onChange={(e) => setForm((current) => ({ ...current, reason: e.target.value }))}
      />
      {formError && <p className="form-field__error">{formError}</p>}
      <Button type="submit" loading={submitting}>
        Adjust stock
      </Button>
    </form>
  );
}
