import { FormField } from "../../shared/components/FormField";

interface OtpInputProps {
  value: string;
  onChange: (value: string) => void;
  error?: string;
}

// A 6-digit OTP code entry field, shared across the email-verification and
// password-reset flows — both send/verify against the same /auth/otp/* API.
export function OtpInput({ value, onChange, error }: OtpInputProps) {
  return (
    <FormField
      label="Verification code"
      name="code"
      inputMode="numeric"
      pattern="[0-9]{6}"
      maxLength={6}
      autoComplete="one-time-code"
      value={value}
      error={error}
      onChange={(e) => onChange(e.target.value.replace(/\D/g, "").slice(0, 6))}
    />
  );
}
