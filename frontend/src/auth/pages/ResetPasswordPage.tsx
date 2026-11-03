import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { AuthLayout } from "../components/AuthLayout";
import { PasswordField } from "../components/PasswordField";
import { OtpInput } from "../components/OtpInput";
import { FormField } from "../../shared/components/FormField";
import { Button } from "../../shared/components/Button";
import * as authApi from "../authApi";
import { toErrorMessage, toFieldErrorMap } from "../../shared/utils/fieldErrors";

export function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [email, setEmail] = useState(searchParams.get("email") ?? "");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [resendMessage, setResendMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleResend() {
    setResendMessage(null);
    try {
      await authApi.sendOtp({ email, type: "PASSWORD_RESET" });
      setResendMessage("A new code has been sent.");
    } catch (error) {
      setResendMessage(toErrorMessage(error));
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setFieldErrors({});

    if (newPassword !== confirmPassword) {
      setFieldErrors({ confirmPassword: "Passwords do not match." });
      return;
    }

    setSubmitting(true);
    try {
      await authApi.resetPassword({ email, code, newPassword });
      navigate("/login", { replace: true, state: { passwordReset: true } });
    } catch (error) {
      const fields = toFieldErrorMap(error);
      if (Object.keys(fields).length > 0) {
        setFieldErrors(fields);
      } else {
        setFormError(toErrorMessage(error));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout title="Reset your password">
      <form onSubmit={handleSubmit}>
        <FormField
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          required
          value={email}
          error={fieldErrors.email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <OtpInput value={code} onChange={setCode} error={fieldErrors.code} />
        <PasswordField
          label="New password"
          name="newPassword"
          autoComplete="new-password"
          required
          minLength={8}
          value={newPassword}
          error={fieldErrors.newPassword}
          onChange={setNewPassword}
        />
        <PasswordField
          label="Confirm new password"
          name="confirmPassword"
          autoComplete="new-password"
          required
          minLength={8}
          value={confirmPassword}
          error={fieldErrors.confirmPassword}
          onChange={setConfirmPassword}
        />
        {formError && <p className="form-field__error">{formError}</p>}
        <Button type="submit" loading={submitting}>
          Reset password
        </Button>
      </form>
      <button type="button" className="link-button" onClick={handleResend}>
        Resend code
      </button>
      {resendMessage && <p>{resendMessage}</p>}
      <p>
        <Link to="/login">Back to sign in</Link>
      </p>
    </AuthLayout>
  );
}
