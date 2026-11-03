import { useState } from "react";
import { FormField } from "../../shared/components/FormField";

interface PasswordFieldProps {
  label: string;
  name: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  autoComplete?: string;
  required?: boolean;
  minLength?: number;
}

export function PasswordField({
  label,
  name,
  value,
  onChange,
  error,
  autoComplete,
  required,
  minLength,
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="password-field">
      <FormField
        label={label}
        name={name}
        type={visible ? "text" : "password"}
        value={value}
        error={error}
        autoComplete={autoComplete}
        required={required}
        minLength={minLength}
        onChange={(e) => onChange(e.target.value)}
      />
      <button
        type="button"
        className="password-field__toggle"
        onClick={() => setVisible((current) => !current)}
      >
        {visible ? "Hide" : "Show"}
      </button>
    </div>
  );
}
