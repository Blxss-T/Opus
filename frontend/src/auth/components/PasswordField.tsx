import { Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { FormField } from "../../shared/components/FormField";
import { IconButton } from "../../shared/components/IconButton";

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
    <div className="relative">
      <FormField
        label={label}
        name={name}
        type={visible ? "text" : "password"}
        value={value}
        error={error}
        autoComplete={autoComplete}
        required={required}
        minLength={minLength}
        className="pr-11"
        onChange={(e) => onChange(e.target.value)}
      />
      <IconButton
        label={visible ? "Hide password" : "Show password"}
        className="absolute top-7 right-1"
        onClick={() => setVisible((current) => !current)}
      >
        {visible ? (
          <EyeOff size={16} strokeWidth={1.75} aria-hidden />
        ) : (
          <Eye size={16} strokeWidth={1.75} aria-hidden />
        )}
      </IconButton>
    </div>
  );
}
