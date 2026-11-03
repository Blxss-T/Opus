import { useState, type FormEvent } from "react";
import { FormField } from "../../shared/components/FormField";
import { Button } from "../../shared/components/Button";
import type { CreateProductRequest, UpdateProductRequest } from "../types";

export interface ProductFormValues {
  sku: string;
  name: string;
  description: string;
  category: string;
  unitPrice: number;
  costPrice: number;
  initialStock: number;
  reorderLevel: number;
}

interface ProductFormProps {
  mode: "create" | "edit";
  initial: ProductFormValues;
  submitLabel: string;
  submitting: boolean;
  fieldErrors: Record<string, string>;
  formError: string | null;
  readOnly?: boolean;
  onSubmit: (values: CreateProductRequest | UpdateProductRequest) => void;
}

export function ProductForm({
  mode,
  initial,
  submitLabel,
  submitting,
  fieldErrors,
  formError,
  readOnly = false,
  onSubmit,
}: ProductFormProps) {
  const [form, setForm] = useState<ProductFormValues>(initial);

  function update<K extends keyof ProductFormValues>(key: K, value: ProductFormValues[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (mode === "create") {
      onSubmit({
        sku: form.sku,
        name: form.name,
        description: form.description,
        category: form.category,
        unitPrice: form.unitPrice,
        costPrice: form.costPrice,
        initialStock: form.initialStock,
        reorderLevel: form.reorderLevel,
      });
    } else {
      onSubmit({
        name: form.name,
        description: form.description,
        category: form.category,
        unitPrice: form.unitPrice,
        costPrice: form.costPrice,
        reorderLevel: form.reorderLevel,
      });
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField
        label="SKU"
        name="sku"
        required
        disabled={mode === "edit" || readOnly}
        value={form.sku}
        error={fieldErrors.sku}
        onChange={(e) => update("sku", e.target.value)}
      />
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
        label="Description"
        name="description"
        disabled={readOnly}
        value={form.description}
        error={fieldErrors.description}
        onChange={(e) => update("description", e.target.value)}
      />
      <FormField
        label="Category"
        name="category"
        required
        disabled={readOnly}
        value={form.category}
        error={fieldErrors.category}
        onChange={(e) => update("category", e.target.value)}
      />
      <FormField
        label="Unit price"
        name="unitPrice"
        type="number"
        step="0.01"
        min="0"
        required
        disabled={readOnly}
        value={form.unitPrice}
        error={fieldErrors.unitPrice}
        onChange={(e) => update("unitPrice", e.target.valueAsNumber || 0)}
      />
      <FormField
        label="Cost price"
        name="costPrice"
        type="number"
        step="0.01"
        min="0"
        required
        disabled={readOnly}
        value={form.costPrice}
        error={fieldErrors.costPrice}
        onChange={(e) => update("costPrice", e.target.valueAsNumber || 0)}
      />
      {mode === "create" && (
        <FormField
          label="Initial stock"
          name="initialStock"
          type="number"
          step="1"
          min="0"
          disabled={readOnly}
          value={form.initialStock}
          error={fieldErrors.initialStock}
          onChange={(e) => update("initialStock", e.target.valueAsNumber || 0)}
        />
      )}
      <FormField
        label="Reorder level"
        name="reorderLevel"
        type="number"
        step="1"
        min="0"
        disabled={readOnly}
        value={form.reorderLevel}
        error={fieldErrors.reorderLevel}
        onChange={(e) => update("reorderLevel", e.target.valueAsNumber || 0)}
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
