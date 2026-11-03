import type { InputHTMLAttributes } from "react";

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export function FormField({ label, error, id, className, ...rest }: FormFieldProps) {
  const fieldId = id ?? rest.name;
  return (
    <div className="form-field">
      <label htmlFor={fieldId}>{label}</label>
      <input id={fieldId} className={["form-field__input", className].filter(Boolean).join(" ")} {...rest} />
      {error && <p className="form-field__error">{error}</p>}
    </div>
  );
}
